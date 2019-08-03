package ru.mail.condef.renderer

import ru.mail.condef.dsl.*

class ConfigCodeRenderer(
        rootDefinition: Definition,
        strategy: CodeGenerationStrategy,
        packageName: String) : JavaCodeRenderer(rootDefinition, packageName) {

    private val rootInterface = ContentsCodeRenderer(rootDefinition, strategy)

    override fun render(writer: TextWriter) {
        super.render(writer)
        writer.append(rootInterface)
    }

    open class ContentsCodeRenderer(
            definition: Definition,
            private val codeGeneration: CodeGenerationStrategy) : Renderer {

        internal val renderers = mutableListOf<Renderer>()

        init {
            renderers.addAll(codeGeneration.createRendersForFields(definition.fields))

            val definitionsFromReturnTypes = definition.fields
                    .filter { it.type is StrictObjectType }
                    .map { Pair((it.type as StrictObjectType).definition, it.name.asClassName()) }

            renderers.addAll(codeGeneration.createRendersForDefinitions(definitionsFromReturnTypes))

            val definitionsFromCompositeTypes = definition.fields
                    .filter { it.type is CompositeType && it.type.subtype is StrictObjectType }
                    .map { Pair(((it.type as CompositeType).subtype as StrictObjectType).definition, it.name.asClassName()) }

            renderers.addAll(codeGeneration.createRendersForDefinitions(definitionsFromCompositeTypes))

            var definitionsFromMultiObjectTypes = definition.fields
                    .filter { it.type is MultiObjectType }
                    .map { Pair((it.type as MultiObjectType).types, it.name.asClassName()) }

            definitionsFromMultiObjectTypes += definition.fields
                    .filter { it.type is CompositeType && it.type.subtype is MultiObjectType }
                    .map { Pair((((it.type as CompositeType).subtype as MultiObjectType).types), it.name.asClassName()) }

            renderers.addAll(codeGeneration.createRendersForMultiObjectDefinitions(definitionsFromMultiObjectTypes))
            renderers.add(codeGeneration.createRendererForMerge(definition.fields, codeGeneration.className))
        }

        override fun render(writer: TextWriter) {
            writer.nextLine()
            writer.append("public ${codeGeneration.entityHeader} {")
            writer.withIncreasedIndentation().join(renderers) { nextLine(2) }
            writer.append("}")
        }
    }
}

interface CodeGenerationStrategy {
    val entityHeader: String
    val className: String
    fun createRendersForFields(fields: List<Field<*>>): Collection<Renderer>
    fun createRendersForDefinitions(definitions: List<Pair<Definition, String>>): Collection<Renderer>
    fun createRendersForMultiObjectDefinitions(types: List<Pair<Map<String, Definition>, String>>): Collection<Renderer>
    fun createRendererForMerge(fields: List<Field<*>>, currentClassType: String) : Renderer
    fun redefineNames(fieldName: String, superType: String = ""): CodeGenerationStrategy
}

class InterfaceGenerationStrategy(private val name: String, private val superType: String = "") : CodeGenerationStrategy {

    override val entityHeader: String
        get() = "interface $name" + if (superType.isEmpty()) "" else " extends $superType"

    override val className: String
        get() = name

    override fun createRendersForFields(fields: List<Field<*>>): Collection<Renderer> {
        return fields.map { InterfaceGetterRenderer(it.name, it.type) }
    }

    override fun createRendersForDefinitions(definitions: List<Pair<Definition, String>>): Collection<Renderer> {
        return definitions.map { ConfigCodeRenderer.ContentsCodeRenderer(it.first, this.redefineNames(it.second)) }
    }

    override fun redefineNames(fieldName: String, superType: String): CodeGenerationStrategy = InterfaceGenerationStrategy(fieldName, superType)

    override fun createRendersForMultiObjectDefinitions(types: List<Pair<Map<String, Definition>, String>>)
            : Collection<Renderer> {


        return types.flatMap {
            val baseName = it.second.capitalize().toCamelCase()
            val mapping = it.first
            listOf(MultiObjectBaseInterface(baseName, mapping.keys.toList())) +
                    mapping.entries.map {
                        ConfigCodeRenderer.ContentsCodeRenderer(it.value, redefineNames(
                                baseName + it.key.capitalize().toCamelCase(), "${baseName}Base"))
                    }
        }
    }

    override fun createRendererForMerge(fields: List<Field<*>>, currentClassType: String) : Renderer {
        return MergeMethodRenderer(fields, currentClassType)
    }

    private data class InterfaceGetterRenderer(val name: Field.Name, val type: Type<*>) : Renderer {
        override fun render(writer: TextWriter) {
            writer.append("${inferFieldTypeName(name, type)} ${name.asGetterName(type)}();")
            writer.appendLine("boolean ${name.asGetterName(type)}Set();")
        }
    }

    private data class MultiObjectBaseInterface(val name: String, val subtypes: List<String>) : Renderer {
        override fun render(writer: TextWriter) {
            with(writer) {
                append("public interface ${name}Base {")
                withIncreasedIndentation().apply {
                    append("<T> T accept(Visitor<T> visitor);").nextLine(2)
                    append("interface Visitor<T> {")
                    withIncreasedIndentation().apply {
                        join(subtypes.map {
                            "T on${it.capitalize().toCamelCase()}(${name + it.capitalize().toCamelCase()} dto);".asRenderer()
                        }, { nextLine(2) })
                    }
                    append("}")
                }
                append("}")
            }
        }

        fun String.asRenderer(): Renderer {
            return object : Renderer {
                override fun render(writer: TextWriter) {
                    writer.append(this@asRenderer)
                }
            }
        }
    }

    private data class MergeMethodRenderer(
            val fields: List<Field<*>>,
            val name: String) : Renderer {

        override fun render(writer: TextWriter) {
            writer.append("public void merge($name from);")
        }
    }
}

class ImplementationGenerationStrategy(
        private val interfaceName: String,
        private val implName: String,
        private val static: Boolean = true) : CodeGenerationStrategy {

    override val entityHeader: String
        get() = "${if (static) "static " else ""}class $implName implements $interfaceName"

    override val className: String
        get() = interfaceName

    override fun createRendersForFields(fields: List<Field<*>>): Collection<Renderer> {
        return fields.map { FieldRenderer(it.name, it.type, it.absenceHandler!!) } +
                fields.map { GetterAndSetterRenderer(it.name, it.type) }
    }

    override fun createRendersForDefinitions(definitions: List<Pair<Definition, String>>): Collection<Renderer> {
        return definitions.map { ConfigCodeRenderer.ContentsCodeRenderer(it.first, this.redefineNames(it.second)) }
    }

    override fun redefineNames(fieldName: String, superType: String): CodeGenerationStrategy
            = ImplementationGenerationStrategy(fieldName, fieldName + "Impl")

    override fun createRendersForMultiObjectDefinitions(types: List<Pair<Map<String, Definition>, String>>): Collection<Renderer> {
        return types.flatMap {
            val baseName = it.second.capitalize().toCamelCase()
            val mapping = it.first
            mapping.entries.map {
                MultiObjectImplRenderer(it.value, redefineNames(
                        baseName + it.key.capitalize().toCamelCase()), it.key)
            }
        }
    }

    override fun createRendererForMerge(fields: List<Field<*>>, currentClassType: String) : Renderer {
        return MergeMethodRenderer(fields, currentClassType)
    }

    private class MultiObjectImplRenderer(rootDefinition: Definition,
                                          strategy: CodeGenerationStrategy,
                                          className: String)
        : ConfigCodeRenderer.ContentsCodeRenderer(rootDefinition, strategy) {

        init {
            renderers.add(VisitorMethodRenderer(className))
        }

        class VisitorMethodRenderer(private val className: String) : Renderer {
            override fun render(writer: TextWriter) {
                with(writer) {
                    append("@Override").nextLine()
                    append("public <T> T accept(Visitor<T> visitor) {")
                    with(withIncreasedIndentation()) {
                        append("return visitor.on${className.toCamelCase()}(this);")
                    }
                    append("}")
                }
            }
        }
    }


    private data class FieldRenderer(
            private val name: Field.Name,
            private val type: Type<*>,
            private val absenceHandler: AbsenceHandler) : Renderer {

        private var internalWriter = TextWriter()

        override fun render(writer: TextWriter) {
            internalWriter = writer.newWriter()
            val inferredDefault = inferDefault()
            val baseLine = "private ${inferFieldTypeName(name, type)} m${name.asClassName()}"
            if (inferredDefault != null) {
                writer.append("$baseLine = $inferredDefault;")
            } else {
                writer.append("$baseLine;")
            }
            writer.nextLine()
            writer.append("private boolean m${name.asClassName()}Set = false;")
        }

        private fun inferDefault(): String? = when (absenceHandler) {
            is SubstituteWithDefaultHandler<*> -> inferDefaultValue(type, absenceHandler.defaultValue)
            is RequiredHandler -> null
        }

        private fun inferDefaultValue(type: Type<*>, defaultValue: Any?): String = when (type) {
            is StringType -> if (defaultValue is String) "\"$defaultValue\"" else "null"
            is IntegerType -> "${defaultValue!! as Int}"
            is LongType -> "${defaultValue!! as Long}L"
            is BoolType -> "${defaultValue!! as Boolean}"
            is FreeObjectType -> inferMapDefaultValue(type, defaultValue)
            is StrictObjectType -> inferStrictObjectDefaultValue(defaultValue)
            is MultiObjectType -> inferMultiObjectDefaultValue(defaultValue)
            is ArrayType<*> -> inferArrayDefaultValues(defaultValue as List<*>)
        }

        private fun inferMultiObjectDefaultValue(defaultValue: Any?): String = when (defaultValue) {
            null -> "null"
            else -> throw IllegalStateException()
        }

        private fun inferMapDefaultValue(type: FreeObjectType, defaultValue: Any?): String = when (defaultValue) {
            is Map<*, *> -> {
                var initialValue = "new HashMap<>()"

                if (!defaultValue.isEmpty()) {
                    if (type.subtype!! is StrictObjectType) {
                        initialValue = "new HashMap<String, ${inferFieldTypeName(name, type.subtype)}>() {{"

                        val writer = internalWriter.withIncreasedIndentation()
                        val definition = (type.subtype as StrictObjectType).definition

                        writer.append("${name.asClassName()}Impl toFill = null;").nextLine()

                        for (pair in defaultValue) {
                            writer.append("toFill = new ${name.asClassName()}Impl();").nextLine()
                            for (field in definition.fields) {
                                val arg = inferDefaultValue(field.type, (pair.value as Map<*, *>)[field.name.rawText])
                                writer.append("toFill.${field.name.asSetterName()}($arg);").nextLine()
                            }

                            writer.append("put(\"${pair.key.toString()}\", toFill);").nextLine(2)
                        }
                    }
                }

                initialValue + internalWriter.append("}}")
            }
            else -> "new HashMap<>()"
        }

        private fun inferStrictObjectDefaultValue(defaultValue: Any?): String = when (defaultValue) {
            is EmptyObject -> "new ${name.asClassName()}Impl()"
            null -> "null"
            else -> throw IllegalStateException()
        }

        private fun inferArrayDefaultValues(defaultValue: List<*>): String {
            return when {
                defaultValue.isNotEmpty() -> asPredefinedCollection(defaultValue)
                else -> "Collections.emptyList()"
            }
        }
    }

    private data class GetterAndSetterRenderer(
            private val name: Field.Name,
            private val type: Type<*>) : Renderer {

        override fun render(writer: TextWriter) {

            writer.append("@Override").nextLine()
            writer.append("public ${inferFieldTypeName(name, type)} ${name.asGetterName(type)}() {")
            writer.withIncreasedIndentation().append("return m${name.asClassName()};")
            writer.append("}").nextLine(2)

            writer.append("@Override").nextLine()
            writer.append("public boolean ${name.asGetterName(type)}Set() {")
            writer.withIncreasedIndentation().append("return m${name.asClassName()}Set;")
            writer.append("}").nextLine(2)

            writer.append("public void ${name.asSetterName()}(${inferFieldTypeName(name, type)} ${name.asFieldName()}) {")
            with(writer.withIncreasedIndentation()) {
                append("m${name.asClassName()}Set = true;").nextLine()
                append("m${name.asClassName()} = ${name.asFieldName()};")
            }
            writer.append("}")
        }
    }

    private data class MergeMethodRenderer(
            val fields: List<Field<*>>,
            val name: String) : Renderer {

        override fun render(writer: TextWriter) {
            writer.append("@Override").nextLine()
            writer.append("public void merge($name from) {")
            writer.nextLine()
            for (field in fields) {
                with(writer.withIncreasedIndentation()) {
                    if (field.type is StrictObjectType) {
                        writer.append("m${field.name.asClassName()}.merge(from.${field.name.asGetterName(field.type)}());")
                    } else {
                        writer.append("if(!m${field.name.asClassName()}Set && from.${field.name.asGetterName(field.type)}Set()) {")
                        writer.append("${field.name.asSetterName()}(from.${field.name.asGetterName(field.type)}());")
                        writer.append("}")
                    }
                }
            }
            writer.append("}").nextLine()
        }
    }
}



