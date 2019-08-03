package ru.mail.flexsettings;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import ru.mail.flexsettings.field.BooleanField;
import ru.mail.flexsettings.field.Field;
import ru.mail.flexsettings.field.IntegerField;
import ru.mail.flexsettings.field.LongField;
import ru.mail.flexsettings.field.ObjectField;
import ru.mail.flexsettings.field.StringField;

class FieldViewFactory {
    private final LayoutInflater mLayoutInflater;
    private final Navigator mNavigator;

    FieldViewFactory(LayoutInflater layoutInflater, Navigator navigator) {
        mLayoutInflater = layoutInflater;
        mNavigator = navigator;
    }

    public FieldViewHolder createView(final Field field, ViewGroup parent, final FieldChangeListener changeListener) {
        if (field.isBoolean()) {
            return createBooleanView(field.asBoolean(), parent, changeListener);
        } else if (field.isString()) {
            return createStringView(field.asString(), parent, changeListener);
        } else if (field.isInteger()) {
            return createIntegerView(field.asInteger(), parent, changeListener);
        } else if (field.isLong()) {
            return createLongView(field.asLong(), parent, changeListener);
        } else if (field.isObject()) {
            return createObjectView(field.asObject(), parent, changeListener);
        } else {
            return createUndefinedView(parent);
        }
    }

    private FieldViewHolder createBooleanView(final BooleanField booleanField, ViewGroup parent, final FieldChangeListener changeListener) {
        View view = mLayoutInflater.inflate(R.layout.item_boolean, parent, false);

        final CheckBox valueCheckBox = view.findViewById(R.id.checkbox);
        valueCheckBox.setChecked(booleanField.getValue());

        return new FieldViewHolder(view, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                valueCheckBox.toggle();
                booleanField.setValue(valueCheckBox.isChecked());
                changeListener.onChanged();
            }
        });
    }

    private FieldViewHolder createStringView(final StringField stringField, ViewGroup parent, final FieldChangeListener changeListener) {
        View view = mLayoutInflater.inflate(R.layout.item_string, parent, false);
        view.<TextView>findViewById(R.id.value).setText(stringField.getValue());
        return new FieldViewHolder(view, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNavigator.editPrimitiveField(stringField.getName(), stringField.getValue(), false, new Navigator.EditListener<String>() {
                    @Override
                    public void onChanged(String value) {
                        stringField.setValue(value);
                        changeListener.onChanged();
                    }
                });
            }
        });
    }

    private FieldViewHolder createIntegerView(final IntegerField field, ViewGroup parent, final FieldChangeListener changeListener) {
        View view = mLayoutInflater.inflate(R.layout.item_number, parent, false);
        final String value = field.getValue().toString();
        view.<TextView>findViewById(R.id.value).setText(value);
        return new FieldViewHolder(view, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNavigator.editPrimitiveField(field.getName(), value, true, new Navigator.EditListener<String>() {
                    @Override
                    public void onChanged(String value) {
                        field.setValue(Integer.parseInt(value));
                        changeListener.onChanged();
                    }
                });
            }
        });
    }

    private FieldViewHolder createLongView(final LongField field, ViewGroup parent, final FieldChangeListener changeListener) {
        View view = mLayoutInflater.inflate(R.layout.item_number, parent, false);
        final String value = field.getValue().toString();
        view.<TextView>findViewById(R.id.value).setText(value);
        return new FieldViewHolder(view, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNavigator.editPrimitiveField(field.getName(), value, true, new Navigator.EditListener<String>() {
                    @Override
                    public void onChanged(String value) {
                        field.asLong().setValue(Long.parseLong(value));
                        changeListener.onChanged();
                    }
                });
            }
        });
    }

    private FieldViewHolder createObjectView(final ObjectField field, ViewGroup parent, final FieldChangeListener changeListener) {
        View view = mLayoutInflater.inflate(R.layout.item_object, parent, false);
        view.<TextView>findViewById(R.id.value).setText("➡️");
        return new FieldViewHolder(view, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNavigator.showObjectField(field.asObject(), new FieldChangeListener() {
                    @Override
                    public void onChanged() {
                        changeListener.onChanged();
                    }
                });
            }
        });
    }

    private FieldViewHolder createUndefinedView(ViewGroup parent) {
        View view = mLayoutInflater.inflate(R.layout.item_object, parent, false);
        view.<TextView>findViewById(R.id.value).setText("❌");
        return new FieldViewHolder(view, Color.LTGRAY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNavigator.showUnsupported();
            }
        });
    }
}
