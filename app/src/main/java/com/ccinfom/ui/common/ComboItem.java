package com.ccinfom.ui.common;

/**
 * Generic combo-box item wrapper that holds both the display label and the
 * associated domain object.
 *
 * @param <T> domain model type
 */
public final class ComboItem<T> {
    private final T value;
    private final String label;

    public ComboItem(T value, String label) {
        this.value = value;
        this.label = label;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return label;
    }
}
