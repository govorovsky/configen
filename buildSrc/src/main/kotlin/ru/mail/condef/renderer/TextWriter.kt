package ru.mail.condef.renderer

class TextWriter(private val indentation: Int = 0) {

    private val lines = mutableListOf<Any>()
    private val line = StringBuilder()

    init {
        line.append(" ".repeat(indentation))
    }

    fun newWriter() = TextWriter(indentation)

    fun withIncreasedIndentation(): TextWriter {
        nextLine()
        val writer = TextWriter(indentation + Constants.INDENTATION_STEP)
        lines.add(writer)
        return writer
    }

    fun append(append: String): TextWriter = apply { line.append(append) }

    fun appendLine(append: String): TextWriter = apply { nextLine().append(append) }

    fun append(renderer: Renderer): TextWriter = apply { renderer.render(this) }

    fun join(renderers: Collection<Renderer>, joiner: TextWriter.() -> Unit): TextWriter {
        for ((i, renderer) in renderers.withIndex()) {
            append(renderer)
            if (i != renderers.size - 1) {
                this.joiner()
            }
        }
        return this
    }

    fun nextLine(linesToAdd: Int = 1): TextWriter = apply {
        for (i in 0..(linesToAdd - 1)) {
            commitLine()
            initNewLine()
        }
    }

    private fun commitLine() {
        lines.add(line.toString())
    }

    private fun initNewLine() {
        line.setLength(0)
        line.append(" ".repeat(indentation))
    }

    override fun toString(): String {
        commitLine()
        return lines.joinToString("\n")
    }

    companion object Constants {
        private const val INDENTATION_STEP: Int = 4
    }
}
