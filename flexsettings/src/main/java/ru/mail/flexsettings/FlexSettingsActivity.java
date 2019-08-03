package ru.mail.flexsettings;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import org.json.JSONException;

import ru.mail.flexsettings.field.Field;

public abstract class FlexSettingsActivity extends AppCompatActivity {
    private Field mBaseField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flex_settings);
    }

    protected void showFieldScreen(Field baseField) {
        mBaseField = baseField;
        setFragment(new FieldFragment(baseField, new FieldChangeListener() {
            @Override
            public void onChanged() {

            }
        }));
    }

    public void setFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();

        if (backStackEntryCount == 1) {
            try {
                onSaveSettings(new SettingToJsonMapper().map(mBaseField.asStrictObject()));
            } catch (JSONException e) {
                finish();
            }
        } else {
            super.onBackPressed();
        }
    }

    public abstract void onSaveSettings(String settingsJson);
}
