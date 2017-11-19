package ru.spbau.mit.tex

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.StringWriter
import java.io.Writer

enum class ArgumentsBrackets(val opening: String, val closing: String) {
    CURLY("{", "}"),
    SQUARE("[", "]")
}

class ArgumentsList(val type: ArgumentsBrackets, vararg val args: String) {
    override fun toString(): String =
            type.opening +
                    args.joinToString(",") +
                    type.closing

    fun nonEmptyOrNull(): ArgumentsList? = if (args.isNotEmpty()) this else null
}

private fun keyValueArguments(vararg args: Pair<String, String?>): Array<String> =
        args.map {
            assert("=" !in it.first)
            if (it.second != null)
                "${it.first}=${it.second}"
            else
                it.first
        }.toTypedArray()

fun curlyArguments() = ArgumentsList(ArgumentsBrackets.CURLY)
fun curlyArguments(vararg args: String) = ArgumentsList(ArgumentsBrackets.CURLY, *args)
fun curlyArguments(vararg args: Pair<String, String?>) = ArgumentsList(ArgumentsBrackets.CURLY, *keyValueArguments(*args))

fun squareArguments() = ArgumentsList(ArgumentsBrackets.SQUARE)
fun squareArguments(vararg args: String) = ArgumentsList(ArgumentsBrackets.SQUARE, *args)
fun squareArguments(vararg args: Pair<String, String?>) = ArgumentsList(ArgumentsBrackets.SQUARE, *keyValueArguments(*args))

fun plainArgument(name: String) = name to null

private fun Writer.writeCommand(name: String, vararg argsLists: ArgumentsList?) {
    write("\\$name")
    argsLists
            .filterNotNull()
            .forEach { write(it.toString()) }
    write("\n")
}

private fun Writer.writeEnvironment(name: String, vararg argsLists: ArgumentsList?, content: () -> Unit) {
    writeCommand("begin", curlyArguments(name), *argsLists)
    content()
    writeCommand("end", curlyArguments(name))
}

private object TexEscaping {
    // Based on https://tex.stackexchange.com/a/34586/98293
    val textSubstitutions: Map<Char, String> =
            "&%$#_{}".map { it -> it to "\\$it" }.toMap() + mapOf(
                    '~' to "\\textasciitilde{}",
                    '^' to "\\textasciicircum{}",
                    '\\' to "\\textbackslash{}")
}

fun String.texTextEscape(): String {
    val builder = StringBuilder()
    for (c in this) {
        val subst = TexEscaping.textSubstitutions[c]
        // Do not use Elvis operator so a String/Char overload is called
        // instead of generic Object one and extra strings are not created.
        if (subst != null) builder.append(subst)
        else builder.append(c)
    }
    return builder.toString()
}

@DslMarker
annotation class TexTagMarker

@TexTagMarker
abstract class TexDsl(protected val out: Writer) {
    fun customCommand(name: String, vararg argsLists: ArgumentsList?) {
        out.writeCommand(name, *argsLists)
    }

    fun customEnvironment(name: String, vararg argsLists: ArgumentsList?, content: () -> Unit) {
        out.writeEnvironment(name, *argsLists, content = content)
    }
}

class Document(out: Writer) : TexDsl(out) {
    private var documentStarted = false

    fun requirePreamble() {
        if (documentStarted) {
            throw IllegalStateException("Document has already started")
        }
    }

    fun requireDocumentStarted() {
        if (!documentStarted) {
            documentStarted = true
            out.writeCommand("begin", curlyArguments("document"))
        }
    }

    internal fun endDocument() {
        if (documentStarted) {
            out.writeCommand("end", curlyArguments("document"))
        }
    }

    fun documentClass(cls: String, vararg options: String) {
        requirePreamble()
        out.writeCommand("documentclass", squareArguments(*options).nonEmptyOrNull(), curlyArguments(cls))
    }

    fun usepackage(name: String, vararg options: String) {
        requirePreamble()
        out.writeCommand("usepackage", squareArguments(*options).nonEmptyOrNull(), curlyArguments(name))
    }

    fun usetheme(name: String) {
        requirePreamble()
        out.writeCommand("usetheme", curlyArguments(name))
    }

    fun frame(frameTitle: String? = null, vararg options: String, init: Frame.() -> Unit) {
        requireDocumentStarted()
        out.writeEnvironment(
                "frame",
                squareArguments(*options).nonEmptyOrNull(),
                if (frameTitle != null) curlyArguments(frameTitle.texTextEscape())
                else null
        ) {
            Frame(out).init()
        }
    }

    companion object {
        fun render(out: Writer, init: Document.() -> Unit) {
            val document = Document(out)
            document.init()
            document.endDocument()
        }
    }
}

class ItemsList(out: Writer) : TexDsl(out) {
    fun item(init: ContentContext.() -> Unit) {
        out.writeCommand("item")
        ContentContext(out).init()
    }
}

open class ContentContext(out: Writer) : TexDsl(out) {
    operator fun String.unaryPlus() {
        this.lines()
                .map { it.texTextEscape() }
                .map { if (it.trim() == "") "%" else it }
                .forEach { out.write(it); out.write("\n") }
    }

    fun unescapedWriteLn(s: String) {
        out.write(s)
        out.write("\n")
    }

    fun newParagraph() {
        out.write("\n")
    }

    fun displayedFormula(init: Formula.() -> Unit) {
        out.write("\\[\n")
        Formula(out).init()
        out.write("\\]\n")
    }

    fun textbf(init: ContentContext.() -> Unit) {
        out.writeCommand("textbf")
        out.write("{")
        init()
        out.write("}")
    }

    fun itemize(init: ItemsList.() -> Unit) {
        out.writeEnvironment("itemize") {
            ItemsList(out).init()
        }
    }

    fun enumerate(init: ItemsList.() -> Unit) {
        out.writeEnvironment("enumerate") {
            ItemsList(out).init()
        }
    }
}

class Frame(out: Writer) : ContentContext(out)

class Formula(out: Writer) : TexDsl(out) {
    operator fun String.unaryMinus() {
        out.write(this)
    }

    fun frac(numerator: Formula.() -> Unit, denominator: Formula.() -> Unit) {
        out.write("\\frac{")
        Formula(out).numerator()
        out.write("}{")
        Formula(out).denominator()
        out.write("}")
    }

    fun gathered(vararg formulas: Formula.() -> Unit) {
        out.writeEnvironment("gathered") {
            for ((i, formula) in formulas.withIndex()) {
                if (i != 0) {
                    out.write(" \\\\\n")
                }
                Formula(out).formula()
            }
        }
    }
}

class DocumentBuilder(private val init: Document.() -> Unit) {
    fun toOutputStream(out: OutputStream) {
        OutputStreamWriter(out).use { writer -> Document.render(writer, init) }
    }

    override fun toString(): String {
        val writer = StringWriter()
        Document.render(writer, init)
        return writer.toString()
    }
}

fun document(init: Document.() -> Unit): DocumentBuilder = DocumentBuilder(init)