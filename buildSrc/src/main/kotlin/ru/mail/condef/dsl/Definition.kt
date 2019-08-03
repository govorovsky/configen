package ru.mail.condef.dsl

class Definition(val fields: List<Field<*>>)

fun definition(vararg fields: Field<*>) = Definition(listOf(*fields))

