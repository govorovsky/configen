package ru.mail.condef.renderer

import ru.mail.condef.dsl.*

fun getValidator(field: Field<*>, type: Type<*>): ValidatorRenderer<*> {
    return when(type) {
        is ArrayType<*> -> StubValidator()
        else -> when(field.validator) {
            null -> StubValidator()
            is SpecialValuesValidator<*> -> SpecialValuesValidatorRenderer(field.validator)
            is RegexValidator -> RegexValidatorRenderer(field.validator)
            is RangeValidator -> RangeValidatorRenderer(field.validator)
        }
    }
}

abstract class ValidatorRenderer<out T>(val validator: T?) {
    abstract fun appendPreValidate(writer: TextWriter)
    abstract fun validationCondition(): String

    open fun doIfValidate(writer: TextWriter, toAppend: TextWriter.() -> Unit): ValidatorChain {
        writer.append("if (${validationCondition()}) {")
        writer.withIncreasedIndentation().toAppend()
        writer.append("}")
        return ValidatorChain(writer)
    }

    open class ValidatorChain(private val writer: TextWriter) {
        open infix fun otherwise(toAppend: TextWriter.() -> Unit) {
            writer.append(" else {")
            writer.withIncreasedIndentation().toAppend()
            writer.append("}")
        }
    }
}

class StubValidator : ValidatorRenderer<Any>(null) {
    override fun validationCondition(): String {
        return ""
    }

    override fun doIfValidate(writer: TextWriter, toAppend: TextWriter.() -> Unit): ValidatorChain {
        writer.toAppend()
        return object : ValidatorChain(writer) {
            override fun otherwise(toAppend: TextWriter.() -> Unit) {
            }
        }
    }

    override fun appendPreValidate(writer: TextWriter) {
    }
}

class SpecialValuesValidatorRenderer(validator: SpecialValuesValidator<*>) : ValidatorRenderer<SpecialValuesValidator<*>>(validator) {

    override fun appendPreValidate(writer: TextWriter) {
        writer.append("Set<?> allowed = new HashSet<>(${asPredefinedCollection(validator!!.values)});")
        writer.nextLine()
    }

    override fun validationCondition(): String {
        return if(validator!!.values[0] is String) "allowed.contains(((String)value).toLowerCase(Locale.ENGLISH))"
        else "allowed.contains(value)"
    }
}

class RegexValidatorRenderer(validator: RegexValidator) : ValidatorRenderer<RegexValidator>(validator) {
    override fun appendPreValidate(writer: TextWriter) {
        writer.append("Matcher matcher = Pattern.compile(\"${StringEscapeUtils.escapeJava(validator!!.pattern.toString())}\").matcher(\"\");")
        writer.nextLine()
    }

    override fun validationCondition(): String {
        return "matcher.reset(value).matches()"
    }
}

class RangeValidatorRenderer(validator: RangeValidator) : ValidatorRenderer<RangeValidator>(validator) {
    override fun appendPreValidate(writer: TextWriter) {
    }

    override fun validationCondition(): String {
        return "value >= ${validator!!.fromInclusive} && value <= ${validator.toInclusive}"
    }
}
