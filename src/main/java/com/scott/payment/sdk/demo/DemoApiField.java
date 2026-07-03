package com.scott.payment.sdk.demo;

import java.util.Collections;
import java.util.List;

/**
 * Demo API request or response field metadata.
 */
public class DemoApiField {

    public static class Option {

        private final String value;
        private final String label;

        public Option(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    private final String name;
    private final String label;
    private final String description;
    private final boolean required;
    private final String defaultValue;
    private final String placeholder;
    private final boolean textarea;
    private final List<Option> options;

    public DemoApiField(String name,
                        String label,
                        String description,
                        boolean required,
                        String defaultValue,
                        String placeholder,
                        boolean textarea,
                        List<Option> options) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.required = required;
        this.defaultValue = defaultValue;
        this.placeholder = placeholder;
        this.textarea = textarea;
        this.options = options == null ? Collections.<Option>emptyList() : Collections.unmodifiableList(options);
    }

    public static DemoApiField input(String name,
                                     String label,
                                     String description,
                                     boolean required,
                                     String defaultValue,
                                     String placeholder) {
        return new DemoApiField(name, label, description, required, defaultValue, placeholder, false, null);
    }

    public static DemoApiField textarea(String name,
                                        String label,
                                        String description,
                                        boolean required,
                                        String defaultValue,
                                        String placeholder) {
        return new DemoApiField(name, label, description, required, defaultValue, placeholder, true, null);
    }

    public static DemoApiField select(String name,
                                      String label,
                                      String description,
                                      boolean required,
                                      String defaultValue,
                                      String placeholder,
                                      List<Option> options) {
        return new DemoApiField(name, label, description, required, defaultValue, placeholder, false, options);
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public boolean isTextarea() {
        return textarea;
    }

    public List<Option> getOptions() {
        return options;
    }

    public boolean isSelect() {
        return !options.isEmpty();
    }
}
