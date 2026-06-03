package com.wind.funds.spec.ledger;

import com.wind.common.WindConstants;
import com.wind.common.exception.AssertUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.Assert;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 结算策略规范
 *
 * <h2>设计目标</h2>
 * 用于统一表达金融系统中的结算规则（Settlement Policy），覆盖：
 *
 * <ul>
 *     <li>支付 / 卡交易 T+N 延迟结算</li>
 *     <li>周期性结算（天 / 周 / 月 / 季度 / 年）</li>
 *     <li>账单周期结算（信用卡 / VCC billing cycle）</li>
 *     <li>自定义账期（跨月账期）</li>
 * </ul>
 *
 * <p>不支持的表达式必须显式失败，不能静默降级为实时结算。</p>
 *
 * <h2>表达式规范（DSL）</h2>
 *
 * <pre>
 * ===================== 延迟结算 =====================
 * RT              实时结算
 * T+1             交易后1天结算
 * T+2             交易后2天结算
 * D@23:00         每日23:00结算
 *
 * ===================== 小时级 =====================
 * H+1             每1小时结算
 * H+6             每6小时结算
 *
 * ===================== 周结算 =====================
 * W+1@1           每周一结算
 * W+1@5           每周五结算
 * W+2@1           每两周周一结算
 * W@MON           每周一结算
 *
 * ===================== 月结算 =====================
 * M+1@1           每月1号
 * M+1@15          每月15号
 * M+1@L           每月最后一天（关键）
 * M+2@1           每两个月1号
 * M@15            每月15号
 *
 * ===================== 季度结算 =====================
 * Q+1              每季度结算
 * Q+1@L            每季度最后一天
 * Q@03-31          每季度第3个月最后一天结算
 *
 * ===================== 年结算 =====================
 * Y+1@01-01        每年1月1日
 * Y+1@12-31        每年最后一天
 * Y@01-01          每年1月1日
 *
 * ===================== 自定义账期 =====================
 * C@05-04          5号 -> 次月4号（信用卡账单模型）
 * C@10-09          10号 -> 次月9号
 * C@contract-2026-05-16_2026-06-15 外部账期引用
 * </pre>
 *
 * @author wuxp
 * @date 2026-04-22
 */
@Getter
public final class SettlementPolicySpec {

    // ========================= 常用标准结算策略 =========================

    public static final SettlementPolicySpec RT = new SettlementPolicySpec(
            SettlementMode.REALTIME, 0, null, null, "RT");
    public static final SettlementPolicySpec T1 = new SettlementPolicySpec(
            SettlementMode.DELAY_DAYS, 1, null, null, "T+1");
    public static final SettlementPolicySpec T2 = new SettlementPolicySpec(
            SettlementMode.DELAY_DAYS, 2, null, null, "T+2");
    public static final SettlementPolicySpec T3 = new SettlementPolicySpec(
            SettlementMode.DELAY_DAYS, 3, null, null, "T+3");
    public static final SettlementPolicySpec T7 = new SettlementPolicySpec(
            SettlementMode.DELAY_DAYS, 7, null, null, "T+7");

    public static final SettlementPolicySpec WEEKLY_MONDAY = new SettlementPolicySpec(
            SettlementMode.WEEKLY, 1, 1, null, "W+1@1");
    public static final SettlementPolicySpec WEEKLY_FRIDAY = new SettlementPolicySpec(
            SettlementMode.WEEKLY, 1, 5, null, "W+1@5");

    public static final SettlementPolicySpec MONTHLY_FIRST_DAY = new SettlementPolicySpec(
            SettlementMode.MONTHLY, 1, 1, null, "M+1@1");
    public static final SettlementPolicySpec MONTHLY_MID = new SettlementPolicySpec(
            SettlementMode.MONTHLY, 1, 15, null, "M+1@15");
    public static final SettlementPolicySpec MONTHLY_END = new SettlementPolicySpec(
            SettlementMode.MONTHLY, 1, -1, null, "M+1@L");

    public static final SettlementPolicySpec QUARTERLY = new SettlementPolicySpec(
            SettlementMode.QUARTERLY, 1, null, null, "Q+1");
    public static final SettlementPolicySpec QUARTERLY_END = new SettlementPolicySpec(
            SettlementMode.QUARTERLY, 1, -1, null, "Q+1@L");

    public static final SettlementPolicySpec YEARLY = new SettlementPolicySpec(
            SettlementMode.YEARLY, 1, null, null, "Y+1");
    public static final SettlementPolicySpec YEARLY_JAN1 = new SettlementPolicySpec(
            SettlementMode.YEARLY, 1, 1, 1, "Y+1@01-01");

    public static final SettlementPolicySpec BILLING_CYCLE_5_4 = new SettlementPolicySpec(
            SettlementMode.CUSTOM_CYCLE, 0, 5, 4, "C@05-04");

    // ========================= 预定义常量缓存 =========================

    private static final Map<String, SettlementPolicySpec> CONSTANTS = new ConcurrentHashMap<>();

    static {
        CONSTANTS.put("RT", RT);
        CONSTANTS.put("T+1", T1);
        CONSTANTS.put("T+2", T2);
        CONSTANTS.put("T+3", T3);
        CONSTANTS.put("T+7", T7);
        CONSTANTS.put("W+1@1", WEEKLY_MONDAY);
        CONSTANTS.put("W+1@5", WEEKLY_FRIDAY);
        CONSTANTS.put("M+1@1", MONTHLY_FIRST_DAY);
        CONSTANTS.put("M+1@15", MONTHLY_MID);
        CONSTANTS.put("M+1@L", MONTHLY_END);
        CONSTANTS.put("Q+1", QUARTERLY);
        CONSTANTS.put("Q+1@L", QUARTERLY_END);
        CONSTANTS.put("Y+1", YEARLY);
        CONSTANTS.put("Y+1@01-01", YEARLY_JAN1);
        CONSTANTS.put("C@05-04", BILLING_CYCLE_5_4);
    }

    // ========================= 核心字段 =========================

    private final SettlementMode settlementMode;
    private final int interval;
    private final Integer param;
    private final Integer endParam;
    private final String raw;
    private final String rangeId;

    private SettlementPolicySpec(SettlementMode mode, int interval, Integer param, Integer endParam, String raw) {
        this(mode, interval, param, endParam, raw, null);
    }

    private SettlementPolicySpec(SettlementMode mode,
                                 int interval,
                                 Integer param,
                                 Integer endParam,
                                 String raw,
                                 String rangeId) {
        this.settlementMode = mode;
        this.interval = interval;
        this.param = param;
        this.endParam = endParam;
        this.raw = raw;
        this.rangeId = rangeId;
    }

    /**
     * 解析结算策略
     */
    public static SettlementPolicySpec parse(String expression) {
        Assert.hasText(expression, "expression must not be null");
        String rawExpression = expression.trim();
        String normalized = rawExpression.toUpperCase(Locale.ROOT);

        // 从缓存中获取预定义常量
        SettlementPolicySpec predefined = CONSTANTS.get(normalized);
        if (predefined != null) {
            return predefined;
        }

        // 自定义账期 C@DD-DD 不使用 + 语法，需在通用周期表达式前解析。
        if (normalized.startsWith("C@")) {
            return parseCustomCycle(rawExpression);
        }

        // 产品 DSL：每日固定时间 D@HH:mm。
        if (normalized.startsWith("D@")) {
            return parseDailyAt(normalized);
        }

        // 产品 DSL：W@MON、M@15、Q@03-31、Y@01-01。
        if (normalized.startsWith("W@")) {
            return parseWeeklyAt(normalized);
        }
        if (normalized.startsWith("M@")) {
            return parseMonthlyAt(normalized);
        }
        if (normalized.startsWith("Q@")) {
            return parseQuarterlyAt(normalized);
        }
        if (normalized.startsWith("Y@")) {
            return parseYearlyAt(normalized);
        }

        // 延迟结算 T+N
        if (normalized.startsWith("T+")) {
            int days = parseInt(normalized.substring(2));
            if (days <= 0) {
                throw new IllegalArgumentException("T delay must be positive: " + rawExpression);
            }
            return new SettlementPolicySpec(SettlementMode.DELAY_DAYS, days, null, null, normalized);
        }

        // 处理带 @ 的表达式
        String[] atParts = normalized.split(WindConstants.AT);
        AssertUtils.isTrue(atParts.length <= 2,
                "Invalid format, expected at most one '@' character: " + rawExpression);
        String left = atParts[0];
        String right = atParts.length > 1 ? atParts[1] : null;

        // 校验 @ 后面的部分不能包含负号（除非是合法的 L 或 Y 模式的 MM-dd）
        if (right != null && !"L".equals(right) && right.contains("-")
                && !(left.charAt(0) == 'Y' && right.matches("\\d{2}-\\d{2}"))) {
            throw new IllegalArgumentException("Invalid parameter, negative numbers not allowed: " + rawExpression);
        }

        char prefix = left.charAt(0);
        String numPart = left.substring(1);
        if (!numPart.startsWith("+")) {
            throw new IllegalArgumentException("Invalid format, expected '+' after prefix: " + rawExpression);
        }
        int interval = parseInt(numPart.substring(1));
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be positive: " + rawExpression);
        }

        return switch (prefix) {
            case 'H' -> new SettlementPolicySpec(SettlementMode.HOURLY, interval, null, null, normalized);
            case 'W' -> {
                if (right == null) {
                    throw new IllegalArgumentException("Weekday must be specified after @: " + rawExpression);
                }
                int weekday = parseInt(right);
                if (weekday < 1 || weekday > 7) {
                    throw new IllegalArgumentException("Weekday must be 1-7, got: " + weekday);
                }
                yield new SettlementPolicySpec(SettlementMode.WEEKLY, interval, weekday, null, normalized);
            }
            case 'M' -> {
                if (right == null) {
                    throw new IllegalArgumentException(
                            "Day must be specified after @ (1-31 or L): " + rawExpression);
                }
                int day;
                if ("L".equals(right)) {
                    day = -1;
                } else {
                    day = parseInt(right);
                    if (day < 1 || day > 31) {
                        throw new IllegalArgumentException("Day must be 1-31 or L, got: " + day);
                    }
                }
                yield new SettlementPolicySpec(SettlementMode.MONTHLY, interval, day, null, normalized);
            }
            case 'Q' -> {
                if (right == null) {
                    yield new SettlementPolicySpec(SettlementMode.QUARTERLY, interval, null, null, normalized);
                } else if ("L".equals(right)) {
                    yield new SettlementPolicySpec(SettlementMode.QUARTERLY, interval, -1, null, normalized);
                } else {
                    throw new IllegalArgumentException("Quarterly only supports @L or no suffix: " + rawExpression);
                }
            }
            case 'Y' -> {
                if (right != null && right.matches("\\d{2}-\\d{2}")) {
                    String[] md = right.split("-");
                    int month = parseInt(md[0]);
                    int day = parseInt(md[1]);
                    if (month < 1 || month > 12) {
                        throw new IllegalArgumentException("Month must be 1-12");
                    }
                    if (day < 1 || day > 31) {
                        throw new IllegalArgumentException("Day must be 1-31");
                    }
                    MonthDay.of(month, day);
                    yield new SettlementPolicySpec(SettlementMode.YEARLY, interval, month, day, normalized);
                } else if (right == null) {
                    yield new SettlementPolicySpec(SettlementMode.YEARLY, interval, null, null, normalized);
                } else {
                    throw new IllegalArgumentException(
                            "Invalid year format, expected MM-dd or nothing: " + rawExpression);
                }
            }
            case 'C' -> parseCustomCycle(rawExpression);
            default -> throw new IllegalArgumentException("Unknown settlement type: " + rawExpression);
        };
    }

    private static SettlementPolicySpec parseCustomCycle(String expr) {
        if (!expr.toUpperCase(Locale.ROOT).startsWith("C@")) {
            throw new IllegalArgumentException("Custom cycle must start with C@: " + expr);
        }
        String body = expr.substring(2);
        Assert.hasText(body, "Custom cycle or range id must not be empty: " + expr);
        if (!body.matches("\\d{1,2}-\\d{1,2}")) {
            return new SettlementPolicySpec(SettlementMode.CUSTOM_RANGE, 0, null, null, expr, body);
        }
        String[] parts = body.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid custom cycle, expected C@dd-dd : " + expr);
        }
        int start = parseInt(parts[0]);
        int end = parseInt(parts[1]);
        if (start < 1 || start > 31) {
            throw new IllegalArgumentException("Custom cycle start day must be 1-31: " + start);
        }
        if (end < 1 || end > 31) {
            throw new IllegalArgumentException("Custom cycle end day must be 1-31: " + end);
        }
        return new SettlementPolicySpec(SettlementMode.CUSTOM_CYCLE, 0, start, end, expr);
    }

    private static SettlementPolicySpec parseDailyAt(String expression) {
        String timeText = expression.substring(2);
        if (!timeText.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("Daily settlement time must be HH:mm: " + expression);
        }
        try {
            LocalTime time = LocalTime.parse(timeText);
            return new SettlementPolicySpec(
                    SettlementMode.DAILY_AT, 1, time.getHour(), time.getMinute(), expression);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid daily settlement time: " + expression, ex);
        }
    }

    private static SettlementPolicySpec parseWeeklyAt(String expression) {
        String weekdayText = expression.substring(2);
        int weekday = parseWeekday(weekdayText);
        return new SettlementPolicySpec(SettlementMode.WEEKLY, 1, weekday, null, expression);
    }

    private static SettlementPolicySpec parseMonthlyAt(String expression) {
        String dayText = expression.substring(2);
        int day = parseMonthDay(dayText);
        return new SettlementPolicySpec(SettlementMode.MONTHLY, 1, day, null, expression);
    }

    private static SettlementPolicySpec parseQuarterlyAt(String expression) {
        String dateText = expression.substring(2);
        String[] parts = dateText.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Quarterly settlement date must be MM-dd: " + expression);
        }
        int quarterMonth = parseInt(parts[0]);
        int day = parseInt(parts[1]);
        if (quarterMonth < 1 || quarterMonth > 3) {
            throw new IllegalArgumentException("Quarterly month offset must be 1-3: " + expression);
        }
        if (day < 1 || day > 31) {
            throw new IllegalArgumentException("Quarterly day must be 1-31: " + expression);
        }
        return new SettlementPolicySpec(SettlementMode.QUARTERLY, 1, quarterMonth, day, expression);
    }

    private static SettlementPolicySpec parseYearlyAt(String expression) {
        String dateText = expression.substring(2);
        String[] parts = dateText.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Yearly settlement date must be MM-dd: " + expression);
        }
        int month = parseInt(parts[0]);
        int day = parseInt(parts[1]);
        MonthDay.of(month, day);
        return new SettlementPolicySpec(SettlementMode.YEARLY, 1, month, day, expression);
    }

    // ========================= 核心计算 =========================

    public LocalDateTime nextSettlementTime(LocalDateTime now) {
        return switch (settlementMode) {
            case REALTIME -> now;
            case DELAY_DAYS -> now.plusDays(interval);
            case DAILY_AT -> nextDailyAt(now, param, endParam);
            case HOURLY -> nextHourly(now, interval);
            case WEEKLY -> nextWeekly(now, interval, param);
            case MONTHLY -> nextMonthly(now, interval, param);
            case QUARTERLY -> nextQuarterly(now, interval, param, endParam);
            case YEARLY -> nextYearly(now, interval, param, endParam);
            case CUSTOM_CYCLE -> nextCustomCycle(now, param, endParam);
            case CUSTOM_RANGE -> throw new IllegalStateException(
                    "Custom external range must be resolved by external range calendar: " + rangeId);
        };
    }

    private static LocalDateTime nextDailyAt(LocalDateTime now, int hour, int minute) {
        LocalDateTime candidate = now.toLocalDate().atTime(hour, minute);
        if (!candidate.isAfter(now)) {
            return candidate.plusDays(1);
        }
        return candidate;
    }

    private static LocalDateTime nextHourly(LocalDateTime now, int intervalHours) {
        int hour = now.getHour();
        int alignedHour = ((hour / intervalHours) + 1) * intervalHours;
        if (alignedHour >= 24) {
            return now.plusDays(1).truncatedTo(ChronoUnit.DAYS).withHour(0);
        }
        return now.truncatedTo(ChronoUnit.HOURS).withHour(alignedHour);
    }

    // 修正周结算间隔逻辑
    private static LocalDateTime nextWeekly(LocalDateTime now, int intervalWeeks, int targetWeekday) {
        LocalDate current = now.toLocalDate();
        DayOfWeek target = DayOfWeek.of(targetWeekday);
        // 找到下一个目标星期几（包括今天）
        LocalDate nextDate = current.with(TemporalAdjusters.nextOrSame(target));
        // 如果找到的日期就是今天，但当前时间不是00:00，则应该算下一个周期
        if (nextDate.equals(current) && !now.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            nextDate = nextDate.plusWeeks(intervalWeeks);
        }
        // 计算从基准周（周一）到目标日期的周数差，确保是 intervalWeeks 的倍数
        long weeksDiff = ChronoUnit.WEEKS.between(getWeekBase(current), getWeekBase(nextDate));
        if (weeksDiff == 0 || weeksDiff % intervalWeeks != 0) {
            long remainder = intervalWeeks - (weeksDiff % intervalWeeks);
            nextDate = nextDate.plusWeeks(remainder);
        }
        // 最终确保日期在 now 之后
        if (nextDate.isEqual(current) && now.toLocalTime().isAfter(LocalTime.MIDNIGHT)) {
            nextDate = nextDate.plusWeeks(intervalWeeks);
        }
        return nextDate.atStartOfDay();
    }

    private static LocalDate getWeekBase(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static LocalDateTime nextMonthly(LocalDateTime now, int intervalMonths, int targetDay) {
        LocalDate current = now.toLocalDate();
        boolean isEndOfMonth = targetDay == -1;
        LocalDate nextDate;
        if (isEndOfMonth) {
            nextDate = current.withDayOfMonth(current.lengthOfMonth());
            if (nextDate.isEqual(current) && now.toLocalTime().isAfter(LocalTime.MIDNIGHT)) {
                LocalDate nextMonth = current.plusMonths(intervalMonths);
                nextDate = nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
            } else if (nextDate.isBefore(current)) {
                LocalDate nextMonth = current.plusMonths(intervalMonths);
                nextDate = nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
            }
        } else {
            int lastDayOfMonth = current.lengthOfMonth();
            int realDay = Math.min(targetDay, lastDayOfMonth);
            nextDate = current.withDayOfMonth(realDay);
            if (nextDate.isEqual(current) && now.toLocalTime().isAfter(LocalTime.MIDNIGHT)) {
                nextDate = current.plusMonths(intervalMonths);
                lastDayOfMonth = nextDate.lengthOfMonth();
                nextDate = nextDate.withDayOfMonth(Math.min(targetDay, lastDayOfMonth));
            } else if (nextDate.isBefore(current)) {
                nextDate = current.plusMonths(intervalMonths);
                lastDayOfMonth = nextDate.lengthOfMonth();
                nextDate = nextDate.withDayOfMonth(Math.min(targetDay, lastDayOfMonth));
            }
        }
        long monthsDiff = ChronoUnit.MONTHS.between(getMonthBase(current), getMonthBase(nextDate));
        if (monthsDiff % intervalMonths != 0) {
            long remainder = intervalMonths - (monthsDiff % intervalMonths);
            nextDate = nextDate.plusMonths(remainder);
            if (isEndOfMonth) {
                nextDate = nextDate.withDayOfMonth(nextDate.lengthOfMonth());
            } else {
                int last = nextDate.lengthOfMonth();
                nextDate = nextDate.withDayOfMonth(Math.min(targetDay, last));
            }
        }
        return nextDate.atStartOfDay();
    }

    private static LocalDate getMonthBase(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    private static LocalDateTime nextQuarterly(LocalDateTime now,
                                               int intervalQuarters,
                                               Integer targetMonthOrDay,
                                               Integer targetDay) {
        if (targetDay != null) {
            return nextQuarterlyAt(now, intervalQuarters, targetMonthOrDay, targetDay);
        }
        int currentQuarterStartMonth = (now.getMonthValue() - 1) / 3 * 3 + 1;
        LocalDate currentQuarterStart = LocalDate.of(now.getYear(), currentQuarterStartMonth, 1);
        if (targetMonthOrDay == null) {
            long quartersToAdd = intervalQuarters;
            LocalDate targetDate = currentQuarterStart.plusMonths(3L * quartersToAdd);
            if (targetDate.isBefore(now.toLocalDate()) ||
                    (targetDate.isEqual(now.toLocalDate()) && now.toLocalTime().isAfter(LocalTime.MIDNIGHT))) {
                targetDate = targetDate.plusMonths(3L * intervalQuarters);
            }
            return targetDate.atStartOfDay();
        } else if (targetMonthOrDay == -1) {
            LocalDate currentQuarterEnd = currentQuarterStart.plusMonths(3).minusDays(1);
            if (currentQuarterEnd.isAfter(now.toLocalDate()) ||
                    (currentQuarterEnd.isEqual(now.toLocalDate()) && now.toLocalTime().equals(LocalTime.MIDNIGHT))) {
                return currentQuarterEnd.atStartOfDay();
            }
            LocalDate nextQuarterStart = currentQuarterStart.plusMonths(3L * intervalQuarters);
            LocalDate nextQuarterEnd = nextQuarterStart.plusMonths(3).minusDays(1);
            if (nextQuarterEnd.isBefore(now.toLocalDate()) ||
                    (nextQuarterEnd.isEqual(now.toLocalDate()) && now.toLocalTime().isAfter(LocalTime.MIDNIGHT))) {
                nextQuarterEnd = nextQuarterStart.plusMonths(3L * (intervalQuarters + 1)).minusDays(1);
            }
            return nextQuarterEnd.atStartOfDay();
        } else {
            throw new IllegalStateException("Unexpected target month or day for quarterly: " + targetMonthOrDay);
        }
    }

    private static LocalDateTime nextQuarterlyAt(LocalDateTime now,
                                                 int intervalQuarters,
                                                 int quarterMonth,
                                                 int targetDay) {
        int currentQuarterStartMonth = (now.getMonthValue() - 1) / 3 * 3 + 1;
        LocalDate currentQuarterStart = LocalDate.of(now.getYear(), currentQuarterStartMonth, 1);
        LocalDate targetDate = quarterlyDate(currentQuarterStart, quarterMonth, targetDay);
        if (!targetDate.atStartOfDay().isAfter(now)) {
            targetDate = quarterlyDate(currentQuarterStart.plusMonths(3L * intervalQuarters), quarterMonth, targetDay);
        }
        return targetDate.atStartOfDay();
    }

    private static LocalDate quarterlyDate(LocalDate quarterStart, int quarterMonth, int targetDay) {
        LocalDate month = quarterStart.plusMonths(quarterMonth - 1L);
        return month.withDayOfMonth(Math.min(targetDay, month.lengthOfMonth()));
    }

    private static LocalDateTime nextYearly(LocalDateTime now, int intervalYears, Integer month, Integer day) {
        LocalDate current = now.toLocalDate();
        LocalDate targetDate;
        if (month != null && day != null) {
            targetDate = LocalDate.of(current.getYear(), month, day);
            if (targetDate.isBefore(current)
                    || (targetDate.isEqual(current) && now.toLocalTime().isAfter(LocalTime.MIDNIGHT))) {
                targetDate = targetDate.plusYears(intervalYears);
            }
        } else {
            targetDate = current;
            if (now.toLocalTime().isAfter(LocalTime.MIDNIGHT)) {
                targetDate = targetDate.plusYears(intervalYears);
            }
        }
        long yearsDiff = ChronoUnit.YEARS.between(getYearBase(current), getYearBase(targetDate));
        if (yearsDiff % intervalYears != 0) {
            long remainder = intervalYears - (yearsDiff % intervalYears);
            targetDate = targetDate.plusYears(remainder);
        }
        return targetDate.atStartOfDay();
    }

    private static LocalDate getYearBase(LocalDate date) {
        return date.withDayOfYear(1);
    }

    private static LocalDateTime nextCustomCycle(LocalDateTime now, int startDay, int endDay) {
        int today = now.getDayOfMonth();
        LocalDate targetDate;
        if (today >= startDay) {
            targetDate = now.toLocalDate().plusMonths(1);
        } else {
            targetDate = now.toLocalDate();
        }
        int lastDay = targetDate.lengthOfMonth();
        int realEnd = Math.min(endDay, lastDay);
        targetDate = targetDate.withDayOfMonth(realEnd);
        if (targetDate.equals(now.toLocalDate()) && now.toLocalTime().isAfter(LocalTime.MIDNIGHT)) {
            LocalDate nextMonth = targetDate.plusMonths(1);
            targetDate = nextMonth.withDayOfMonth(Math.min(endDay, nextMonth.lengthOfMonth()));
        } else if (targetDate.isBefore(now.toLocalDate())) {
            LocalDate nextMonth = targetDate.plusMonths(1);
            targetDate = nextMonth.withDayOfMonth(Math.min(endDay, nextMonth.lengthOfMonth()));
        }
        return targetDate.atStartOfDay();
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + s);
        }
    }

    private static int parseWeekday(String value) {
        return switch (value) {
            case "MON" -> 1;
            case "TUE" -> 2;
            case "WED" -> 3;
            case "THU" -> 4;
            case "FRI" -> 5;
            case "SAT" -> 6;
            case "SUN" -> 7;
            default -> {
                int weekday = parseInt(value);
                if (weekday < 1 || weekday > 7) {
                    throw new IllegalArgumentException("Weekday must be MON-SUN or 1-7, got: " + value);
                }
                yield weekday;
            }
        };
    }

    private static int parseMonthDay(String value) {
        if ("L".equals(value)) {
            return -1;
        }
        int day = parseInt(value);
        if (day < 1 || day > 31) {
            throw new IllegalArgumentException("Day must be 1-31 or L, got: " + day);
        }
        return day;
    }

    @Override
    public String toString() {
        return raw;
    }

    @Getter
    @AllArgsConstructor
    public enum SettlementMode {
        REALTIME("RT"), DELAY_DAYS("T"), DAILY_AT("D"), HOURLY("H"),
        WEEKLY("W"), MONTHLY("M"), QUARTERLY("Q"),
        YEARLY("Y"), CUSTOM_CYCLE("C"), CUSTOM_RANGE("C");
        private final String prefix;
    }
}
