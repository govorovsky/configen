package ru.mail.condef.dsl

sealed class AbsenceHandler

class SubstituteWithDefaultHandler<out T>(val defaultValue: T) : AbsenceHandler()
class RequiredHandler : AbsenceHandler()

fun requiredHandler() : RequiredHandler {
    return RequiredHandler()
}