package ru.mail.configen

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.*

const val reqCodeSetSettings = 1234

class MainActivity : AppCompatActivity() {


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val configView: TextView = findViewById(R.id.config)
        GlobalScope.launch(Dispatchers.Main) {
            val config = ConfigurationDelegate().loadConfig(applicationContext)
            configView.text = "received config: \n" +
                    "config.fieldRoot ${config.fieldRoot}\n" +
                    "config.feature.field1 ${config.feature.field1}\n" +
                    "config.feature.field2 ${config.feature.field2}\n" +
                    "config.feature.field3 ${config.feature.field3}\n" +
                    "config.feature.nestedConfig.nestedField1 ${config.feature.nestedConfig.nestedField1}\n" +
                    "config.feature.nestedConfig.nestedField2 ${config.feature.nestedConfig.nestedField2}\n" +
                    ""
        }
        findViewById<View>(R.id.dev_settings).setOnClickListener {
            val intent = Intent(this@MainActivity, DeveloperSettingsActivity::class.java)
            startActivityForResult(intent, reqCodeSetSettings)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqCodeSetSettings) {
            recreate()
        }
    }
}
