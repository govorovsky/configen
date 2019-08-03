package ru.mail.flexsettings.field;

public class BooleanField extends PrimitiveField<Boolean> {
    public BooleanField(String name) {
        super(name, false);
    }

    @Override
    public Field copy(String name) {
        return new BooleanField(name);
    }

    @Override
    public String toString() {
        return "(BooleanField) \"" + getName() + "\" = " + getValue();
    }
}
