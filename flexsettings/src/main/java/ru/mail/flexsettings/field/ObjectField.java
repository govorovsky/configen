package ru.mail.flexsettings.field;

import java.util.Collections;
import java.util.List;

public abstract class ObjectField extends Field {
    private final List<Field> mFields;

    protected ObjectField(String name, List<Field> fields) {
        super(name);
        Collections.sort(fields);
        mFields = fields;
    }

    public List<Field> getAllFields() {
        return mFields;
    }

    public Field getField(String name) {
        for (Field field : mFields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        throw new IllegalStateException("field '" + name + "' absent in field " + getName());
    }
}
