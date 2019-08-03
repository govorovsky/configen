package ru.mail.flexsettings.field;

public abstract class Field implements Comparable<Field> {
    private final String mName;
    private boolean mIsChanged;

    public Field(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public boolean isChanged() {
        return mIsChanged;
    }

    public void markChanged() {
        mIsChanged = true;
    }

    public void setChanged(boolean isChanged) {
        mIsChanged = isChanged;
    }

    public static StringField string(String name) {
        return new StringField(name);
    }

    public boolean isString() {
        return this instanceof StringField;
    }

    public StringField asString() {
        return (StringField) this;
    }

    public static BooleanField bool(String name) {
        return new BooleanField(name);
    }

    public boolean isBoolean() {
        return this instanceof BooleanField;
    }

    public BooleanField asBoolean() {
        return (BooleanField) this;
    }

    public static IntegerField integer(String name) {
        return new IntegerField(name);
    }

    public boolean isInteger() {
        return this instanceof IntegerField;
    }

    public IntegerField asInteger() {
        return (IntegerField) this;
    }

    public static LongField longField(String name) {
        return new LongField(name);
    }

    public boolean isLong() {
        return this instanceof LongField;
    }

    public LongField asLong() {
        return (LongField) this;
    }

    public static StrictObjectField strictObject(String name, Field... fields) {
        return new StrictObjectField(name, fields);
    }

    public boolean isStrictObject() {
        return this instanceof StrictObjectField;
    }

    public StrictObjectField asStrictObject() {
        return (StrictObjectField) this;
    }

    public boolean isObject() {
        return this instanceof ObjectField;
    }

    public ObjectField asObject() {
        return (ObjectField) this;
    }

    public static FreeObjectField freeObject(String name, Field baseField) {
        return new FreeObjectField(name, baseField);
    }

    public boolean isFreeObject() {
        return this instanceof FreeObjectField;
    }

    public FreeObjectField asFreeObject() {
        return (FreeObjectField) this;
    }

    public PrimitiveField<?> asPrimitive() {
        return (PrimitiveField<?>) this;
    }

    public static EmptyField empty(String name) {
        return new EmptyField(name);
    }

    public abstract Field copy(String name);

    @Override
    public int compareTo(Field o) {
        return getName().compareTo(o.getName());
    }
}
