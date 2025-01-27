package com.taskmanagementsystem;

import javafx.util.StringConverter;

public class ConverterUtils {

    /**
     * Returns a StringConverter for Category objects, displaying category.getName().
     */
    public static StringConverter<Category> getCategoryConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Category object) {
                if (object == null) return "";
                return object.getName();
            }

            @Override
            public Category fromString(String string) {
                // Not used if we're picking from a ComboBox
                return null;
            }
        };
    }

    /**
     * Returns a StringConverter for Priority objects, displaying priority.getName().
     */
    public static StringConverter<Priority> getPriorityConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Priority object) {
                if (object == null) return "";
                return object.getName();
            }

            @Override
            public Priority fromString(String string) {
                return null;
            }
        };
    }
}
