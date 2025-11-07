package com.ccinfom.model;

/**
 * Simple id + label pair for populating dropdowns without leaking DAO details into the UI.
 */
public class LookupValue {
    private final Long id;
    private final String label;

    public LookupValue(Long id, String label) {
        this.id = id;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
