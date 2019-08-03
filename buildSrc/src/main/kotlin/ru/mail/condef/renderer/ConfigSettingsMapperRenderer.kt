package ru.mail.condef.renderer

import ru.mail.condef.dsl.*

class ConfigSettingsMapperRenderer(
    rootDefinition: Definition,
    packageName: String
) : JavaCodeRenderer(rootDefinition, packageName) {

    override fun listDependencies(): List<String> =
        super.listDependencies() + listOf("Field", "StrictObjectField", "FreeObjectField").map { "ru.mail.flexsettings.field.$it" }

    override fun render(writer: TextWriter) {
        super.render(writer)

        writer.codeBlock("public class ConfigSettingsMapper") {
            codeBlock("public static void map(StrictObjectField configurationField, DTOConfiguration configuration, DTOConfiguration configurationState)") {
                initDefinition("DTOConfiguration", "configuration", definition)
            }
        }
    }

    private fun TextWriter.initPrimitive(parentObjectName : String, type : String, field : Field<*>) {
        val fieldGetter = "${parentObjectName}Field.getField(\"${field.key()}\")"
        appendLine("$fieldGetter.as${type.capitalize()}().setValue($parentObjectName.${field.getterName()}());")
        appendLine("$fieldGetter.setChanged(${parentObjectName}State.${field.getterName()}Set());")
    }

    private fun TextWriter.initStrictObject(parentClassName: String, parentObjectName: String, field: Field<*>) {
        val className = "$parentClassName.${field.name.asClassName()}"
        val objectName = field.name.asFieldName()
        val fieldName = objectName + "Field"

        nextLine()
        appendLine("StrictObjectField $fieldName = ${parentObjectName}Field.getField(\"${field.key()}\").asStrictObject();")
        appendLine("$className $objectName = $parentObjectName.${field.getterName()}();")
        appendLine("$className ${objectName}State = ${parentObjectName}State.${field.getterName()}();")
        appendLine("$fieldName.setChanged(${parentObjectName}State.${field.getterName()}Set());")

        initDefinition(className, objectName, (field.type as StrictObjectType).definition)
    }

    private fun TextWriter.initFreeObject(parentClassName: String, parentObjectName: String, field: Field<*>) {
        val className = "$parentClassName.${field.name.asClassName()}"
        val objectName = field.name.asFieldName()
        val fieldName = objectName + "Field"
        val type = field.type as FreeObjectType
        val mapClass = when (type.subtype) {
            is StringType -> "String"
            is StrictObjectType -> className
            else -> "Void"
        }

        appendLine("FreeObjectField $fieldName = ${parentObjectName}Field.getField(\"${field.key()}\").asFreeObject();")
        appendLine("$fieldName.setChanged(${parentObjectName}State.${field.getterName()}Set());")
        appendLine("Map<String, $mapClass> $objectName = $parentObjectName.${field.getterName()}();")
        appendLine("for (Map.Entry<String, $mapClass> entry : $objectName.entrySet()) {")
        with(withIncreasedIndentation()) {
            if (type.subtype is StringType) {
                append("$fieldName.addField(entry.getKey()).asString().setValue(entry.getValue());")
                appendLine("$fieldName.getField(entry.getKey()).setChanged($fieldName.isChanged());")
            } else {
                append("// TODO add support of ${type.subtype}")
            }
        }
        append("}")
    }

    private fun TextWriter.initDefinition(parentClassName: String, parentObjectName: String, definition: Definition) {
        definition.fields.forEach {
            when (it.type) {
                is StringType -> initPrimitive(parentObjectName, "string", it)
                is BoolType -> initPrimitive(parentObjectName, "boolean", it)
                is IntegerType -> initPrimitive(parentObjectName, "integer", it)
                is LongType -> initPrimitive(parentObjectName, "long", it)
                is StrictObjectType -> initStrictObject(parentClassName, parentObjectName, it)
                is FreeObjectType -> initFreeObject(parentClassName, parentObjectName, it)
                else -> appendLine("// ${it.name.asFieldName()} - ${it.type}")
            }
        }
    }

    private fun TextWriter.codeBlock(header: String, block: TextWriter.() -> Unit) {
        appendLine("$header {")
        with(withIncreasedIndentation()) {
            block()
        }
        append("}")
    }

    private fun Field<*>.key() = name.rawText
    private fun Field<*>.getterName() = name.asGetterName(type)
}
