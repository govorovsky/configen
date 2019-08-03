package ru.mail.configen

import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.*
import ru.mail.flexsettings.FlexSettingsActivity

class DeveloperSettingsActivity : FlexSettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val field = ConfigSettingsDefinition.create()
        runBlocking {
            ConfigSettingsMapper.map(
                field.asStrictObject(),
                ConfigurationDelegate().loadConfig(this@DeveloperSettingsActivity),
                ConfigurationDelegate().loadDeveloperSettings(this@DeveloperSettingsActivity)
            )
        }
        showFieldScreen(field)
    }

    override fun onSaveSettings(settingsJson: String) {
        GlobalScope.async {
            ConfigurationDelegate().saveDeveloperSettings(applicationContext, settingsJson)
        }
        Toast.makeText(this, "Settings saved", Toast.LENGTH_LONG).show()
        setResult(reqCodeSetSettings)
        finish()
    }
}