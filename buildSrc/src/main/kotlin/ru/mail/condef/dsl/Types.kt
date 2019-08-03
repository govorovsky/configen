package ru.mail.condef.dsl

sealed class Type<out N> {
    abstract fun defaultValue(): N
}

interface CompositeType {
    val subtype: Type<*>?
}

class StringType : Type<String>() {
    override fun defaultValue(): String = ""
}

class IntegerType : Type<Int>() {
    override fun defaultValue(): Int = 0
}

class LongType : Type<Long>() {
    override fun defaultValue(): Long = 0
}

class FreeObjectType(override val subtype: Type<*>? = null) : Type<Any?>(), CompositeType {
    override fun defaultValue(): Any? = null
}

class StrictObjectType(val definition: Definition) : Type<Unit?>() {
    override fun defaultValue(): Unit? = null
}

class MultiObjectType(val types:Map<String, Definition>) : Type<Unit?>() {
    override fun defaultValue(): Unit? = null
}

class BoolType : Type<Boolean>() {
    override fun defaultValue(): Boolean = false
}

class ArrayType<out N>(override val subtype: Type<N>) : Type<List<N>>(), CompositeType {
    override fun defaultValue(): List<N> = listOf()
}
