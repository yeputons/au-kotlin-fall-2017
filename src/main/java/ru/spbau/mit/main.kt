package ru.spbau.mit

import org.antlr.v4.runtime.BufferedTokenStream
import org.antlr.v4.runtime.CharStreams
import ru.spbau.mit.ast.ast
import ru.spbau.mit.parser.LangLexer
import ru.spbau.mit.parser.LangParser
import java.nio.file.Paths
import kotlin.system.exitProcess

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
    val parser = LangParser(BufferedTokenStream(LangLexer(CharStreams.fromPath(filename))))
    val antlrAst = parser.file()
    if (parser.numberOfSyntaxErrors > 0) {
        System.err.println("Syntax errors detected, won't run")
        exitProcess(1)
    }
    val program = antlrAst.ast()
    val context = BaseInterpretationContext(createStdlibScope(
            println = { interpreterPrintln(it) }
    ))
    program.statements.forEach({ context.run(it) })
}
