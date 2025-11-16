package com.ccinfom.report.r2;

import java.sql.SQLException;
import java.util.List;

/**
 * Simple console runner for quick verification during Stage 3.
 */
public final class ReportR2DaoRunner {

    private ReportR2DaoRunner() {
    }

    public static void main(String[] args) throws SQLException {
        ReportR2Dao dao = new ReportR2Dao();
        
        // Test 1: Whole year summary
        System.out.println("=== R2 Weekly Picker Productivity (Year=2025) ===");
        List<R2WeeklyProductivityRow> yearlyRows = dao.findWeeklyProductivity(2025, null);
        printResults(yearlyRows);
        
        // Test 2: Specific week with filters
        System.out.println("\n=== R2 Weekly Picker Productivity (Year=2025, Week=45) ===");
        List<R2WeeklyProductivityRow> weeklyRows = dao.findWeeklyProductivity(2025, 45);
        printResults(weeklyRows);
        
        // Test 3: Using filters builder
        System.out.println("\n=== R2 Using Filters Builder ===");
        R2ProductivityFilters filters = R2ProductivityFilters.builder(2025)
            .isoWeek(45)
            .productCategory("Electronics") // Example category
            .build();
        List<R2WeeklyProductivityRow> filteredRows = dao.findWeeklyProductivity(filters);
        printResults(filteredRows);
    }

    private static void printResults(List<R2WeeklyProductivityRow> rows) {
        if (rows.isEmpty()) {
            System.out.println("No data found for the given filters.");
            return;
        }

        for (R2WeeklyProductivityRow row : rows) {
            System.out.printf(
                "Week %d-%d | %s | %s | Lines: %d | Units: %.1f | Units/Hour: %.2f | Avg Time: %d min | Errors: %d%n",
                row.getIsoYear(),
                row.getIsoWeek(),
                row.getPickerFullName(),
                row.getProductCategory(),
                row.getLinesPicked(),
                row.getUnitsPicked(),
                row.getUnitsPerHour(),
                row.getAvgPickTimeMin(),
                row.getShortErrorLines()
            );
        }
        System.out.println("Total rows: " + rows.size());
    }
}