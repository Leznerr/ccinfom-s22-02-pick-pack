package com.ccinfom.report.r2;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable DTO representing a single row from v_r2_weekly_picker_productivity.
 */
public class R2WeeklyProductivityRow {

    private final int isoYear;
    private final int isoWeek;
    private final LocalDate isoWeekStart;
    private final LocalDate isoWeekEnd;
    private final Long pickerEmployeeId;
    private final String pickerFirstName;
    private final String pickerLastName;
    private final String productCategory;
    private final Long linesPicked;
    private final BigDecimal unitsPicked;
    private final BigDecimal unitsPerHour;
    private final Long avgPickTimeMin;
    private final Long shortErrorLines;

    public R2WeeklyProductivityRow(
            int isoYear,
            int isoWeek,
            LocalDate isoWeekStart,
            LocalDate isoWeekEnd,
            Long pickerEmployeeId,
            String pickerFirstName,
            String pickerLastName,
            String productCategory,
            Long linesPicked,
            BigDecimal unitsPicked,
            BigDecimal unitsPerHour,
            Long avgPickTimeMin,
            Long shortErrorLines) {
        this.isoYear = isoYear;
        this.isoWeek = isoWeek;
        this.isoWeekStart = isoWeekStart;
        this.isoWeekEnd = isoWeekEnd;
        this.pickerEmployeeId = pickerEmployeeId;
        this.pickerFirstName = pickerFirstName;
        this.pickerLastName = pickerLastName;
        this.productCategory = productCategory;
        this.linesPicked = linesPicked;
        this.unitsPicked = unitsPicked;
        this.unitsPerHour = unitsPerHour;
        this.avgPickTimeMin = avgPickTimeMin;
        this.shortErrorLines = shortErrorLines;
    }

    // Getters
    public int getIsoYear() 
    { 
        return isoYear; 
    }

    public int getIsoWeek() 
    { 
        return isoWeek; 
    }

    public LocalDate getIsoWeekStart() 
    { 
        return isoWeekStart; 
    }

    public LocalDate getIsoWeekEnd() 
    { 
        return isoWeekEnd; 
    }

    public Long getPickerEmployeeId() 
    { 
        return pickerEmployeeId; 
    }

    public String getPickerFirstName() 
    { 
        return pickerFirstName; 
    }

    public String getPickerLastName() 
    { 
        return pickerLastName; 
    }

    public String getProductCategory() 
    { 
        return productCategory; 
    }

    public Long getLinesPicked() 
    { 
        return linesPicked; 
    }

    public BigDecimal getUnitsPicked() 
    { 
        return unitsPicked; 
    }

    public BigDecimal getUnitsPerHour() 
    { 
        return unitsPerHour; 
    }

    public Long getAvgPickTimeMin() 
    { 
        return avgPickTimeMin; 
    };

    public Long getShortErrorLines() 
    { 
        return shortErrorLines; 
    }

    // Convenience method for full name display
    public String getPickerFullName() {
        return pickerFirstName + " " + pickerLastName;
    }

    // Convenience method for week range display
    public String getWeekRangeDisplay() {
        return isoWeekStart + " to " + isoWeekEnd;
    }
}