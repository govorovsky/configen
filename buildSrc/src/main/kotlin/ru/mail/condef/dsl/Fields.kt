package ru.mail.condef.dsl

import java.util.regex.Pattern

data class Field<N>(
        val name: Name,
        val type: Type<N>,
        val absenceHandler: AbsenceHandler? = null,
        val validator: Validator? = null,
        val description: String = ""
) {

    data class Name(val rawText: String, val customName: String = "")
}

infix fun <N> String.of(type: Type<N>) = Field(Field.Name(this), type)

private fun <T, N> Field<N>.checkRedefinition(actual: T, expected: T, fieldName: String, newInstance: Field<N>): Field<N> {
    if (actual == expected) {
        return newInstance
    } else {
        throw IllegalArgumentException("Redefinition of $fieldName in field $name")
    }
}

infix fun <N> Field<N>.withDescription(description: String): Field<N> = checkRedefinition(
        this.description, "", "description", copy(description = description)
)

infix fun <N> Field<N>.withDefault(newDefaultValue: N?): Field<N> = checkRedefinition(
        absenceHandler, null, "default",
        copy(absenceHandler = SubstituteWithDefaultHandler(newDefaultValue))
)

infix fun <N> Field<N>.withAbsenceHandler(handler: AbsenceHandler): Field<N> = checkRedefinition(
        absenceHandler, null, "default",
        copy(absenceHandler = handler)
)

infix fun <N> Field<N>.withAllowedValues(values: List<N>): Field<N> = checkRedefinition(
        validator, null, "validator",
        copy(validator = SpecialValuesValidator(values))
)

infix fun <N> Field<List<N>>.withAllowedValuesInArray(values: List<N>): Field<List<N>> = checkRedefinition(
        validator, null, "validator",
        copy(validator = SpecialValuesValidator(values))
)

infix fun Field<Int>.withAllowedRange(range: IntRange): Field<Int> = checkRedefinition(
        validator, null, "validator",
        copy(validator = RangeValidator(range.start, range.endInclusive))
)

infix fun Field<String>.withAllowedPattern(pattern: Pattern): Field<String> = checkRedefinition(
        validator, null, "validator",
        copy(validator = RegexValidator(pattern))
)

infix fun <N> Field<N>.withClassName(className: String): Field<N> = checkRedefinition(
        validator, null, "validator",
        copy(name = Field.Name(this.name.rawText, className))
)

infix fun Field<Any?>.withValuesType(type: Type<*>): Field<Any?> =
        Field(name, FreeObjectType(type), absenceHandler, validator, description)

infix fun Field<Any?>.withValuesType(definition: Definition): Field<Any?> =
        Field(name, FreeObjectType(StrictObjectType(definition)), absenceHandler, validator, description)

infix fun <N> Field<N>.withFieldName(fieldName: String): Field<N> = withClassName(fieldName)

infix fun Field<List<String>>.withAllowedPatternOfEachElement(pattern: Pattern): Field<List<String>> = checkRedefinition(
        validator, null, "validator",
        copy(validator = RegexValidator(pattern))
)

infix fun Field<Any?>.restrictedBy(definition: Definition): Field<Unit?> =
        Field(name, StrictObjectType(definition), absenceHandler, validator, description)

infix fun Field<List<Any?>>.withEachElementRestrictedBy(definition: Definition): Field<List<Unit?>> =
        Field(name, ArrayType(StrictObjectType(definition)), absenceHandler, validator, description)

infix fun Field<List<Any?>>.withEachElementRestrictedByAnyOf(definitions: Map<String, Definition>): Field<List<Unit?>> =
        Field(name, ArrayType(MultiObjectType(definitions)), absenceHandler, validator, description)

infix fun Field<List<Any?>>.withEachElementAllowedValuesType(type: Type<*>): Field<List<Any?>> =
        Field(name, ArrayType(FreeObjectType(type)), absenceHandler, validator, description)

infix fun Field<List<Any?>>.withEachElementAllowedValuesType(definition: Definition): Field<List<Any?>> =
        Field(name, ArrayType(FreeObjectType(StrictObjectType(definition))), absenceHandler, validator, description)

