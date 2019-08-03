package ru.mail.flexsettings.field;

public abstract class PrimitiveField<T> extends Field {
    private T mValue;

    public PrimitiveField(String name, T defaultValue) {
        super(name);
        mValue = defaultValue;
    }

    public T getValue() {
        return mValue;
    }

    public void setValue(T value) {
        mValue = value;
    }
}
