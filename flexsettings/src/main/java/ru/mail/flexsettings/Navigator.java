package ru.mail.flexsettings;

import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import ru.mail.flexsettings.field.ObjectField;

public class Navigator {
    private final FlexSettingsActivity mMainActivity;

    public Navigator(FlexSettingsActivity activity) {
        mMainActivity = activity;
    }

    public void editPrimitiveField(String name, String defValue, boolean isNumber, final EditListener<String> listener) {
        final EditText edittext = new EditText(mMainActivity);
        if (isNumber) {
            edittext.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        }

        if (defValue == null) {
            defValue = "";
        }
        edittext.setText(defValue);
        edittext.setSelection(defValue.length());

        new AlertDialog.Builder(mMainActivity)
                .setTitle("Set '" + name + "' value:")
                .setView(edittext)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = edittext.getText().toString();
                        listener.onChanged(value);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    public void showObjectField(ObjectField objectField, FieldChangeListener changeListener) {
        mMainActivity.setFragment(new FieldFragment(objectField, changeListener));
    }

    public void showUnsupported() {
        Toast.makeText(
                mMainActivity.getApplicationContext(),
                "Operation unsupported",
                Toast.LENGTH_SHORT
        ).show();
    }

    public interface EditListener<T> {
        void onChanged(T value);
    }
}
