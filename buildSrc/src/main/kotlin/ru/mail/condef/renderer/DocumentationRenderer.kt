package ru.mail.condef.renderer

import ru.mail.condef.dsl.*

class DocumentationRenderer(private val definition: Definition, private val version: String) : Renderer {

    override fun render(writer: TextWriter) {
        with(writer) {
            val docHtml = html {
                head {
                    style { +cssStyles() }
                    title { +"Android Mail Configuration Constants" }
                    script {
                        +getJs()
                    }
                    link(attrs("rel" to "stylesheet",
                            "href" to "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta.3/css/bootstrap.min.css",
                            "integrity" to "sha384-Zug+QiDoJOrZ5t4lssLdxGhVrurbmBWopoEl+M6BdEfwnCJZtKxi1KgxUyJq13dy",
                            "crossorigin" to "anonymous")) {
                    }
                }

                body {
                    h1(attrs("class" to "text-center")) {
                        +"Android Mail Configuration for $version"
                    }
                    table(attrs("class" to "table table-bordered table-hover")) {
                        thead {
                            tr {
                                th(attrs("style" to "width: 20%")) {
                                    +"JSON Field"
                                }

                                th(attrs("style" to "width: 35%")) {
                                    +"Description"
                                }

                                th(attrs("style" to "width: 15%")) {
                                    +"Value type"
                                }

                                th(attrs("style" to "width: 10%")) {
                                    +"Default value"
                                }

                                th(attrs("style" to "width: 20%")) {
                                    +"Example"
                                }
                            }
                        }
                        traverse(definition, this, emptyList())
                    }
                }
            }
            append(docHtml.toString())
        }
    }

    private fun getJs(): String {
        val inputStream = javaClass.classLoader.getResourceAsStream("doc.js")
        return inputStream.bufferedReader().use {
            it.readText()
        }
    }

    private fun cssStyles(): String {
        return ""
    }


    private fun traverse(definition: Definition, table: Table, fieldPath: List<Field<*>>) {
        definition.fields
                .forEach {
                    val currentPath = fieldPath + it
                    renderTableRow(currentPath, table)
                    if (it.type is StrictObjectType) {
                        traverse(it.type.definition, table, currentPath)
                    } else if (it.type is CompositeType) {
                        when (it.type.subtype) {
                            is StrictObjectType ->
                                traverse((it.type.subtype as StrictObjectType).definition, table, currentPath)
                            is MultiObjectType -> (it.type.subtype as MultiObjectType).types.forEach { type ->
                                val multiObjectPath = currentPath + Field(Field.Name(type.key), it.type.subtype as MultiObjectType, SubstituteWithDefaultHandler(EmptyObject))
                                renderTableRow(multiObjectPath, table)
                                traverse(type.value, table, multiObjectPath)
                            }
                        }
                    }
                }
    }

    private fun renderTableRow(path: List<Field<*>>, table: Table) {
        val fullNodeName = getFieldName(path)
        table.tr(
                attrs(
                        "data-node" to fullNodeName,
                        "data-node-parent" to fullNodeName.substringBeforeLast('.', "_root")))
        {
            td {
                +path.last().name.asJsonFieldName()
            }
            td {
                +getDescription(path.last())
            }
            td {
                div {
                    +getDefaultValueType(path.last().type)
                }
                path.last().validator?.let {
                    div(attrs("style" to "padding-top: 10px")) {
                        +getConstraints(it)
                    }
                }
            }
            td {
                +getDefaultValue(path.last())
            }
            td {
                a(attrs("href" to "#", "class" to "toggablePre")) {
                    +"Example"
                }
                pre(attrs("style" to "display: none; margin-top: 10px; margin-bottom:0px;")) {
                    +getJsonExample(path)
                }
            }
        }
    }

    private fun getDefaultValueType(type: Type<*>): String {
        return when (type) {
            is StrictObjectType, is FreeObjectType -> "Object"
            is ArrayType<*> -> "Array of ${getDefaultValueType(type.subtype)}"
            else -> type.javaClass.simpleName.split("Type")[0]
        }
    }

    private fun getConstraints(validator: Validator): String {
        return when (validator) {
            is SpecialValuesValidator<*> -> "Allowed values: ${renderKotlinValue(validator.values)}"
            is RegexValidator -> "Allowed regex pattern: ${validator.pattern.toRegex()}"
            is RangeValidator -> "Allowed range [${validator.fromInclusive}...${validator.toInclusive}]"
        }
    }

    private fun getFieldName(path: List<Field<*>>) =
            path.joinToString(".") { it.name.asJsonFieldName() }

    private fun getDescription(field: Field<*>) =
            field.description

    private fun getDefaultValue(field: Field<*>) = when (field.absenceHandler) {
        is RequiredHandler -> "Required. Obtained from server"
        is SubstituteWithDefaultHandler<*> -> renderKotlinValue(field.absenceHandler.defaultValue)
        null -> throw IllegalStateException()
    }

    private fun renderKotlinValue(value: Any?): String = when (value) {
        is String -> "\"$value\""
        is Number, is Boolean -> value.toString()
        is EmptyObject -> "-"
        is Collection<*> -> value.joinToString(prefix = "[", postfix = "]") {
            when (it) {
                is String -> "\"$it\""
                else -> it.toString()
            }
        }
        is Map<*, *> -> renderMapValue(value)
        null -> "null"
        else -> throw IllegalArgumentException("Unknown default type ${value::class.java}")
    }

    private fun renderMapValue(value: Map<*, *>): String {
        return value.map { (mapKey, mapValue) -> "\"${renderKotlinValue(mapKey)}\": ${renderKotlinValue(mapValue)}" }
                .joinToString(prefix = "{", postfix = "}")
    }

    private fun getJsonExample(path: List<Field<*>>): String {
        var json = "{\n%s\n}"
        path.forEachIndexed { i, field ->
            json = json.format(when (field.type) {
                is StrictObjectType -> "${indent(i)}\"${field.name.asJsonFieldName()}\" : {\n%s\n${indent(i)}}"

                is MultiObjectType -> if (field == path.last()) getExampleJsonValue(field, field.type, i) else "%s"

                is FreeObjectType -> "${indent(i)}\"${field.name.asJsonFieldName()}\" : " + when (field.type.subtype) {
                    is StrictObjectType -> "{\n${indent(i + 1)}\"key\" : {\n${indent(i + 1)}%s\n${indent(i + 1)}}\n${indent(i)}}"
                    else -> "{\n${indent(i + 1)}\"key\" : ${getExampleJsonValue(field, field.type.subtype!!)}\n${indent(i)}}"
                }

                is ArrayType<*> -> "${indent(i)}\"${field.name.asJsonFieldName()}\" : " + when (field.type.subtype) {
                    is StrictObjectType, is MultiObjectType -> "[{\n%s\n${indent(i)}}]"
                    else -> "[\n${indent(i + 1)}${getExampleJsonValue(field, field.type.subtype)}\n${indent(i)}]"
                }

                else -> "${indent(i)}\"${field.name.asJsonFieldName()}\": ${getExampleJsonValue(field, field.type)}"
            })
        }
        return json.format("${indent(path.lastIndex + 1)}&lt;object&gt;")
    }

    private fun getExampleJsonValue(field: Field<*>, type: Type<*>, indent: Int = 0) = when (type) {
        is StringType, is IntegerType, is LongType -> getValueFromValidatorOrDefault(type, field.validator)
        is BoolType -> "false"
        is ArrayType<*> -> "[]"
        is StrictObjectType, is FreeObjectType -> "{}"
        is MultiObjectType -> getFullMultiObjectValue(field, type, indent)
    }

    private fun getFullMultiObjectValue(field: Field<*>, type: MultiObjectType, i: Int): String {
        val fieldsRepr = mutableListOf<String>()
        fieldsRepr.add("${indent(i)}\"type\" : \"${field.name.rawText}\"")
        type.types[field.name.rawText]?.fields?.forEach {
            fieldsRepr.add("${indent(i)}\"${it.name.rawText}\" : ${getExampleJsonValue(it, it.type)}")
        }
        return fieldsRepr.joinToString(separator = ",\n")
    }

    private fun getValueFromValidatorOrDefault(type: Type<*>, validator: Validator?) = when (validator) {
        is SpecialValuesValidator<*> -> "\"${validator.values[0]}\""
        is RangeValidator -> validator.fromInclusive.toString()
        is RegexValidator, null -> when (type) {
            is StringType -> "\"abc\""
            is IntegerType -> "42"
            is LongType -> "42"
            else -> throw IllegalStateException("wrongs pair of validator and type")
        }
    }

    private fun indent(num: Int) = " ".repeat(num + 1)
}