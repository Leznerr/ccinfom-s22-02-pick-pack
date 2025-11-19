package com.ccinfom.report.r1;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable DTO representing a single row from v_r1_daily_outcomes.
 */
public class R1DailyOutcomeRow {

    private final LocalDate calendarDate;
    private final int calendarYear;
    private final int calendarMonth;
    private final String monthNameLabel;
    private final String dayNameLabel;
    private final int deliveredTicketCount;
    private final int shortClosedTicketCount;
    private final int ticketsClosed;
    private final int shortageLineCount;
    private final BigDecimal shortageUnits;

    public R1DailyOutcomeRow(
            LocalDate calendarDate,
            int calendarYear,
            int calendarMonth,
            String monthNameLabel,
            String dayNameLabel,
            int deliveredTicketCount,
            int shortClosedTicketCount,
            int ticketsClosed,
            int shortageLineCount,
            BigDecimal shortageUnits) {
        this.calendarDate = calendarDate;
        this.calendarYear = calendarYear;
        this.calendarMonth = calendarMonth;
        this.monthNameLabel = monthNameLabel;
        this.dayNameLabel = dayNameLabel;
        this.deliveredTicketCount = deliveredTicketCount;
        this.shortClosedTicketCount = shortClosedTicketCount;
        this.ticketsClosed = ticketsClosed;
        this.shortageLineCount = shortageLineCount;
        this.shortageUnits = shortageUnits;
    }

    public LocalDate getCalendarDate() {
        return calendarDate;
    }

    public int getCalendarYear() {
        return calendarYear;
    }

    public int getCalendarMonth() {
        return calendarMonth;
    }

    public String getMonthNameLabel() {
        return monthNameLabel;
    }

    public String getDayNameLabel() {
        return dayNameLabel;
    }

    public int getDeliveredTicketCount() {
        return deliveredTicketCount;
    }

    public int getShortClosedTicketCount() {
        return shortClosedTicketCount;
    }

    public int getTicketsClosed() {
        return ticketsClosed;
    }

    public int getShortageLineCount() {
        return shortageLineCount;
    }

    public BigDecimal getShortageUnits() {
        return shortageUnits;
    }
}
