package ru.mail.configen

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class ConfigurationDelegate {

    private val devConfig = "dev_config.json"
    private val etalonConfig = "etalon_config.json"

    suspend fun loadConfig(context: Context): DTOConfiguration {
        return withContext(Dispatchers.IO) {
            val parser = DTOConfigurationJsonParser(AnalyticsSenderImpl())
            val file = File(context.filesDir, devConfig)
            var developerSettings: DTOConfigurationImpl? = null
            if (file.exists()) {
                developerSettings = parser.parse(JSONObject(file.readText()))
            }
            val etalon = parser.parse(
                JSONObject(context.resources.assets.open(etalonConfig)
                    .bufferedReader()
                    .use {
                        it.readText()
                    })
            )
            if (developerSettings != null) {
                developerSettings.merge(etalon)
                developerSettings
            } else {
                etalon
            }
        }
    }

    suspend fun loadDeveloperSettings(context: Context): DTOConfiguration {
        return withContext(Dispatchers.IO) {
            val file = File(context.filesDir, devConfig)
            if (file.exists()) {
                DTOConfigurationJsonParser(AnalyticsSenderImpl()).parse(JSONObject(file.readText()))
            } else {
                DTOConfigurationImpl()
            }
        }
    }

    suspend fun saveDeveloperSettings(context: Context, config: String) {
        withContext(Dispatchers.IO) {
            File(context.filesDir, devConfig).writeText(config)
        }
    }

    class AnalyticsSenderImpl : AnalyticsSender {
        override fun sendParsingConfigError(fieldName: String?, reason: String?, actionTaken: String?) {
            Log.d("Parsing", "$fieldName validation error $reason $actionTaken")
        }
    }
}