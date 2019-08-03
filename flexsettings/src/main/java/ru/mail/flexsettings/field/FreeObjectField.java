package ru.mail.flexsettings.field;

import java.util.ArrayList;

public class FreeObjectField extends ObjectField {
    private final Field mBaseField;

    public FreeObjectField(String name, Field baseField) {
        super(name, new ArrayList<Field>());
        mBaseField = baseField;
    }

    public Field getBaseField() {
        return mBaseField;
    }

    public Field addField(String name) {
        Field copy = mBaseField.copy(name);
        getAllFields().add(copy);
        return copy;
    }

    @Override
    public Field copy(String name) {
        return null;
    }

    @Override
    public String toString() {
        return "(FreeObjectField) \"" + getName() + "\"";
    }
}
