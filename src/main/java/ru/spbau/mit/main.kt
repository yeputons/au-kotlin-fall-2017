package ru.spbau.mit

import org.antlr.v4.runtime.BufferedTokenStream
import org.antlr.v4.runtime.CharStreams
import ru.spbau.mit.parser.LangLexer
import ru.spbau.mit.parser.LangParser
import java.nio.file.Paths

fun interpreterPrintln(args: List<InterpreterValue>): InterpreterValue? {
    println(args.joinToString(separator = " ") { it.toString() })
    return 0
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: main <file-to-interpret>")
        System.exit(1)
    }
    val filename = Paths.get(args[0])
    val program = LangParser(BufferedTokenStream(LangLexer(CharStreams.fromPath(filename)))).file().value!!
    val context = BaseInterpretationContext(createStdlibScope(
            println = { interpreterPrintln(it) }
    ))
    program.statements.forEach({ context.run(it) })
}
