#!/usr/bin/env bash

set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo_root"

command -v rg >/dev/null 2>&1 || { echo "rg is required"; exit 1; }

face_roots=(
    transaction/face/src/main/java
    wallet/face/src/main/java
    ledger/face/src/main/java
    reconciliation/face/src/main/java
    governance/face/src/main/java
)

violations=0
checked_types=0
checked_models=0
checked_enums=0
type_pattern='^(public )?(abstract |final |sealed |non-sealed )*'
type_pattern+='(class|interface|record|enum|@interface) '

while IFS= read -r file; do
    if rg -q "$type_pattern" "$file"; then
        ((checked_types += 1))
        if ! rg -q '/\*\*' "$file"; then
            echo "Missing type Javadoc: $file"
            ((violations += 1))
        fi
    fi

    if [[ "$file" =~ /(model/)?(dto|request|query|command)/.*\.java$ ]] \
        && [[ "$file" != */package-info.java ]]; then
        ((checked_models += 1))
        if ! rg -q '@Schema' "$file"; then
            echo "Missing @Schema: $file"
            ((violations += 1))
        fi
        if rg -q '@Schema\(description[[:space:]]*=[[:space:]]*""\)' "$file"; then
            echo "Empty @Schema description: $file"
            ((violations += 1))
        fi
    fi

    if rg -q '^public enum ' "$file"; then
        ((checked_enums += 1))
        if ! rg -q 'implements DescriptiveEnum' "$file"; then
            echo "Public enum must implement DescriptiveEnum: $file"
            ((violations += 1))
        fi
    fi
done < <(rg --files "${face_roots[@]}" -g '*.java' | sort)

if ((violations > 0)); then
    echo "Public contract convention violations: $violations"
    exit 1
fi

echo "Public contract conventions verified: types=$checked_types, models=$checked_models, enums=$checked_enums"
