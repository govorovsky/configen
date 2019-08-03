package ru.mail.flexsettings.field;

public class LongField extends PrimitiveField<Long> {
    public LongField(String name) {
        super(name, 0L);
    }

    @Override
    public Field copy(String name) {
        return new LongField(name);
    }

    @Override
    public String toString() {
        return "(LongField) \"" + getName() + "\" = " + getValue();
    }
}
