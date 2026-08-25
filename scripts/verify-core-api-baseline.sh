#!/usr/bin/env bash

set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
classes_dir="$repo_root/core/target/classes"
update_baseline=false
if [[ "${1:-}" == "--update" ]]; then
    update_baseline=true
    shift
fi
baseline_dir=${1:-"$repo_root/core/api-baseline"}
policy_file="$baseline_dir/api-policy.tsv"
baseline_file="$baseline_dir/stable-api.txt"

if [[ -n "${WIND_FUNDS_JAVA_HOME:-}" ]]; then
    javap="$WIND_FUNDS_JAVA_HOME/bin/javap"
elif [[ -n "${JAVA_HOME:-}" ]]; then
    javap="$JAVA_HOME/bin/javap"
else
    javap=$(command -v javap || true)
fi

[[ -x "$javap" ]] || { echo "javap not found; set WIND_FUNDS_JAVA_HOME or JAVA_HOME"; exit 1; }
[[ -d "$classes_dir/com/wind/funds" ]] || { echo "Core classes not found; run just compile first"; exit 1; }
[[ -f "$policy_file" ]] || { echo "Core API policy not found: $policy_file"; exit 1; }
[[ -f "$baseline_file" ]] || { echo "Core API baseline not found: $baseline_file"; exit 1; }

export LC_ALL=C
work_dir=$(mktemp -d "${TMPDIR:-/tmp}/wind-funds-core-api.XXXXXX")
trap 'rm -rf "$work_dir"' EXIT

actual_types="$work_dir/actual-types.txt"
policy_types="$work_dir/policy-types.txt"
stable_types="$work_dir/stable-types.txt"
stable_signature_types="$work_dir/stable-signature-types.txt"
actual_api="$work_dir/actual-api.txt"
member_exclusions="$work_dir/member-exclusions.txt"
filtered_api="$work_dir/filtered-api.txt"

find "$classes_dir/com/wind/funds" -type f -name '*.class' ! -name '*$*' ! -name 'package-info.class' -print \
    | sort \
    | while IFS= read -r class_file; do
        type_name=${class_file#"$classes_dir/"}
        type_name=${type_name%.class}
        type_name=${type_name//\//.}
        declaration=$("$javap" -public -classpath "$classes_dir" "$type_name" 2>/dev/null \
            | awk '/^public / { print; exit }')
        [[ -n "$declaration" ]] && printf '%s\n' "$type_name"
    done > "$actual_types"

awk -F '\t' '
    /^#/ || NF == 0 { next }
    $1 == "EXPERIMENTAL" || $1 == "INTERNAL" { print $2; next }
    $1 == "EXCLUDED_MEMBER" { next }
    { print "Invalid API policy line: " $0 > "/dev/stderr"; invalid = 1 }
    END { exit invalid }
' "$policy_file" | sort > "$policy_types"

if [[ $(wc -l < "$actual_types" | tr -d ' ') -ne 103 ]]; then
    echo "Expected 103 public top-level core types; found $(wc -l < "$actual_types" | tr -d ' ')"
    exit 1
fi
if [[ $(awk -F '\t' '$1 == "EXPERIMENTAL" { count++ } END { print count + 0 }' "$policy_file") -ne 4 ]] \
    || [[ $(awk -F '\t' '$1 == "INTERNAL" { count++ } END { print count + 0 }' "$policy_file") -ne 4 ]]; then
    echo "API policy must contain 4 EXPERIMENTAL and 4 INTERNAL types"
    exit 1
fi
if [[ $(sort "$policy_types" | uniq -d | wc -l | tr -d ' ') -ne 0 ]]; then
    echo "Duplicate type classification in $policy_file"
    exit 1
fi
if ! comm -23 "$policy_types" "$actual_types" | diff -u /dev/null -; then
    echo "API policy contains types that are not public top-level core types"
    exit 1
fi

comm -23 "$actual_types" "$policy_types" > "$stable_types"
if [[ $(wc -l < "$stable_types" | tr -d ' ') -ne 95 ]]; then
    echo "Expected 95 stable core types; found $(wc -l < "$stable_types" | tr -d ' ')"
    exit 1
fi

find "$classes_dir/com/wind/funds" -type f -name '*.class' ! -name 'package-info.class' -print \
    | sort \
    | while IFS= read -r class_file; do
        type_name=${class_file#"$classes_dir/"}
        type_name=${type_name%.class}
        type_name=${type_name//\//.}
        top_level_name=${type_name%%\$*}
        grep -Fxq "$top_level_name" "$stable_types" || continue
        declaration=$("$javap" -public -classpath "$classes_dir" "$type_name" 2>/dev/null \
            | awk '/^public / { print; exit }')
        [[ -n "$declaration" ]] && printf '%s\n' "$type_name"
    done > "$stable_signature_types"

while IFS= read -r type_name; do
    "$javap" -public -classpath "$classes_dir" "$type_name" 2>/dev/null \
        | awk -v type_name="$type_name" '
            /^public / || /^  public / {
                line = $0
                gsub(/^[[:space:]]+|[[:space:]]+$/, "", line)
                gsub(/[[:space:]]+/, " ", line)
                print type_name "\t" line
            }
        '
done < "$stable_signature_types" | sort > "$actual_api"

awk -F '\t' '$1 == "EXCLUDED_MEMBER" { print $2 "\t" $3 }' "$policy_file" \
    | sort > "$member_exclusions"
if [[ $(sort "$member_exclusions" | uniq -d | wc -l | tr -d ' ') -ne 0 ]]; then
    echo "Duplicate member exclusion in $policy_file"
    exit 1
fi
if ! comm -23 "$member_exclusions" "$actual_api" | diff -u /dev/null -; then
    echo "API policy contains stale member exclusions"
    exit 1
fi

grep -Fvx -f "$member_exclusions" "$actual_api" > "$filtered_api"
if $update_baseline; then
    cp "$filtered_api" "$baseline_file"
    echo "Core API baseline updated: 95 stable, 4 experimental, 4 internal public top-level types; public nested signatures included"
    exit 0
fi
if ! diff -u "$baseline_file" "$filtered_api"; then
    echo "Stable core API differs from the approved baseline"
    exit 1
fi

echo "Core API baseline verified: 95 stable, 4 experimental, 4 internal public top-level types; public nested signatures included"
