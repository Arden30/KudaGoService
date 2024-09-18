package arden.java.dsl

class Readme {
    private val children = StringBuilder()

    fun header(level: Int, init: Header.() -> Unit): Header {
        val header = Header(level)
        header.init()
        children.append(header)
        return header
    }

    fun text(init: Text.() -> Unit): Text {
        val text = Text()
        text.init()
        children.append(text)
        return text
    }

    override fun toString(): String = children.toString()
}

fun readme(init: Readme.() -> Unit): Readme {
    val readme = Readme()
    readme.init()
    return readme
}

class Text {
    private val text = StringBuilder()

    fun bold(text: String): String = "**$text**"
    fun link(link: String, text: String): String = "[$text]($link)"
    fun underlined(text: String): String = "__$text" + "__"

    operator fun String.unaryPlus() {
        text.append(this).append("\n\n")
    }

    override fun toString(): String = text.toString()
}

class Header(private val level: Int) {
    private val head = StringBuilder()

    operator fun String.unaryPlus() {
        head.append(this)
    }

    override fun toString(): String {
        val prefix = "#".repeat(level).plus(" ")
        return prefix + head.toString() + "\n"
    }
}