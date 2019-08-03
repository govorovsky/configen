package ru.mail.flexsettings.field;

public class EmptyField extends PrimitiveField<Void> {
    public EmptyField(String name) {
        super(name, null);
    }

    @Override
    public Field copy(String name) {
        return new EmptyField(name);
    }

    @Override
    public String toString() {
        return "(EmptyField) \"" + getName() + "\"";
    }
}
