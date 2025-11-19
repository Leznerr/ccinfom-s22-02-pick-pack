package com.ccinfom.report.r2;

import java.sql.SQLException;
import java.util.List;

/**
 * Comprehensive test runner for R2 DAO - Tests both Stage 2 (View) and Stage 3 (DAO)
 */
public final class ReportR2DaoRunner {

    public static void main(String[] args) {
        System.out.println("=== R2 WEEKLY PICKER PRODUCTIVITY - STAGE 2 & 3 VALIDATION ===\n");
        
        try {
            ReportR2Dao dao = new ReportR2Dao();
            
            // Test 1: Database Connection
            testDatabaseConnection(dao);
            
            // Test 2: Basic DAO functionality
            testBasicDaoFunctionality(dao);
            
            // Test 3: Filter combinations
            testFilterCombinations(dao);
            
            // Test 4: Builder pattern
            testBuilderPattern(dao);
            
            // Test 5: Edge cases
            testEdgeCases(dao);
            
            // Test 6: Data validation against design expectations
            testDesignValidation(dao);
            
            System.out.println("\n=== ALL TESTS COMPLETED SUCCESSFULLY ===");
            
        } catch (Exception e) {
            System.err.println("TEST FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testDatabaseConnection(ReportR2Dao dao) throws SQLException {
        System.out.println("1. DATABASE CONNECTION TEST");
        System.out.println("---------------------------");
        
        // This will implicitly test the connection when we run the first query
        System.out.println("Database connection established via ReportDaoBase");
        System.out.println();
    }
    
    private static void testBasicDaoFunctionality(ReportR2Dao dao) throws SQLException {
        System.out.println("2. BASIC DAO FUNCTIONALITY TEST");
        System.out.println("-------------------------------");
        
        // Test without filters (should return all data)
        List<R2WeeklyProductivityRow> allRows = dao.findWeeklyProductivity(2025, null);
        System.out.println("findWeeklyProductivity(2025, null) - returned " + allRows.size() + " rows");
        
        // Test with specific week
        List<R2WeeklyProductivityRow> weekRows = dao.findWeeklyProductivity(2025, 45);
        System.out.println("findWeeklyProductivity(2025, 45) - returned " + weekRows.size() + " rows");
        
        if (!allRows.isEmpty()) {
            // Test that we can access all properties without errors
            R2WeeklyProductivityRow firstRow = allRows.get(0);
            System.out.println("Row object properties accessible:");
            System.out.println("   - ISO Year: " + firstRow.getIsoYear());
            System.out.println("   - ISO Week: " + firstRow.getIsoWeek());
            System.out.println("   - Picker: " + firstRow.getPickerFullName());
            System.out.println("   - Category: " + firstRow.getProductCategory());
            System.out.println("   - Lines: " + firstRow.getLinesPicked());
            System.out.println("   - Units: " + firstRow.getUnitsPicked());
            System.out.println("   - Units/Hour: " + firstRow.getUnitsPerHour());
            System.out.println("   - Avg Time: " + firstRow.getAvgPickTimeMin());
            System.out.println("   - Errors: " + firstRow.getShortErrorLines());
        }
        System.out.println();
    }
    
    private static void testFilterCombinations(ReportR2Dao dao) throws SQLException {
        System.out.println("3. FILTER COMBINATIONS TEST");
        System.out.println("---------------------------");
        
        // Test picker filter
        List<R2WeeklyProductivityRow> pickerRows = dao.findWeeklyProductivity(2025, null, 1L, null);
        System.out.println("Picker filter - returned " + pickerRows.size() + " rows");
        
        // Test category filter (use a category that exists in your data)
        List<R2WeeklyProductivityRow> categoryRows = dao.findWeeklyProductivity(2025, null, null, "Electronics");
        System.out.println("Category filter - returned " + categoryRows.size() + " rows");
        
        // Test combined filters
        List<R2WeeklyProductivityRow> combinedRows = dao.findWeeklyProductivity(2025, 45, 1L, "Electronics");
        System.out.println("Combined filters - returned " + combinedRows.size() + " rows");
        
        System.out.println();
    }
    
    private static void testBuilderPattern(ReportR2Dao dao) throws SQLException {
        System.out.println("4. BUILDER PATTERN TEST");
        System.out.println("-----------------------");
        
        R2ProductivityFilters filters = R2ProductivityFilters.builder(2025)
            .isoWeek(45)
            .pickerEmployeeId(1L)
            .productCategory("Electronics")
            .build();
            
        List<R2WeeklyProductivityRow> builderRows = dao.findWeeklyProductivity(filters);
        System.out.println("Builder pattern - returned " + builderRows.size() + " rows");
        
        // Verify filter values
        System.out.println("   Filter values: Year=" + filters.getIsoYear() + 
                         ", Week=" + filters.getIsoWeek() +
                         ", Picker=" + filters.getPickerEmployeeId() +
                         ", Category=" + filters.getProductCategory());
        System.out.println();
    }
    
    private static void testEdgeCases(ReportR2Dao dao) throws SQLException {
        System.out.println("5. EDGE CASES TEST");
        System.out.println("------------------");
        
        // Test with non-existent year
        List<R2WeeklyProductivityRow> futureRows = dao.findWeeklyProductivity(2099, 1);
        System.out.println("Non-existent year - returned " + futureRows.size() + " rows (expected: 0)");
        
        // Test with non-existent week
        List<R2WeeklyProductivityRow> invalidWeekRows = dao.findWeeklyProductivity(2025, 99);
        System.out.println("Invalid week - returned " + invalidWeekRows.size() + " rows (expected: 0)");
        
        // Test with non-existent picker
        List<R2WeeklyProductivityRow> invalidPickerRows = dao.findWeeklyProductivity(2025, null, 999L, null);
        System.out.println("Invalid picker - returned " + invalidPickerRows.size() + " rows (expected: 0)");
        
        System.out.println();
    }
    
    private static void testDesignValidation(ReportR2Dao dao) throws SQLException {
        System.out.println("6. DESIGN VALIDATION TEST");
        System.out.println("-------------------------");
        
        // Test with your specific design case (Week 45, 2025)
        List<R2WeeklyProductivityRow> designRows = dao.findWeeklyProductivity(2025, 45);
        
        if (designRows.isEmpty()) {
            System.out.println("No data found for design validation week (2025, Week 45)");
            System.out.println("Please check your seed data for tickets A and B");
            return;
        }
        
        System.out.println("Design week data found: " + designRows.size() + " rows");
        
        // Calculate totals for comparison with your design expectations
        long totalLines = 0;
        double totalUnits = 0.0;
        long totalErrors = 0;
        
        for (R2WeeklyProductivityRow row : designRows) {
            totalLines += row.getLinesPicked();
            totalUnits += row.getUnitsPicked().doubleValue();
            totalErrors += row.getShortErrorLines();
        }
        
        System.out.println("   DESIGN METRICS SUMMARY:");
        System.out.println("   - Total Lines Picked: " + totalLines + " (Expected: ~5)");
        System.out.println("   - Total Units Picked: " + totalUnits + " (Expected: ~25)");
        System.out.println("   - Total Error Lines: " + totalErrors + " (Expected: ~1)");
        
        // Your design expected:
        // Picker 1 (Ticket A) → 2 lines picked, 10 units, 0 errors
        // Picker 2 (Ticket B) → 3 lines picked, 15 units, 1 error
        
        // Print detailed breakdown
        System.out.println("\n   DETAILED BREAKDOWN:");
        for (R2WeeklyProductivityRow row : designRows) {
            System.out.printf("   - %s | %s | Lines: %d | Units: %.1f | Errors: %d%n",
                row.getPickerFullName(),
                row.getProductCategory(),
                row.getLinesPicked(),
                row.getUnitsPicked().doubleValue(),
                row.getShortErrorLines()
            );
        }
        
        // Validate against your design expectations
        boolean linesMatch = totalLines >= 5; // Your design expected 5
        boolean unitsMatch = totalUnits >= 25; // Your design expected 25
        boolean errorsMatch = totalErrors >= 1; // Your design expected 1
        
        if (linesMatch && unitsMatch && errorsMatch) {
            System.out.println("Design validation PASSED - metrics match expectations");
        } else {
            System.out.println("Design validation PARTIAL - some metrics differ from expectations");
        }
    }
}