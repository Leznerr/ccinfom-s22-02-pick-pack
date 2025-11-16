package com.ccinfom.report.r2;

/**
 * Filter container for R2 report queries.
 */
public class R2ProductivityFilters {
    private final int isoYear;
    private final Integer isoWeek;
    private final Long pickerEmployeeId;
    private final String productCategory;

    private R2ProductivityFilters(Builder builder) {
        this.isoYear = builder.isoYear;
        this.isoWeek = builder.isoWeek;
        this.pickerEmployeeId = builder.pickerEmployeeId;
        this.productCategory = builder.productCategory;
    }

    // Getters
    public int getIsoYear() { return isoYear; }
    public Integer getIsoWeek() { return isoWeek; }
    public Long getPickerEmployeeId() { return pickerEmployeeId; }
    public String getProductCategory() { return productCategory; }

    public static Builder builder(int isoYear) {
        return new Builder(isoYear);
    }

    public static class Builder {
        private final int isoYear;
        private Integer isoWeek;
        private Long pickerEmployeeId;
        private String productCategory;

        public Builder(int isoYear) {
            this.isoYear = isoYear;
        }

        public Builder isoWeek(Integer isoWeek) {
            this.isoWeek = isoWeek;
            return this;
        }

        public Builder pickerEmployeeId(Long pickerEmployeeId) {
            this.pickerEmployeeId = pickerEmployeeId;
            return this;
        }

        public Builder productCategory(String productCategory) {
            this.productCategory = productCategory;
            return this;
        }

        public R2ProductivityFilters build() {
            return new R2ProductivityFilters(this);
        }
    }
}