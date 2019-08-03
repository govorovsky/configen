package ru.mail.condef.renderer

import ru.mail.condef.dsl.*
import java.util.*

fun String.toCamelCase() =
        split('_', '-', ' ').joinToString("") { it.capitalize() }

fun Field.Name.asClassName() = if (customName.isEmpty()) rawText.toCamelCase() else customName.toCamelCase()

fun Field.Name.asFieldName() = asClassName().decapitalize()

fun Field.Name.asGetterName(type: Type<*>) = when (type) {
    is BoolType -> "is"
    else -> "get"
} + asClassName()

fun Field.Name.asSetterName() = "set" + asClassName()

fun Field.Name.asJsonFieldName() = rawText

fun inferFieldTypeName(name: Field.Name, type: Type<*>): String = when (type) {
    is StringType -> "String"
    is IntegerType -> "Integer"
    is LongType -> "Long"
    is BoolType -> "Boolean"
    is FreeObjectType -> "Map<String, ${inferFieldTypeName(name, type.subtype!!)}>"
    is StrictObjectType -> name.asClassName()
    is ArrayType<*> -> "List<${inferFieldTypeName(name, type.subtype)}>"
    is MultiObjectType -> "${name.asClassName()}Base"
}

fun asPredefinedCollection(values: List<*>): String {
    return "Arrays.asList(${values.joinToString {
        when (it) {
            is String -> "\"${it.toLowerCase(Locale.ENGLISH)}\""
            is Int -> "$it"
            is Long -> "${it}L"
            else -> throw IllegalStateException("unsupported default type")
        }
    }})"
}

abstract class JavaCodeRenderer(val definition: Definition, val packageName: String) : Renderer {
    override fun render(writer: TextWriter) {
        with(writer) {
            append("/*").nextLine()
            append(" * Automatically generated file. DO NOT MODIFY").nextLine()
            append("*/").nextLine().nextLine()

            append("package $packageName;").nextLine(2)
            performChecks(collectAllFieldsRecursively(definition))
            listDependencies().forEach { import ->
                append("import $import;")
                nextLine()
            }
        }
    }

    protected open fun performChecks(fields: List<Field<*>>) {
        checkUniqueClassNames(fields)
        checkReservedWordsCollisions(fields)
        checkUniqueParseMethodNames(fields)
        checkUnsupportedRecursionTypes<ArrayType<*>>(fields)
        checkUnsupportedRecursionTypes<FreeObjectType>(fields)
    }

    private fun checkReservedWordsCollisions(fields: List<Field<*>>) {
        fields
                .firstOrNull { reservedJavaWords.contains(it.name.asFieldName()) }
                ?.let {
                    throw IllegalStateException("Generated field `${it.name.asFieldName()}` clashes with reserved Java keyword, specify different name using `withFieldName`")
                }
    }

    private inline fun <reified T : CompositeType> checkUnsupportedRecursionTypes(fields: List<Field<*>>) {
        fields
                .firstOrNull { it.type is T && it.type.subtype is T }
                ?.let {
                    throw IllegalStateException("${T::class.java} of ${T::class.java} does not supported")
                }
    }


    private fun checkUniqueClassNames(fields: List<Field<*>>) {
        fields
                .filter { it.type is StrictObjectType || (it.type is ArrayType<*> && it.type.subtype is StrictObjectType) }
                .groupBy { it.name.asClassName() }
                .asIterable()
                .firstNonSingle()
                ?.let {
                    throw IllegalStateException("Duplicate generated ClassName in Field `${it.key}`, specify custom class name using `withClassName`")
                }
    }

    private fun checkUniqueParseMethodNames(fields: List<Field<*>>) {
        fields
                .filter { it.type is StrictObjectType || (it.type is ArrayType<*> || it.type is FreeObjectType) }
                .groupBy { it.name.asClassName() + it.type.javaClass.simpleName }
                .asIterable()
                .firstNonSingle()
                ?.let {
                    throw IllegalStateException("Duplicate generated Method name in Field `${it.key}`, specify custom class name using `withClassName`")
                }
    }

    private fun <T : Map.Entry<*, Collection<*>>> Iterable<T>.firstNonSingle(): T? {
        return this.firstOrNull { it.value.size > 1 }
    }

    private fun collectAllFieldsRecursively(definition: Definition): List<Field<*>> {
        return definition.fields
                .flatMap {
                    val type = it.type
                    listOf(it) + if (type is StrictObjectType) {
                        collectAllFieldsRecursively(type.definition)
                    } else if (type is CompositeType && type.subtype is StrictObjectType) {
                        collectAllFieldsRecursively((type.subtype as StrictObjectType).definition)
                    } else if (type is MultiObjectType) {
                        type.types.values.flatMap { collectAllFieldsRecursively(it) }
                    } else {
                        emptyList()
                    }
                }
    }

    protected open fun listDependencies(): List<String>
            = listOf("List", "Map", "Set", "Collection", "Collections", "ArrayList", "HashMap", "HashSet", "Iterator", "Arrays", "regex.*", "Locale").map { "java.util.$it" }

    companion object {
        val reservedJavaWords = setOf("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "default",
                "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements",
                "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected",
                "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
                "transient", "true", "try", "void", "volatile", "while", "continue")
    }
}