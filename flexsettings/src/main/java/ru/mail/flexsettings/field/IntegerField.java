package ru.mail.flexsettings.field;

public class IntegerField extends PrimitiveField<Integer> {
    public IntegerField(String name) {
        super(name, 0);
    }

    @Override
    public Field copy(String name) {
        return new IntegerField(name);
    }

    @Override
    public String toString() {
        return "(IntegerField) \"" + getName() + "\" = " + getValue();
    }
}
