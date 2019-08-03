package ru.mail.condef.renderer

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Element {
    fun render(builder: StringBuilder, indent: String)
}

class TextElement(private val text: String) : Element {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$text\n")
    }
}

typealias attributes = Map<String, String>

@DslMarker
annotation class HtmlTagMarker

@HtmlTagMarker
abstract class Tag(private val name: String, private val attrs: attributes) : Element {
    val children = arrayListOf<Element>()
    val className: String? by optionalAttr(attrs, "class")

    protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }

    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent<$name${renderAttributes()}>\n")
        for (c in children) {
            renderChild(c, builder, indent)
        }
        builder.append("$indent</$name>\n")
    }

    open fun renderChild(c: Element, builder: StringBuilder, indent: String) {
        c.render(builder, indent + "  ")
    }

    private fun renderAttributes(): String {
        val builder = StringBuilder()
        for ((attr, value) in attrs) {
            if (!value.isEmpty()) {
                builder.append(" $attr=\"$value\"")
            }
        }
        return builder.toString()
    }

    private fun <T, TValue> optionalAttr(properties: Map<String, TValue>, key: String): ReadOnlyProperty<T, TValue?> {
        return object : ReadOnlyProperty<T, TValue?> {
            override fun getValue(thisRef: T, property: KProperty<*>) = properties[key]
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

abstract class TagWithText(name: String, map: attributes) : Tag(name, map) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}

class HTML : TagWithText("html", mapOf()) {
    fun head(init: Head.() -> Unit) = initTag(Head(mapOf()), init)

    fun body(init: Body.() -> Unit) = initTag(Body(mapOf()), init)
}

class Head(map: attributes) : TagWithText("head", map) {
    fun title(init: Title.() -> Unit) = initTag(Title(), init)
    fun style(init: Style.() -> Unit) = initTag(Style(), init)
    fun script(init: Script.() -> Unit) = initTag(Script(), init)
    fun link(attrs: attributes, init: Link.() -> Unit) = initTag(Link(attrs), init)
}

class Title : TagWithText("title", mapOf())
class Style : TagWithText("style", mapOf())
class Script : TagWithText("script", mapOf())
class Link(attrs: attributes): TagWithText("link", attrs)

abstract class BodyTag(name: String, attrs: attributes) : TagWithText(name, attrs) {
    fun table(attrs: attributes = mapOf(), init: Table.() -> Unit) = initTag(Table(attrs), init)
    fun b(attrs: attributes = mapOf(), init: B.() -> Unit) = initTag(B(attrs), init)
    fun p(attrs: attributes = mapOf(), init: P.() -> Unit) = initTag(P(attrs), init)
    fun h1(attrs: attributes = mapOf(), init: H1.() -> Unit) = initTag(H1(attrs), init)
    fun pre(attrs: attributes = mapOf(), init: Pre.() -> Unit) = initTag(Pre(attrs), init)
    fun a(attrs: attributes = mapOf(), init: A.() -> Unit) = initTag(A(attrs), init)
    fun div(attrs: attributes = mapOf(), init: Div.() -> Unit) = initTag(Div(attrs), init)
}

class Body(attrs: attributes) : BodyTag("body", attrs)
class B(attrs: attributes) : BodyTag("b", attrs)
class P(attrs: attributes) : BodyTag("p", attrs)
class H1(attrs: attributes) : BodyTag("h1", attrs)
class Div(attrs: attributes) : BodyTag("div", attrs)
class Pre(attrs: attributes) : BodyTag("pre", attrs) {

    override fun render(builder: StringBuilder, indent: String) {
        super.render(builder, "")
    }

    override fun renderChild(c: Element, builder: StringBuilder, indent: String) {
        c.render(builder, "") /* pre rendered as is without indent */
    }
}

abstract class TableTag(name: String, map: attributes) : BodyTag(name, map) {
    fun thead(attrs: attributes = mapOf(), init: THead.() -> Unit) = initTag(THead(attrs), init)
    fun th(attrs: attributes = mapOf(), init: Th.() -> Unit) = initTag(Th(attrs), init)
    fun tr(attrs: attributes = mapOf(), init: Tr.() -> Unit) = initTag(Tr(attrs), init)
    fun td(attrs: attributes = mapOf(), init: Td.() -> Unit) = initTag(Td(attrs), init)
}

class Table(attrs: attributes) : TableTag("table", attrs)
class THead(attrs: attributes) : TableTag("thead", attrs)
class Tr(attrs: attributes) : TableTag("tr", attrs)
class Th(attrs: attributes) : TableTag("th", attrs)
class Td(attrs: attributes) : TableTag("td", attrs)

class A(attrs: attributes) : BodyTag("a", attrs) {
    val href by attrs
}

fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}

fun attrs(vararg atts: Pair<String, String>) = mapOf(*atts)