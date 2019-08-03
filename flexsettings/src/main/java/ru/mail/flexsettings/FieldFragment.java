package ru.mail.flexsettings;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;
import ru.mail.flexsettings.field.Field;

public class FieldFragment extends Fragment {
    private Field mField;
    private FieldChangeListener mChangeListener;

    public FieldFragment() {
    }

    @SuppressLint("ValidFragment")
    public FieldFragment(Field field, FieldChangeListener changeListener) {
        this();
        mField = field;
        mChangeListener = changeListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment, container, false);
        ListView list = view.findViewById(R.id.free_type_fragment);

        if (mField.isObject()) {
            FieldAdapter adapter = new FieldAdapter((FlexSettingsActivity) getActivity(), mField.asObject(), mChangeListener);
            list.setAdapter(adapter);
        }

        return view;
    }
}
