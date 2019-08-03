package ru.mail.condef.main

import dsl.rootDefinition
import ru.mail.condef.renderer.*
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

class ConfigGenerator(
    val packageName: String = "ru.mail",
    val interfaceName: String = "Configuration",
    val implClassName: String = "ConfigurationContentImpl",
    val parserClassName: String = "ConfigurationJsonParser",
    val settingsDefinitionClassName: String = "ConfigSettingsDefinition",
    val settingsMapperClassName: String = "ConfigSettingsMapper",
    val destDir: String = "generated",
    val docFile: File = File("generatedDoc/configuration.html"),
    val versionCode: String = "1900_alpha"
) {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            ConfigGenerator("ru.mail").generate()
        }
    }

    fun generate() {
        ConfigCodeRenderer(
            rootDefinition,
            InterfaceGenerationStrategy(interfaceName),
            packageName
        ).renderToFile("$destDir/$interfaceName.java")

        ConfigCodeRenderer(
            rootDefinition,
            ImplementationGenerationStrategy(interfaceName, implClassName, static = false),
            packageName
        ).renderToFile("$destDir/$implClassName.java")

        ConfigSettingsDefinitionRenderer(
            rootDefinition,
            packageName
        ).renderToFile("$destDir/$settingsDefinitionClassName.java")

        ConfigSettingsMapperRenderer(
            rootDefinition,
            packageName
        ).renderToFile("$destDir/$settingsMapperClassName.java")

        ParserRenderer(
            rootDefinition,
            packageName,
            parserClassName,
            implClassName
        ).renderToFile("$destDir/$parserClassName.java")

        AnalyticsSenderRenderer(
            packageName
        ).renderToFile("$destDir/AnalyticsSender.java")

        RequiredFieldExceptionRenderer(
            packageName
        ).renderToFile("$destDir/RequiredFieldException.java")

        DocumentationRenderer(rootDefinition, versionCode)
            .renderToFile(docFile.absolutePath)
    }

    private fun Renderer.renderToFile(fileName: String) {
        val writer = TextWriter()
        render(writer)

        val file = File(fileName)
        if (file.exists() && !file.delete()) {
            throw IOException("Cannot delete previous version of " + fileName)
        }
        val parent = File(file.parent)
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Cannot create dirs for " + file.parent)
        }
        val printWriter = file.printWriter(Charset.forName("UTF-8"))
        try {
            printWriter.print(writer.toString())
        } finally {
            printWriter.close()
        }
    }
}

