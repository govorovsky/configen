package ru.mail.condef.renderer

import ru.mail.condef.dsl.*

class RequiredFieldExceptionRenderer(val packageName: String) : Renderer {

    override fun render(writer: TextWriter) {
        with(writer) {
            append("/*").nextLine()
            append(" * Automatically generated file. DO NOT MODIFY").nextLine()
            append("*/").nextLine().nextLine()

            append("package $packageName;").nextLine(2)
            append("public class RequiredFieldException extends Exception {")
            with(withIncreasedIndentation()) {
                append("public RequiredFieldException(String message) {")
                withIncreasedIndentation().append("super(message);")
                append("}")
            }
            append("}")
        }
    }
}