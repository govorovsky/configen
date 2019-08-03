package ru.mail.flexsettings;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import ru.mail.flexsettings.field.Field;
import ru.mail.flexsettings.field.ObjectField;

public class FieldAdapter extends BaseAdapter {
    private final ObjectField mField;
    private final FieldChangeListener mChangeListener;
    private final FieldViewFactory mFieldViewFactory;

    public FieldAdapter(FlexSettingsActivity context, ObjectField field, FieldChangeListener changeListener) {
        mField = field;
        mChangeListener = changeListener;
        mFieldViewFactory = new FieldViewFactory(LayoutInflater.from(context), new Navigator(context));
    }

    @Override
    public int getCount() {
        return mField.getAllFields().size();
    }

    @Override
    public Object getItem(int position) {
        return mField.getAllFields().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        final Field field = mField.getAllFields().get(position);
        FieldViewHolder viewHolder = mFieldViewFactory.createView(field, parent, new FieldChangeListener() {
            @Override
            public void onChanged() {
                onFieldChanged(field);
            }
        });
        view = viewHolder.getView();
        view.<TextView>findViewById(R.id.key).setText(field.getName());
//        view.setBackgroundColor(field.isChanged() ? Color.YELLOW : viewHolder.getColor());
        view.setBackgroundColor(viewHolder.getColor());
        view.setOnClickListener(viewHolder.getListener());
        return view;
    }

    private void onFieldChanged(Field field) {
        field.markChanged();
        markChanged(mField);
        mChangeListener.onChanged();
        notifyDataSetChanged();
    }

    private void markChanged(Field field) {
        field.markChanged();
        if (field.isFreeObject()) {
            for (Field subField : mField.getAllFields()) {
                markChanged(subField);
            }
        }
    }
}
