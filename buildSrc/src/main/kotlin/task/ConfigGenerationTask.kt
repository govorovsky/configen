package task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import ru.mail.condef.main.ConfigGenerator
import java.io.File

open class ConfigGenerationTask : DefaultTask() {

    /*
    * Package name for the generated classes.
    */
    lateinit var packageName: String

    /*
    * Dest dir for classes
    */
    lateinit var destDir: String

    /*
    * JSON parser class name.
    */
    lateinit var jsonParserClassName: String

    /*
    * Configuration class name.
    */
    lateinit var configurationClassName: String

    /*
    * Configuration class name.
    */
    lateinit var configurationInterfaceName: String

    /*
    * Configuration settings definition class name.
    */
    lateinit var settingsDefinitionClassName: String

    /*
    * Configuration settings mapper class name.
    */
    lateinit var settingsMapperClassName: String

    /*
    * Configuration definition file.
    */
    @InputFile
    lateinit var configurationDefinition: File

    /*
    * Documentation file.
    */
    @OutputFile
    lateinit var documentationFile: File

    /*
    * Build version.
    */
    lateinit var buildVersion: String


    @TaskAction
    fun executeTask() {
        println("========== Generating config =========")
        println(configurationDefinition.length())
        println("Destination: ${getOutputDirName()}, " +
                "Parser: $jsonParserClassName, " +
                "Interface: $configurationInterfaceName, " +
                "Impl: $configurationClassName")
        ConfigGenerator(
            packageName,
            configurationInterfaceName,
            configurationClassName,
            jsonParserClassName,
            settingsDefinitionClassName,
            settingsMapperClassName,
            getOutputDirName().absolutePath,
            documentationFile,
            buildVersion)
            .generate()
    }


    @OutputDirectory
    private fun getOutputDirName(): File {
        return File(destDir + "/" + packageName.replace('.', '/'))
    }
}
