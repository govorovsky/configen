package ru.mail.condef.renderer

import ru.mail.condef.dsl.*

class ConfigSettingsDefinitionRenderer(
    rootDefinition: Definition,
    packageName: String
) : JavaCodeRenderer(rootDefinition, packageName) {

    override fun listDependencies(): List<String> =
        super.listDependencies() + listOf("ru.mail.flexsettings.field.Field")

    override fun render(writer: TextWriter) {
        super.render(writer)

        writer.codeBlock("public class ConfigSettingsDefinition") {
            codeBlock("public static Field create()") {
                append("return Field.strictObject(\"base\",")
                renderDefinition(this, definition)
                append(");")
            }
        }
    }

    private fun renderDefinition(writer: TextWriter, definition: Definition) {
        with(writer.withIncreasedIndentation()) {
            definition.fields.forEachIndexed { index, field ->
                if (index != 0) {
                    append(",").nextLine()
                }
                renderType(field.key(), field.type)
            }
        }
    }

    private fun TextWriter.renderType(key: String, type: Type<*>) {
        when (type) {
            is StringType -> renderStringField(key)
            is BoolType -> renderBooleanField(key)
            is IntegerType -> renderIntegerField(key)
            is LongType -> renderLongField(key)
            is StrictObjectType -> renderStrictObjectField(key, type)
            is FreeObjectType -> renderFreeObjectField(key, type)
            else -> renderUnsupported(key, type)
        }
    }

    private fun TextWriter.renderStringField(key: String) {
        renderPrimitiveField("string", key)
    }

    private fun TextWriter.renderBooleanField(key: String) {
        renderPrimitiveField("bool", key)
    }

    private fun TextWriter.renderIntegerField(key: String) {
        renderPrimitiveField("integer", key)
    }

    private fun TextWriter.renderLongField(key: String) {
        renderPrimitiveField("longField", key)
    }

    private fun TextWriter.renderPrimitiveField(type: String, key: String) {
        append("Field.$type(\"$key\")")
    }

    private fun TextWriter.renderStrictObjectField(key: String, type: StrictObjectType) {
        append("Field.strictObject(\"$key\",")
        renderDefinition(this, type.definition)
        append(")")
    }

    private fun TextWriter.renderFreeObjectField(key: String, type: FreeObjectType) {
        append("Field.freeObject(\"$key\",")
        with(withIncreasedIndentation()) {
            renderType("", type.subtype!!)
        }
        append(")")
    }

    private fun TextWriter.renderUnsupported(key: String, type: Type<*>) {
        append("Field.empty(\"$key | ${type.javaClass.simpleName} \")")
    }

    private fun TextWriter.codeBlock(header: String, block: TextWriter.() -> Unit) {
        appendLine("$header {")
        with(withIncreasedIndentation()) {
            block()
        }
        append("}")
    }

    private fun Field<*>.key() = name.rawText
}
