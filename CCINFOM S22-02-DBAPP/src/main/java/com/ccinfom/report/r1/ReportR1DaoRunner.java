package com.ccinfom.report.r1;

import java.sql.SQLException;
import java.util.List;

/**
 * Simple console runner for quick verification during Stage 3.
 */
public final class ReportR1DaoRunner {

    private ReportR1DaoRunner() {
    }

    public static void main(String[] args) throws SQLException {
        ReportR1Dao dao = new ReportR1Dao();
        List<R1DailyOutcomeRow> rows = dao.findDailyOutcomes(2025, 11);

        System.out.println("R1 Daily Outcomes (Year=2025, Month=11)");
        for (R1DailyOutcomeRow row : rows) {
            System.out.printf(
                    "%s | delivered=%d | shortClosed=%d | tickets=%d | shortageLines=%d | shortageUnits=%s%n",
                    row.getCalendarDate(),
                    row.getDeliveredTicketCount(),
                    row.getShortClosedTicketCount(),
                    row.getTicketsClosed(),
                    row.getShortageLineCount(),
                    row.getShortageUnits()
            );
        }
    }
}
