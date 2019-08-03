package ru.mail.condef.dsl

import java.util.regex.Pattern

sealed class Validator

class SpecialValuesValidator<out T>(val values: List<T>) : Validator()

class RegexValidator(val pattern: Pattern) : Validator()

class RangeValidator(val fromInclusive: Int, val toInclusive: Int) : Validator()
