package ru.mail.condef.renderer

import ru.mail.condef.dsl.*

class ParserRenderer(definition: Definition,
                     packageName: String,
                     private val className: String,
                     private val implementationName: String)
    : JavaCodeRenderer(definition, packageName) {

    override fun listDependencies(): List<String> =
            super.listDependencies() + listOf("JSONArray", "JSONException", "JSONObject").map { "org.json.$it" } + listNestedDependencies()


    private fun listNestedDependencies(): Collection<String> {
        val rootDependency = "$packageName.$implementationName"
        val deps = mutableSetOf(rootDependency)
        traverseDependencies(deps, definition, rootDependency)
        return deps.map { "static $it.*" }
    }

    private fun traverseDependencies(traversed: MutableSet<String>, definition: Definition, currentDependency: String) {
        definition.fields
                .filter { it.type is StrictObjectType
                        || (it.type is ArrayType<*> && (it.type.subtype is StrictObjectType || it.type.subtype is MultiObjectType)) }
                .map {
                    val dependencyClass = "$currentDependency.${it.name.asClassName()}Impl"
                    traversed.add(dependencyClass)
                    when (it.type) {
                        is StrictObjectType -> {
                            traverseDependencies(traversed, it.type.definition, dependencyClass)
                        }
                        is ArrayType<*> -> {
                            when (it.type.subtype) {
                                is StrictObjectType -> traverseDependencies(traversed, it.type.subtype.definition, dependencyClass)
                                is MultiObjectType ->  {
                                    traversed.remove(dependencyClass) // we don't need base interface as dependency
                                    it.type.subtype.types.forEach { entry ->
                                        traversed.add("$currentDependency.${it.name.asClassName()}${entry.key.toCamelCase()}Impl")
                                        traverseDependencies(traversed, entry.value, "$currentDependency.${it.name.asClassName()}${entry.key.toCamelCase()}Impl")
                                    }
                                }
                            }
                        }
                        else -> throw IllegalStateException()
                    }
                }
    }

    override fun render(writer: TextWriter) {
        super.render(writer)
        with(writer) {
            nextLine()
            append("public class $className {")
            with(withIncreasedIndentation()) {
                append("private AnalyticsSender mAnalyticsSender;")
                nextLine()
                append("public $className(AnalyticsSender analyticsSender) {")
                withIncreasedIndentation().append("mAnalyticsSender = analyticsSender;")
                append("}")
            }
            withIncreasedIndentation().join(parseMethods()) {
                nextLine(2)
            }
            append("}")
        }
    }

    private fun parseMethods(): Collection<Renderer> =
            listOf(ParseStrictObjectMethod(definition, "parse", implementationName, Privacy.PUBLIC, implementationName))
                    .flatMap { listOf(it) + it.listSubMethods() + it.listSubMethodsFromCompositeTypes() }


    abstract class ParseMethod(
            val name: String,
            val returnType: String,
            val privacy: Privacy) : Renderer {

        abstract fun collectWrittenFields(): List<Field<*>>

        override fun render(writer: TextWriter) {
            with(writer) {
                appendMethodHeader(writer)
                append("{")
                appendCode(withIncreasedIndentation())
                append("}")
            }
        }

        abstract fun appendMethodHeader(writer: TextWriter)

        abstract fun appendCode(writer: TextWriter)

        protected fun inferValueSource(field: Field<*>, type: Type<*>, key: String, sourceName: String): String {
            return when (type) {
                is StringType -> "$sourceName.getString($key)"
                is IntegerType -> "$sourceName.getInt($key)"
                is LongType -> "$sourceName.getLong($key)"
                is BoolType -> "$sourceName.getBoolean($key)"
                is StrictObjectType -> "${inferParseFuncName(field, type)}($sourceName.getJSONObject($key))"
                is ArrayType<*> -> "${inferParseFuncName(field, type)}($sourceName.getJSONArray($key))"
                is FreeObjectType -> "${inferParseFuncName(field, type)}($sourceName.getJSONObject($key))"
                is MultiObjectType -> "${inferParseFuncName(field, type)}($sourceName.getJSONObject($key))"
            }
        }

        private fun inferParseFuncName(field: Field<*>, type: Type<*> = field.type): String {
            return when (type) {
                is ArrayType<*> -> "parse${field.name.asClassName()}Array"
                is FreeObjectType -> "parse${field.name.asClassName()}Object"
                is StrictObjectType -> "parse${field.name.asClassName()}"
                is MultiObjectType -> "parse${field.name.asClassName()}"
                else -> unsupportedType("Unsupported type $type")
            }
        }

        protected fun isNeedUseCatchParsingBlock(type: Type<*>): Boolean {
            return when (type) {
                is StringType, is IntegerType, is BoolType, is LongType -> true
                is StrictObjectType, is ArrayType<*>, is FreeObjectType, is MultiObjectType -> false
            }
        }


        fun listSubMethods(): List<Renderer> {
            return collectWrittenFields()
                    .filter {
                        it.type is StrictObjectType
                                || it.type is ArrayType<*>
                                || it.type is FreeObjectType
                                || it.type is MultiObjectType
                    }
                    .map {
                        when (it.type) {
                            is StrictObjectType ->
                                parseStrictObject(it, it.type)

                            is ArrayType<*> ->
                                parseArray(it, it.type.subtype)

                            is FreeObjectType ->
                                parseFreeObject(it, it.type)

                            is MultiObjectType ->
                                parseMultiObject(it, it.type)

                            else -> unsupportedType()
                        }
                    }
                    .flatMap { listOf(it) + it.listSubMethods() + it.listSubMethodsFromCompositeTypes() }
        }

        fun listSubMethodsFromCompositeTypes(): List<Renderer> {
            return collectWrittenFields()
                    .filter {
                        it.type is CompositeType && (
                                it.type.subtype is StrictObjectType
                                        || it.type.subtype is CompositeType
                                        || it.type.subtype is MultiObjectType)
                    }
                    .map {
                        when ((it.type as CompositeType).subtype) {
                            is StrictObjectType ->
                                parseStrictObject(it, it.type.subtype!!)

                            is FreeObjectType ->
                                parseFreeObject(it, it.type.subtype as FreeObjectType)

                            is ArrayType<*> ->
                                parseArray(Field(it.name, it.type.subtype!!), (it.type.subtype!! as ArrayType<*>).subtype)

                            is MultiObjectType ->
                                parseMultiObject(it, (it.type as CompositeType).subtype as MultiObjectType)

                            else -> unsupportedType("Only Strict, FreeObject and MultiObject are supported as composite subtypes $it")
                        }
                    }
        }

        private fun parseMultiObject(field: Field<*>, type: MultiObjectType): ParseMethod =
                ParseMultiObjectMethod(
                        field,
                        type.types,
                        inferParseFuncName(field, type),
                        inferFieldTypeName(field.name, type),
                        Privacy.PRIVATE)

        private fun parseStrictObject(field: Field<*>, type: Type<*>): ParseMethod =
                ParseStrictObjectMethod(
                        getDefinition(field.type),
                        inferParseFuncName(field, type),
                        inferFieldTypeName(field.name, type),
                        Privacy.PRIVATE)

        private fun parseFreeObject(field: Field<*>, type: FreeObjectType): ParseMethod =
                ParseFreeObjectMethod(
                        type.subtype!!, field,
                        getDefinition(field.type),
                        inferParseFuncName(field, type),
                        inferFieldTypeName(field.name, type),
                        Privacy.PRIVATE)

        private fun parseArray(field: Field<*>, type: Type<*>): ParseMethod =
                ParseArrayMethod(field,
                        getDefinition(field.type),
                        inferParseFuncName(field),
                        inferFieldTypeName(field.name, type),
                        Privacy.PRIVATE)

        private fun unsupportedType(msg: String = "Unsupported type"): Nothing = throw IllegalArgumentException(msg)

        private fun getDefinition(type: Type<*>): Definition = when (type) {
            is StrictObjectType -> type.definition
            is FreeObjectType -> getDefinition(type.subtype!!)
            is ArrayType<*> -> getDefinition(type.subtype)
            else -> Definition(emptyList())
        }
    }

    class ParseMultiObjectMethod(
            private val field: Field<*>,
            private val types: Map<String, Definition>,
            methodName: String,
            fieldTypeName: String,
            privacy: Privacy) : ParseMethod(methodName, fieldTypeName, privacy) {

        override fun collectWrittenFields(): List<Field<*>> {
            return types.values.flatMap { it.fields }
        }

        override fun appendMethodHeader(writer: TextWriter) {
            writer.append("${privacy.syntax} $returnType $name(JSONObject json) throws JSONException, RequiredFieldException")
        }

        override fun appendCode(writer: TextWriter) {
            val typeKey = "\"type\""
            with(writer) {
                append("String key = json.getString($typeKey);").nextLine()
                types.forEach { key, _ ->
                    append("if (\"$key\".equals(key)) {")
                    with(withIncreasedIndentation()) {
                        append("return ${toSubMethodName(key)}(json);")
                    }
                    append("}").nextLine()
                }
                appendParsingFieldError(this)
            }
        }

        private fun appendParsingFieldError(writer: TextWriter) {
            writer.append("mAnalyticsSender.sendParsingConfigError(\"${field.name.rawText}\", \"bad_value\", \"configuration_not_accepted\");")
            writer.nextLine()
            writer.append("throw new RequiredFieldException(\"${field.name.rawText}\");")
        }

        private fun toSubMethodName(key: String) = name + key.toCamelCase()

        override fun render(writer: TextWriter) {
            super.render(writer)
            with(writer) {
                nextLine(2)
                types.forEach { key, definition ->
                    val parseStrictObjectMethod = ParseStrictObjectMethod(
                            definition,
                            toSubMethodName(key),
                            field.name.asClassName() + key.capitalize().toCamelCase(),
                            Privacy.PRIVATE
                    )
                    parseStrictObjectMethod.render(writer)
                    writer.nextLine(2)

                    parseStrictObjectMethod.listSubMethods().forEach {
                        it.render(writer)
                        writer.nextLine(2)
                    }

                    parseStrictObjectMethod.listSubMethodsFromCompositeTypes().forEach {
                        it.render(writer)
                        writer.nextLine(2)
                    }
                }
            }
        }
    }

    class ParseArrayMethod(private val field: Field<*>,
                           private val definition: Definition,
                           name: String,
                           returnType: String,
                           privacy: Privacy) : ParseMethod(name, returnType, privacy) {

        override fun collectWrittenFields(): List<Field<*>> = definition.fields

        override fun appendMethodHeader(writer: TextWriter) {
            writer.append("${privacy.syntax} List<$returnType> $name(JSONArray array) throws JSONException, RequiredFieldException")
        }

        override fun appendCode(writer: TextWriter) {
            val validator = getValidator(field, (field.type as ArrayType<*>).subtype)
            with(writer) {
                validator.appendPreValidate(this)
                append("List<$returnType> list = new ArrayList<>();").nextLine()
                var writerForCatchBlock = this
                if (isNeedUseCatchParsingBlock(field.type.subtype)) {
                    append("try {")
                    writerForCatchBlock = withIncreasedIndentation()
                }
                with(writerForCatchBlock) {
                    append("for (int i = 0; i < array.length(); i++) {")
                    with(withIncreasedIndentation()) {
                        appendParsingFieldBlock(field.type, validator)
                    }
                    append("}")
                }
                if (isNeedUseCatchParsingBlock(field.type.subtype)) {
                    appendCatchBlock(this, this@ParseArrayMethod)
                }
                nextLine()
                append("return list;")
            }
        }

        private fun TextWriter.appendParsingFieldBlock(type: ArrayType<*>, validator: ValidatorRenderer<*>) {
            append("$returnType value = ${inferValueSource(field, type.subtype, "i", "array")};").nextLine()
            validator.doIfValidate(this) {
                append("list.add(value);")
            } otherwise {
                append("mAnalyticsSender.sendParsingConfigError(\"${field.name.rawText}\", \"bad_value\", \"configuration_not_accepted\");")
                if (field.absenceHandler is RequiredHandler) {
                    nextLine()
                    append("throw new RequiredFieldException(\"${field.name.rawText}\");")
                }
            }
        }

        private fun appendCatchBlock(textWriter: TextWriter, parseArrayMethod: ParseArrayMethod) {
            textWriter.append("} catch (JSONException e) {")
            with(textWriter.withIncreasedIndentation()) {
                if (parseArrayMethod.field.absenceHandler is RequiredHandler) {
                    append("mAnalyticsSender.sendParsingConfigError(\"${parseArrayMethod.field.name.rawText}\", \"bad_value\", \"configuration_not_accepted\");")
                    nextLine()
                    append("throw new RequiredFieldException(\"${parseArrayMethod.field.name.rawText}\");")
                } else {
                    append("mAnalyticsSender.sendParsingConfigError(\"${parseArrayMethod.field.name.rawText}\", \"bad_type\", \"default_substituted\");")
                }
            }
            textWriter.append("}")
        }
    }

    class ParseFreeObjectMethod(private val subtype: Type<*>,
                                private val field: Field<*>,
                                private val definition: Definition,
                                name: String,
                                returnType: String,
                                privacy: Privacy) : ParseMethod(name, returnType, privacy) {

        override fun collectWrittenFields(): List<Field<*>> = definition.fields

        override fun appendMethodHeader(writer: TextWriter) {
            writer.append("${privacy.syntax} $returnType $name(JSONObject json) throws JSONException, RequiredFieldException")
        }

        override fun appendCode(writer: TextWriter) {
            with(writer) {
                append("$returnType map = new HashMap<>();").nextLine()
                append("Iterator<String> iter = json.keys();").nextLine()
                append("while (iter.hasNext()) {")
                with(withIncreasedIndentation()) {
                    append("String key = iter.next();").nextLine()
                    append("map.put(key, ${inferValueSource(field, subtype, "key", "json")});")
                }
                append("}").nextLine()
                append("return map;")
            }
        }
    }


    class ParseStrictObjectMethod(private val definition: Definition,
                                  name: String,
                                  returnType: String,
                                  privacy: Privacy,
                                  private val implementationName: String = returnType + "Impl")
        : ParseMethod(name, returnType, privacy) {

        override fun collectWrittenFields(): List<Field<*>> = definition.fields

        override fun appendMethodHeader(writer: TextWriter) {
            writer.append("${privacy.syntax} $returnType $name(JSONObject json) throws JSONException, RequiredFieldException")
        }

        override fun appendCode(writer: TextWriter) {
            with(writer) {
                append("$implementationName obj = new $implementationName();").nextLine()
                definition.fields.forEach { field ->
                    val validator = getValidator(field, field.type)
                    append("if (json.has(\"${field.name.asJsonFieldName()}\")) {")
                    with(withIncreasedIndentation()) {
                        append("${inferFieldTypeName(field.name, field.type)} value;").nextLine()
                        var writerForCatchBlock = this
                        if (isNeedUseCatchParsingBlock(field.type)) {
                            append("try {")
                            writerForCatchBlock = withIncreasedIndentation()
                        }
                        with(writerForCatchBlock) {
                            appendFieldParsingBlock(validator, field)
                        }
                        if (isNeedUseCatchParsingBlock(field.type)) {
                            appendCatchBlock(field)
                        }
                    }
                    append("}")
                    if (field.absenceHandler is RequiredHandler) {
                        appendErrorHandlingBlock(field)
                    }
                    nextLine()
                }
                append("return obj;")
            }
        }

        private fun TextWriter.appendFieldParsingBlock(validator: ValidatorRenderer<*>, field: Field<*>) {
            validator.appendPreValidate(this)
            append("value = ${inferValueSource(field, field.type, "\"${field.name.asJsonFieldName()}\"", "json")};").nextLine()
            validator.doIfValidate(this) {
                append("obj.${field.name.asSetterName()}(value);")
            }
            if (validator is SpecialValuesValidatorRenderer) {
                append(" else {")
                append("mAnalyticsSender.sendParsingConfigError(\"${field.name.rawText}\", \"bad_value\", \"configuration_not_accepted\");")
                if (field.absenceHandler is RequiredHandler) {
                    withIncreasedIndentation().append("throw new RequiredFieldException(\"${field.name.rawText}\");")
                }
                append("}")
            }
        }

        private fun TextWriter.appendErrorHandlingBlock(field: Field<*>) {
            append(" else {")
            with(withIncreasedIndentation()) {
                append("mAnalyticsSender.sendParsingConfigError(\"${field.name.rawText}\", \"bad_value\", \"configuration_not_accepted\");")
                nextLine()
                append("throw new RequiredFieldException(\"${field.name.rawText}\");")
            }
            append("}")
        }

        private fun TextWriter.appendCatchBlock(field: Field<*>) {
            append("} catch (JSONException e) {")
            with(withIncreasedIndentation()) {
                if (field.absenceHandler is RequiredHandler) {
                    append("mAnalyticsSender.sendParsingConfigError(\"${field.name.rawText}\", \"bad_value\", \"configuration_not_accepted\");")
                    nextLine()
                    append("throw new RequiredFieldException(\"${field.name.rawText}\");")
                } else {
                    append("mAnalyticsSender.sendParsingConfigError(\"${field.name.rawText}\", \"bad_type\", \"default_substituted\");")
                }
            }
            append("}")
        }
    }

    enum class Privacy(val syntax: String) {
        PUBLIC("public"), PRIVATE("private")
    }
}