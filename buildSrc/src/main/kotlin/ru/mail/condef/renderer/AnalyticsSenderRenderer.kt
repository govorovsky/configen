package ru.mail.condef.renderer

import ru.mail.condef.dsl.*

class AnalyticsSenderRenderer(val packageName: String) : Renderer {

    override fun render(writer: TextWriter) {
        with(writer) {
            append("/*").nextLine()
            append(" * Automatically generated file. DO NOT MODIFY").nextLine()
            append("*/").nextLine().nextLine()

            append("package $packageName;").nextLine(2)
            append("public interface AnalyticsSender {")
            withIncreasedIndentation().append("public void sendParsingConfigError(String fieldName, String reason, String actionTaken);")
            append("}")
        }
    }
}