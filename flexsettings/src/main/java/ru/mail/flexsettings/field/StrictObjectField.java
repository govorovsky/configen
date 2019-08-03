package ru.mail.flexsettings.field;

import java.util.Arrays;

public class StrictObjectField extends ObjectField {
    public StrictObjectField(String name, Field... fields) {
        super(name, Arrays.asList(fields));
    }

    @Override
    public Field copy(String name) {
        return null;
    }

    @Override
    public String toString() {
        return "(StrictObjectField) \"" + getName() + "\"";
    }
}
