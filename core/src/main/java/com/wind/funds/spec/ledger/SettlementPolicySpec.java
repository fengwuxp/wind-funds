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
import java.util.concurrent.atomic.AtomicReference;

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
 * <p>字符和符号含义：</p>
 * <ul>
 *     <li>RT：Realtime，实时结算。</li>
 *     <li>T：Trade day，交易日 / 工作日延迟，T+N 会跳过节假日，
 *     包括周末和交易日历登记的法定节假日。</li>
 *     <li>D：Day，自然日或每日固定时间，D+N 表示 N 个自然日后，
 *     D@HH:mm 表示每日固定时间。</li>
 *     <li>H：Hour，小时级周期。</li>
 *     <li>W：Week，周周期。</li>
 *     <li>M：Month，月周期。</li>
 *     <li>Q：Quarter，季度周期。</li>
 *     <li>Y：Year，年周期。</li>
 *     <li>C：Custom cycle / range，自定义账期或外部账期引用。</li>
 *     <li>+：周期间隔或延迟数量，例如 T+2、W+2@1、M+2@1。</li>
 *     <li>@：结算锚点、cutoff 或账期参数，例如 D@23:00、W@MON、C@05-04。</li>
 *     <li>L：Last day，所在月、季度或周期的最后一天。</li>
 * </ul>
 *
 * <pre>
 * ===================== 延迟结算 =====================
 * RT              实时结算
 * T+1             交易后1个工作日结算
 * T+2             交易后2个工作日结算
 * D+1             交易后1个自然日结算
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

    private static final String EXPRESSION_REALTIME = "RT";
    private static final String EXPRESSION_T1 = "T+1";
    private static final String EXPRESSION_T2 = "T+2";
    private static final String EXPRESSION_T3 = "T+3";
    private static final String EXPRESSION_T7 = "T+7";
    private static final String EXPRESSION_D1 = "D+1";
    private static final String EXPRESSION_WEEKLY_MONDAY = "W+1@1";
    private static final String EXPRESSION_WEEKLY_FRIDAY = "W+1@5";
    private static final String EXPRESSION_MONTHLY_FIRST_DAY = "M+1@1";
    private static final String EXPRESSION_MONTHLY_MID = "M+1@15";
    private static final String EXPRESSION_MONTHLY_END = "M+1@L";
    private static final String EXPRESSION_QUARTERLY = "Q+1";
    private static final String EXPRESSION_QUARTERLY_END = "Q+1@L";
    private static final String EXPRESSION_YEARLY = "Y+1";
    private static final String EXPRESSION_YEARLY_JAN1 = "Y+1@01-01";
    private static final String EXPRESSION_BILLING_CYCLE_5_4 = "C@05-04";

    private static final String TYPE_REALTIME = "RT";
    private static final String TYPE_TRADE_DAY = "T";
    private static final String TYPE_DAY = "D";
    private static final String TYPE_HOUR = "H";
    private static final String TYPE_WEEK = "W";
    private static final String TYPE_MONTH = "M";
    private static final String TYPE_QUARTER = "Q";
    private static final String TYPE_YEAR = "Y";
    private static final String TYPE_CUSTOM_CYCLE = "C";

    private static final String PREFIX_CUSTOM_CYCLE = "C@";
    private static final String PREFIX_DAILY_AT = "D@";
    private static final String PREFIX_NATURAL_DAY_DELAY = "D+";
    private static final String PREFIX_WEEKLY_AT = "W@";
    private static final String PREFIX_MONTHLY_AT = "M@";
    private static final String PREFIX_QUARTERLY_AT = "Q@";
    private static final String PREFIX_YEARLY_AT = "Y@";
    private static final String PREFIX_TRADE_DAY_DELAY = "T+";
    private static final String INTERVAL_SEPARATOR = "+";
    private static final String LAST_DAY_TOKEN = "L";
    private static final String DATE_SEPARATOR = "-";
    private static final String REGEX_MONTH_DAY = "\\d{2}-\\d{2}";
    private static final String REGEX_CUSTOM_CYCLE = "\\d{1,2}-\\d{1,2}";
    private static final String REGEX_HOUR_MINUTE = "\\d{2}:\\d{2}";
    private static final String WEEKDAY_MONDAY = "MON";
    private static final String WEEKDAY_TUESDAY = "TUE";
    private static final String WEEKDAY_WEDNESDAY = "WED";
    private static final String WEEKDAY_THURSDAY = "THU";
    private static final String WEEKDAY_FRIDAY = "FRI";
    private static final String WEEKDAY_SATURDAY = "SAT";
    private static final String WEEKDAY_SUNDAY = "SUN";
    private static final String REQUIRED_EXPRESSION_MESSAGE = "expression must not be null";
    private static final String REQUIRED_HOLIDAY_CALENDAR_MESSAGE = "holidayCalendar must not be null";
    private static final String YEAR_MONTH_RANGE_MESSAGE = "Month must be 1-12";
    private static final String MONTH_DAY_RANGE_MESSAGE = "Day must be 1-31";

    private static final char HOURLY_EXPRESSION_PREFIX = 'H';
    private static final char WEEKLY_EXPRESSION_PREFIX = 'W';
    private static final char MONTHLY_EXPRESSION_PREFIX = 'M';
    private static final char QUARTERLY_EXPRESSION_PREFIX = 'Q';
    private static final char YEARLY_EXPRESSION_PREFIX = 'Y';
    private static final char CUSTOM_CYCLE_EXPRESSION_PREFIX = 'C';

    private static final int ZERO_INTERVAL = 0;
    private static final int DEFAULT_INTERVAL = 1;
    private static final int EXPRESSION_PREFIX_LENGTH = 2;
    private static final int PREFIX_CHAR_INDEX = 0;
    private static final int FIRST_PART_INDEX = 0;
    private static final int SECOND_PART_INDEX = 1;
    private static final int SINGLE_PART_LENGTH = 1;
    private static final int PAIR_PART_LENGTH = 2;
    private static final int MAX_AT_PARTS = 2;
    private static final int LAST_DAY = -1;
    private static final int FIRST_DAY = 1;
    private static final int LAST_DAY_OF_MONTH = 31;
    private static final int FIRST_MONTH = 1;
    private static final int LAST_MONTH = 12;
    private static final int FIRST_WEEKDAY = 1;
    private static final int TUESDAY_WEEKDAY = 2;
    private static final int WEDNESDAY_WEEKDAY = 3;
    private static final int THURSDAY_WEEKDAY = 4;
    private static final int FRIDAY_WEEKDAY = 5;
    private static final int SATURDAY_WEEKDAY = 6;
    private static final int LAST_WEEKDAY = 7;
    private static final int TWO_DAY_DELAY = 2;
    private static final int THREE_DAY_DELAY = 3;
    private static final int SEVEN_DAY_DELAY = 7;
    private static final int FIRST_QUARTER_MONTH_OFFSET = 1;
    private static final int LAST_QUARTER_MONTH_OFFSET = 3;
    private static final int HOURS_PER_DAY = 24;
    private static final int MONTHS_PER_QUARTER = 3;
    private static final int MIDNIGHT_HOUR = 0;
    private static final int MID_MONTH_DAY = 15;
    private static final int BILLING_CYCLE_START_DAY = 5;
    private static final int BILLING_CYCLE_END_DAY = 4;

    private static final SettlementHolidayCalendar WEEKEND_ONLY_HOLIDAY_CALENDAR = SettlementPolicySpec::isWeekend;
    private static final AtomicReference<SettlementHolidayCalendar> DEFAULT_HOLIDAY_CALENDAR =
            new AtomicReference<>(WEEKEND_ONLY_HOLIDAY_CALENDAR);

    public static final SettlementPolicySpec RT = new SettlementPolicySpec(
            SettlementMode.REALTIME, ZERO_INTERVAL, null, null, EXPRESSION_REALTIME);
    public static final SettlementPolicySpec T1 = new SettlementPolicySpec(
            SettlementMode.DELAY_DAYS, DEFAULT_INTERVAL, null, null, EXPRESSION_T1);
    public static final SettlementPolicySpec T2 = new SettlementPolicySpec(
            SettlementMode.DELAY_DAYS, TWO_DAY_DELAY, null, null, EXPRESSION_T2);
    public static final SettlementPolicySpec T3 = new SettlementPolicySpec(
            SettlementMode.DELAY_DAYS, THREE_DAY_DELAY, null, null, EXPRESSION_T3);
    public static final SettlementPolicySpec T7 = new SettlementPolicySpec(
            SettlementMode.DELAY_DAYS, SEVEN_DAY_DELAY, null, null, EXPRESSION_T7);
    public static final SettlementPolicySpec D1 = new SettlementPolicySpec(
            SettlementMode.DELAY_NATURAL_DAYS, DEFAULT_INTERVAL, null, null, EXPRESSION_D1);

    public static final SettlementPolicySpec WEEKLY_MONDAY = new SettlementPolicySpec(
            SettlementMode.WEEKLY, DEFAULT_INTERVAL, FIRST_WEEKDAY, null, EXPRESSION_WEEKLY_MONDAY);
    public static final SettlementPolicySpec WEEKLY_FRIDAY = new SettlementPolicySpec(
            SettlementMode.WEEKLY, DEFAULT_INTERVAL, FRIDAY_WEEKDAY, null, EXPRESSION_WEEKLY_FRIDAY);

    public static final SettlementPolicySpec MONTHLY_FIRST_DAY = new SettlementPolicySpec(
            SettlementMode.MONTHLY, DEFAULT_INTERVAL, FIRST_DAY, null, EXPRESSION_MONTHLY_FIRST_DAY);
    public static final SettlementPolicySpec MONTHLY_MID = new SettlementPolicySpec(
            SettlementMode.MONTHLY, DEFAULT_INTERVAL, MID_MONTH_DAY, null, EXPRESSION_MONTHLY_MID);
    public static final SettlementPolicySpec MONTHLY_END = new SettlementPolicySpec(
            SettlementMode.MONTHLY, DEFAULT_INTERVAL, LAST_DAY, null, EXPRESSION_MONTHLY_END);

    public static final SettlementPolicySpec QUARTERLY = new SettlementPolicySpec(
            SettlementMode.QUARTERLY, DEFAULT_INTERVAL, null, null, EXPRESSION_QUARTERLY);
    public static final SettlementPolicySpec QUARTERLY_END = new SettlementPolicySpec(
            SettlementMode.QUARTERLY, DEFAULT_INTERVAL, LAST_DAY, null, EXPRESSION_QUARTERLY_END);

    public static final SettlementPolicySpec YEARLY = new SettlementPolicySpec(
            SettlementMode.YEARLY, DEFAULT_INTERVAL, null, null, EXPRESSION_YEARLY);
    public static final SettlementPolicySpec YEARLY_JAN1 = new SettlementPolicySpec(
            SettlementMode.YEARLY, DEFAULT_INTERVAL, FIRST_MONTH, FIRST_DAY, EXPRESSION_YEARLY_JAN1);

    public static final SettlementPolicySpec BILLING_CYCLE_5_4 = new SettlementPolicySpec(
            SettlementMode.CUSTOM_CYCLE,
            ZERO_INTERVAL,
            BILLING_CYCLE_START_DAY,
            BILLING_CYCLE_END_DAY,
            EXPRESSION_BILLING_CYCLE_5_4);

    // ========================= 预定义常量缓存 =========================

    private static final Map<String, SettlementPolicySpec> CONSTANTS = new ConcurrentHashMap<>();

    static {
        CONSTANTS.put(EXPRESSION_REALTIME, RT);
        CONSTANTS.put(EXPRESSION_T1, T1);
        CONSTANTS.put(EXPRESSION_T2, T2);
        CONSTANTS.put(EXPRESSION_T3, T3);
        CONSTANTS.put(EXPRESSION_T7, T7);
        CONSTANTS.put(EXPRESSION_D1, D1);
        CONSTANTS.put(EXPRESSION_WEEKLY_MONDAY, WEEKLY_MONDAY);
        CONSTANTS.put(EXPRESSION_WEEKLY_FRIDAY, WEEKLY_FRIDAY);
        CONSTANTS.put(EXPRESSION_MONTHLY_FIRST_DAY, MONTHLY_FIRST_DAY);
        CONSTANTS.put(EXPRESSION_MONTHLY_MID, MONTHLY_MID);
        CONSTANTS.put(EXPRESSION_MONTHLY_END, MONTHLY_END);
        CONSTANTS.put(EXPRESSION_QUARTERLY, QUARTERLY);
        CONSTANTS.put(EXPRESSION_QUARTERLY_END, QUARTERLY_END);
        CONSTANTS.put(EXPRESSION_YEARLY, YEARLY);
        CONSTANTS.put(EXPRESSION_YEARLY_JAN1, YEARLY_JAN1);
        CONSTANTS.put(EXPRESSION_BILLING_CYCLE_5_4, BILLING_CYCLE_5_4);
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
        Assert.hasText(expression, REQUIRED_EXPRESSION_MESSAGE);
        String rawExpression = expression.trim();
        String normalized = rawExpression.toUpperCase(Locale.ROOT);

        // 从缓存中获取预定义常量
        SettlementPolicySpec predefined = CONSTANTS.get(normalized);
        if (predefined != null) {
            return predefined;
        }

        // 自定义账期 C@DD-DD 不使用 + 语法，需在通用周期表达式前解析。
        if (normalized.startsWith(PREFIX_CUSTOM_CYCLE)) {
            return parseCustomCycle(rawExpression);
        }

        // 产品 DSL：每日固定时间 D@HH:mm。
        if (normalized.startsWith(PREFIX_DAILY_AT)) {
            return parseDailyAt(normalized);
        }
        if (normalized.startsWith(PREFIX_NATURAL_DAY_DELAY)) {
            return parseNaturalDayDelay(rawExpression, normalized);
        }

        // 产品 DSL：W@MON、M@15、Q@03-31、Y@01-01。
        if (normalized.startsWith(PREFIX_WEEKLY_AT)) {
            return parseWeeklyAt(normalized);
        }
        if (normalized.startsWith(PREFIX_MONTHLY_AT)) {
            return parseMonthlyAt(normalized);
        }
        if (normalized.startsWith(PREFIX_QUARTERLY_AT)) {
            return parseQuarterlyAt(normalized);
        }
        if (normalized.startsWith(PREFIX_YEARLY_AT)) {
            return parseYearlyAt(normalized);
        }

        // 延迟结算 T+N
        if (normalized.startsWith(PREFIX_TRADE_DAY_DELAY)) {
            int days = parseInt(normalized.substring(EXPRESSION_PREFIX_LENGTH));
            if (days <= ZERO_INTERVAL) {
                throw new IllegalArgumentException("T delay must be positive: " + rawExpression);
            }
            return new SettlementPolicySpec(SettlementMode.DELAY_DAYS, days, null, null, normalized);
        }

        // 处理带 @ 的表达式
        String[] atParts = normalized.split(WindConstants.AT);
        AssertUtils.isTrue(atParts.length <= MAX_AT_PARTS,
                "Invalid format, expected at most one '@' character: " + rawExpression);
        String left = atParts[FIRST_PART_INDEX];
        String right = atParts.length > SINGLE_PART_LENGTH ? atParts[SECOND_PART_INDEX] : null;

        // 校验 @ 后面的部分不能包含负号（除非是合法的 L 或 Y 模式的 MM-dd）
        if (right != null && !LAST_DAY_TOKEN.equals(right) && right.contains(DATE_SEPARATOR)
                && !(left.charAt(PREFIX_CHAR_INDEX) == YEARLY_EXPRESSION_PREFIX && right.matches(REGEX_MONTH_DAY))) {
            throw new IllegalArgumentException("Invalid parameter, negative numbers not allowed: " + rawExpression);
        }

        char prefix = left.charAt(PREFIX_CHAR_INDEX);
        String numPart = left.substring(SINGLE_PART_LENGTH);
        if (!numPart.startsWith(INTERVAL_SEPARATOR)) {
            throw new IllegalArgumentException("Invalid format, expected '+' after prefix: " + rawExpression);
        }
        int interval = parseInt(numPart.substring(SINGLE_PART_LENGTH));
        if (interval <= ZERO_INTERVAL) {
            throw new IllegalArgumentException("Interval must be positive: " + rawExpression);
        }

        return switch (prefix) {
            case HOURLY_EXPRESSION_PREFIX -> new SettlementPolicySpec(
                    SettlementMode.HOURLY, interval, null, null, normalized);
            case WEEKLY_EXPRESSION_PREFIX -> {
                if (right == null) {
                    throw new IllegalArgumentException("Weekday must be specified after @: " + rawExpression);
                }
                int weekday = parseInt(right);
                if (weekday < FIRST_WEEKDAY || weekday > LAST_WEEKDAY) {
                    throw new IllegalArgumentException("Weekday must be 1-7, got: " + weekday);
                }
                yield new SettlementPolicySpec(SettlementMode.WEEKLY, interval, weekday, null, normalized);
            }
            case MONTHLY_EXPRESSION_PREFIX -> {
                if (right == null) {
                    throw new IllegalArgumentException(
                            "Day must be specified after @ (1-31 or L): " + rawExpression);
                }
                int day;
                if (LAST_DAY_TOKEN.equals(right)) {
                    day = LAST_DAY;
                } else {
                    day = parseInt(right);
                    if (day < FIRST_DAY || day > LAST_DAY_OF_MONTH) {
                        throw new IllegalArgumentException("Day must be 1-31 or L, got: " + day);
                    }
                }
                yield new SettlementPolicySpec(SettlementMode.MONTHLY, interval, day, null, normalized);
            }
            case QUARTERLY_EXPRESSION_PREFIX -> {
                if (right == null) {
                    yield new SettlementPolicySpec(SettlementMode.QUARTERLY, interval, null, null, normalized);
                } else if (LAST_DAY_TOKEN.equals(right)) {
                    yield new SettlementPolicySpec(SettlementMode.QUARTERLY, interval, LAST_DAY, null, normalized);
                } else {
                    throw new IllegalArgumentException("Quarterly only supports @L or no suffix: " + rawExpression);
                }
            }
            case YEARLY_EXPRESSION_PREFIX -> {
                if (right != null && right.matches(REGEX_MONTH_DAY)) {
                    String[] md = right.split(DATE_SEPARATOR);
                    int month = parseInt(md[FIRST_PART_INDEX]);
                    int day = parseInt(md[SECOND_PART_INDEX]);
                    if (month < FIRST_MONTH || month > LAST_MONTH) {
                        throw new IllegalArgumentException(YEAR_MONTH_RANGE_MESSAGE);
                    }
                    if (day < FIRST_DAY || day > LAST_DAY_OF_MONTH) {
                        throw new IllegalArgumentException(MONTH_DAY_RANGE_MESSAGE);
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
            case CUSTOM_CYCLE_EXPRESSION_PREFIX -> parseCustomCycle(rawExpression);
            default -> throw new IllegalArgumentException("Unknown settlement type: " + rawExpression);
        };
    }

    private static SettlementPolicySpec parseNaturalDayDelay(String rawExpression, String normalized) {
        int days = parseInt(normalized.substring(EXPRESSION_PREFIX_LENGTH));
        if (days <= ZERO_INTERVAL) {
            throw new IllegalArgumentException("D delay must be positive: " + rawExpression);
        }
        return new SettlementPolicySpec(SettlementMode.DELAY_NATURAL_DAYS, days, null, null, normalized);
    }

    private static SettlementPolicySpec parseCustomCycle(String expr) {
        if (!expr.toUpperCase(Locale.ROOT).startsWith(PREFIX_CUSTOM_CYCLE)) {
            throw new IllegalArgumentException("Custom cycle must start with C@: " + expr);
        }
        String body = expr.substring(EXPRESSION_PREFIX_LENGTH);
        Assert.hasText(body, "Custom cycle or range id must not be empty: " + expr);
        if (!body.matches(REGEX_CUSTOM_CYCLE)) {
            return new SettlementPolicySpec(SettlementMode.CUSTOM_RANGE, ZERO_INTERVAL, null, null, expr, body);
        }
        String[] parts = body.split(DATE_SEPARATOR);
        if (parts.length != PAIR_PART_LENGTH) {
            throw new IllegalArgumentException("Invalid custom cycle, expected C@dd-dd : " + expr);
        }
        int start = parseInt(parts[FIRST_PART_INDEX]);
        int end = parseInt(parts[SECOND_PART_INDEX]);
        if (start < FIRST_DAY || start > LAST_DAY_OF_MONTH) {
            throw new IllegalArgumentException("Custom cycle start day must be 1-31: " + start);
        }
        if (end < FIRST_DAY || end > LAST_DAY_OF_MONTH) {
            throw new IllegalArgumentException("Custom cycle end day must be 1-31: " + end);
        }
        return new SettlementPolicySpec(SettlementMode.CUSTOM_CYCLE, ZERO_INTERVAL, start, end, expr);
    }

    private static SettlementPolicySpec parseDailyAt(String expression) {
        String timeText = expression.substring(EXPRESSION_PREFIX_LENGTH);
        if (!timeText.matches(REGEX_HOUR_MINUTE)) {
            throw new IllegalArgumentException("Daily settlement time must be HH:mm: " + expression);
        }
        try {
            LocalTime time = LocalTime.parse(timeText);
            return new SettlementPolicySpec(
                    SettlementMode.DAILY_AT, DEFAULT_INTERVAL, time.getHour(), time.getMinute(), expression);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid daily settlement time: " + expression, ex);
        }
    }

    private static SettlementPolicySpec parseWeeklyAt(String expression) {
        String weekdayText = expression.substring(EXPRESSION_PREFIX_LENGTH);
        int weekday = parseWeekday(weekdayText);
        return new SettlementPolicySpec(SettlementMode.WEEKLY, DEFAULT_INTERVAL, weekday, null, expression);
    }

    private static SettlementPolicySpec parseMonthlyAt(String expression) {
        String dayText = expression.substring(EXPRESSION_PREFIX_LENGTH);
        int day = parseMonthDay(dayText);
        return new SettlementPolicySpec(SettlementMode.MONTHLY, DEFAULT_INTERVAL, day, null, expression);
    }

    private static SettlementPolicySpec parseQuarterlyAt(String expression) {
        String dateText = expression.substring(EXPRESSION_PREFIX_LENGTH);
        String[] parts = dateText.split(DATE_SEPARATOR);
        if (parts.length != PAIR_PART_LENGTH) {
            throw new IllegalArgumentException("Quarterly settlement date must be MM-dd: " + expression);
        }
        int quarterMonth = parseInt(parts[FIRST_PART_INDEX]);
        int day = parseInt(parts[SECOND_PART_INDEX]);
        if (quarterMonth < FIRST_QUARTER_MONTH_OFFSET || quarterMonth > LAST_QUARTER_MONTH_OFFSET) {
            throw new IllegalArgumentException("Quarterly month offset must be 1-3: " + expression);
        }
        if (day < FIRST_DAY || day > LAST_DAY_OF_MONTH) {
            throw new IllegalArgumentException("Quarterly day must be 1-31: " + expression);
        }
        return new SettlementPolicySpec(SettlementMode.QUARTERLY, DEFAULT_INTERVAL, quarterMonth, day, expression);
    }

    private static SettlementPolicySpec parseYearlyAt(String expression) {
        String dateText = expression.substring(EXPRESSION_PREFIX_LENGTH);
        String[] parts = dateText.split(DATE_SEPARATOR);
        if (parts.length != PAIR_PART_LENGTH) {
            throw new IllegalArgumentException("Yearly settlement date must be MM-dd: " + expression);
        }
        int month = parseInt(parts[FIRST_PART_INDEX]);
        int day = parseInt(parts[SECOND_PART_INDEX]);
        MonthDay.of(month, day);
        return new SettlementPolicySpec(SettlementMode.YEARLY, DEFAULT_INTERVAL, month, day, expression);
    }

    // ========================= 核心计算 =========================

    /**
     * 配置默认节假日日历，影响未显式传入日历的 T+N 结算时间计算。
     *
     * @param holidayCalendar 默认节假日日历，返回 true 表示该日期不参与交易日计数
     */
    public static void configureDefaultHolidayCalendar(SettlementHolidayCalendar holidayCalendar) {
        Assert.notNull(holidayCalendar, REQUIRED_HOLIDAY_CALENDAR_MESSAGE);
        DEFAULT_HOLIDAY_CALENDAR.set(holidayCalendar);
    }

    /**
     * 恢复默认节假日日历为仅识别周末。
     */
    public static void resetDefaultHolidayCalendar() {
        DEFAULT_HOLIDAY_CALENDAR.set(WEEKEND_ONLY_HOLIDAY_CALENDAR);
    }

    /**
     * 使用默认节假日日历计算下一次候选结算时间。
     *
     * @param now 当前时间
     * @return 下一次候选结算时间
     */
    public LocalDateTime nextSettlementTime(LocalDateTime now) {
        return nextSettlementTime(now, DEFAULT_HOLIDAY_CALENDAR.get());
    }

    /**
     * 使用指定节假日日历计算下一次候选结算时间。
     *
     * @param now 当前时间
     * @param holidayCalendar 节假日日历，返回 true 表示该日期不参与 T+N 交易日计数
     * @return 下一次候选结算时间
     */
    public LocalDateTime nextSettlementTime(LocalDateTime now, SettlementHolidayCalendar holidayCalendar) {
        Assert.notNull(holidayCalendar, REQUIRED_HOLIDAY_CALENDAR_MESSAGE);
        return switch (settlementMode) {
            case REALTIME -> now;
            case DELAY_DAYS -> nextBusinessDayDelay(now, interval, holidayCalendar);
            case DELAY_NATURAL_DAYS -> now.plusDays(interval);
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

    private static LocalDateTime nextBusinessDayDelay(LocalDateTime now,
                                                      int days,
                                                      SettlementHolidayCalendar holidayCalendar) {
        LocalDateTime candidate = now;
        int remaining = days;
        while (remaining > ZERO_INTERVAL) {
            candidate = candidate.plusDays(DEFAULT_INTERVAL);
            if (!holidayCalendar.isHoliday(candidate.toLocalDate())) {
                remaining--;
            }
        }
        return candidate;
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private static LocalDateTime nextDailyAt(LocalDateTime now, int hour, int minute) {
        LocalDateTime candidate = now.toLocalDate().atTime(hour, minute);
        if (!candidate.isAfter(now)) {
            return candidate.plusDays(DEFAULT_INTERVAL);
        }
        return candidate;
    }

    private static LocalDateTime nextHourly(LocalDateTime now, int intervalHours) {
        int hour = now.getHour();
        int alignedHour = ((hour / intervalHours) + DEFAULT_INTERVAL) * intervalHours;
        if (alignedHour >= HOURS_PER_DAY) {
            return now.plusDays(DEFAULT_INTERVAL).truncatedTo(ChronoUnit.DAYS).withHour(MIDNIGHT_HOUR);
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
        if (weeksDiff == ZERO_INTERVAL || weeksDiff % intervalWeeks != ZERO_INTERVAL) {
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
        boolean isEndOfMonth = targetDay == LAST_DAY;
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
        if (monthsDiff % intervalMonths != ZERO_INTERVAL) {
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
        return date.withDayOfMonth(FIRST_DAY);
    }

    private static LocalDateTime nextQuarterly(LocalDateTime now,
                                               int intervalQuarters,
                                               Integer targetMonthOrDay,
                                               Integer targetDay) {
        if (targetDay != null) {
            return nextQuarterlyAt(now, intervalQuarters, targetMonthOrDay, targetDay);
        }
        int currentQuarterStartMonth =
                (now.getMonthValue() - DEFAULT_INTERVAL) / MONTHS_PER_QUARTER * MONTHS_PER_QUARTER
                        + FIRST_MONTH;
        LocalDate currentQuarterStart = LocalDate.of(now.getYear(), currentQuarterStartMonth, FIRST_DAY);
        if (targetMonthOrDay == null) {
            long quartersToAdd = intervalQuarters;
            LocalDate targetDate = currentQuarterStart.plusMonths(MONTHS_PER_QUARTER * quartersToAdd);
            if (targetDate.isBefore(now.toLocalDate()) ||
                    (targetDate.isEqual(now.toLocalDate()) && now.toLocalTime().isAfter(LocalTime.MIDNIGHT))) {
                targetDate = targetDate.plusMonths(MONTHS_PER_QUARTER * intervalQuarters);
            }
            return targetDate.atStartOfDay();
        } else if (targetMonthOrDay == LAST_DAY) {
            LocalDate currentQuarterEnd = currentQuarterStart.plusMonths(MONTHS_PER_QUARTER)
                    .minusDays(DEFAULT_INTERVAL);
            if (currentQuarterEnd.isAfter(now.toLocalDate()) ||
                    (currentQuarterEnd.isEqual(now.toLocalDate()) && now.toLocalTime().equals(LocalTime.MIDNIGHT))) {
                return currentQuarterEnd.atStartOfDay();
            }
            LocalDate nextQuarterStart = currentQuarterStart.plusMonths(MONTHS_PER_QUARTER * intervalQuarters);
            LocalDate nextQuarterEnd = nextQuarterStart.plusMonths(MONTHS_PER_QUARTER).minusDays(DEFAULT_INTERVAL);
            if (nextQuarterEnd.isBefore(now.toLocalDate()) ||
                    (nextQuarterEnd.isEqual(now.toLocalDate()) && now.toLocalTime().isAfter(LocalTime.MIDNIGHT))) {
                nextQuarterEnd = nextQuarterStart.plusMonths(MONTHS_PER_QUARTER
                        * (intervalQuarters + DEFAULT_INTERVAL)).minusDays(DEFAULT_INTERVAL);
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
        int currentQuarterStartMonth =
                (now.getMonthValue() - DEFAULT_INTERVAL) / MONTHS_PER_QUARTER * MONTHS_PER_QUARTER
                        + FIRST_MONTH;
        LocalDate currentQuarterStart = LocalDate.of(now.getYear(), currentQuarterStartMonth, FIRST_DAY);
        LocalDate targetDate = quarterlyDate(currentQuarterStart, quarterMonth, targetDay);
        if (!targetDate.atStartOfDay().isAfter(now)) {
            targetDate = quarterlyDate(
                    currentQuarterStart.plusMonths(MONTHS_PER_QUARTER * intervalQuarters),
                    quarterMonth,
                    targetDay);
        }
        return targetDate.atStartOfDay();
    }

    private static LocalDate quarterlyDate(LocalDate quarterStart, int quarterMonth, int targetDay) {
        LocalDate month = quarterStart.plusMonths(quarterMonth - DEFAULT_INTERVAL);
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
        if (yearsDiff % intervalYears != ZERO_INTERVAL) {
            long remainder = intervalYears - (yearsDiff % intervalYears);
            targetDate = targetDate.plusYears(remainder);
        }
        return targetDate.atStartOfDay();
    }

    private static LocalDate getYearBase(LocalDate date) {
        return date.withDayOfYear(FIRST_DAY);
    }

    private static LocalDateTime nextCustomCycle(LocalDateTime now, int startDay, int endDay) {
        int today = now.getDayOfMonth();
        LocalDate targetDate;
        if (today >= startDay) {
            targetDate = now.toLocalDate().plusMonths(DEFAULT_INTERVAL);
        } else {
            targetDate = now.toLocalDate();
        }
        int lastDay = targetDate.lengthOfMonth();
        int realEnd = Math.min(endDay, lastDay);
        targetDate = targetDate.withDayOfMonth(realEnd);
        if (targetDate.equals(now.toLocalDate()) && now.toLocalTime().isAfter(LocalTime.MIDNIGHT)) {
            LocalDate nextMonth = targetDate.plusMonths(DEFAULT_INTERVAL);
            targetDate = nextMonth.withDayOfMonth(Math.min(endDay, nextMonth.lengthOfMonth()));
        } else if (targetDate.isBefore(now.toLocalDate())) {
            LocalDate nextMonth = targetDate.plusMonths(DEFAULT_INTERVAL);
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
            case WEEKDAY_MONDAY -> FIRST_WEEKDAY;
            case WEEKDAY_TUESDAY -> TUESDAY_WEEKDAY;
            case WEEKDAY_WEDNESDAY -> WEDNESDAY_WEEKDAY;
            case WEEKDAY_THURSDAY -> THURSDAY_WEEKDAY;
            case WEEKDAY_FRIDAY -> FRIDAY_WEEKDAY;
            case WEEKDAY_SATURDAY -> SATURDAY_WEEKDAY;
            case WEEKDAY_SUNDAY -> LAST_WEEKDAY;
            default -> {
                int weekday = parseInt(value);
                if (weekday < FIRST_WEEKDAY || weekday > LAST_WEEKDAY) {
                    throw new IllegalArgumentException("Weekday must be MON-SUN or 1-7, got: " + value);
                }
                yield weekday;
            }
        };
    }

    private static int parseMonthDay(String value) {
        if (LAST_DAY_TOKEN.equals(value)) {
            return LAST_DAY;
        }
        int day = parseInt(value);
        if (day < FIRST_DAY || day > LAST_DAY_OF_MONTH) {
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
        REALTIME(TYPE_REALTIME),
        DELAY_DAYS(TYPE_TRADE_DAY),
        DELAY_NATURAL_DAYS(TYPE_DAY),
        DAILY_AT(TYPE_DAY),
        HOURLY(TYPE_HOUR),
        WEEKLY(TYPE_WEEK),
        MONTHLY(TYPE_MONTH),
        QUARTERLY(TYPE_QUARTER),
        YEARLY(TYPE_YEAR),
        CUSTOM_CYCLE(TYPE_CUSTOM_CYCLE),
        CUSTOM_RANGE(TYPE_CUSTOM_CYCLE);

        private final String prefix;
    }

    /**
     * 节假日日历。返回 true 表示该日期不参与 T+N 交易日计数，
     * 通常包括周末和法定节假日。
     */
    @FunctionalInterface
    public interface SettlementHolidayCalendar {

        /**
         * 判断目标日期是否不参与 T+N 交易日计数。
         *
         * @param date 目标日期
         * @return true 表示节假日，false 表示交易日
         */
        boolean isHoliday(LocalDate date);
    }
}
