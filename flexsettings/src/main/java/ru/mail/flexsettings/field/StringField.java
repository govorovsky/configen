package ru.mail.flexsettings.field;

public class StringField extends PrimitiveField<String> {
    public StringField(String name) {
        super(name, "");
    }

    @Override
    public Field copy(String name) {
        return new StringField(name);
    }

    @Override
    public String toString() {
        return "(StringField) \"" + getName() + "\" = \"" + getValue() + "\"";
    }
}
