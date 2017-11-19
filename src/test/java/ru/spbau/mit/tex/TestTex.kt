package ru.spbau.mit.tex

import org.junit.Test
import kotlin.test.assertEquals

class ArgumentsTest {
    @Test
    fun testCurlyArguments() {
        assertEquals("{hello,world}", curlyArguments("hello", "world").toString())
        assertEquals("{hello,,world}", curlyArguments("hello", "", "world").toString())
        assertEquals("{hello}", curlyArguments("hello").toString())
        assertEquals("{}", curlyArguments().toString())
    }

    @Test
    fun testCurlyArgumentsPairs() {
        assertEquals("{foo=bar,hello=world}", curlyArguments("foo" to "bar", "hello" to "world").toString())
        assertEquals("{foo=bar,hello}", curlyArguments("foo" to "bar", plainArgument("hello")).toString())
        assertEquals("{width=\\foo}", curlyArguments("width" to """\foo""").toString())
    }

    @Test
    fun testSquareArguments() {
        assertEquals("[hello,world]", squareArguments("hello", "world").toString())
        assertEquals("[hello,,world]", squareArguments("hello", "", "world").toString())
        assertEquals("[hello]", squareArguments("hello").toString())
        assertEquals("[]", squareArguments().toString())
    }

    @Test
    fun testSquareArgumentsPairs() {
        assertEquals("[foo=bar,hello=world]", squareArguments("foo" to "bar", "hello" to "world").toString())
        assertEquals("[foo=bar,hello]", squareArguments("foo" to "bar", plainArgument("hello")).toString())
        assertEquals("[width=\\foo]", squareArguments("width" to """\foo""").toString())
    }
}

class TestEscaping {
    @Test
    fun testTexTextEscaping() {
        assertEquals("a\\&b", "a&b".texTextEscape())
        assertEquals("a\\%b", "a%b".texTextEscape())
        assertEquals("a\\\$b", "a\$b".texTextEscape())
        assertEquals("a\\#b", "a#b".texTextEscape())
        assertEquals("a\\_b", "a_b".texTextEscape())
        assertEquals("a\\{b", "a{b".texTextEscape())
        assertEquals("a\\}b", "a}b".texTextEscape())
    }

    @Test
    fun testTexTextEscapingCommands() {
        assertEquals("a\\textasciitilde{}b", "a~b".texTextEscape())
        assertEquals("a\\textasciicircum{}b", "a^b".texTextEscape())
        assertEquals("a\\textbackslash{}b", "a\\b".texTextEscape())
    }
}

class TestTexDsl {
    @Test
    fun testCustomCommand() {
        val result = document {
            customCommand("hello",
                    curlyArguments("wow"),
                    squareArguments("foo"),
                    curlyArguments("1" to "2"),
                    squareArguments())
        }.toString()
        assertEquals(
                """
                |\hello{wow}[foo]{1=2}[]
                |""".trimMargin(), result)
    }

    @Test
    fun testCustomEnvironment() {
        val result = document {
            customEnvironment("hello",
                    curlyArguments("wow"),
                    squareArguments("foo"),
                    curlyArguments("1" to "2"),
                    squareArguments()) {
                customCommand("hello")
            }
        }.toString()
        assertEquals(
                """
                |\begin{hello}{wow}[foo]{1=2}[]
                |\hello
                |\end{hello}
                |""".trimMargin(), result)
    }

    @Test
    fun testRequireDocumentStarted() {
        val result = document {
            requireDocumentStarted()
            customCommand("hello")
        }.toString()
        assertEquals(
                """
                |\begin{document}
                |\hello
                |\end{document}
                |""".trimMargin(), result)
    }

    @Test
    fun testFrameNoTitle() {
        val result = document {
            frame {
                +"Hello world #1"
            }
        }.toString()
        assertEquals(
                """
                |\begin{document}
                |\begin{frame}
                |Hello world \#1
                |\end{frame}
                |\end{document}
                |""".trimMargin(), result)
    }


    @Test
    fun testFrameWithTitle() {
        val result = document {
            frame("The Title #1") {
                +"Hello world #1"
            }
        }.toString()
        assertEquals(
                """
                |\begin{document}
                |\begin{frame}{The Title \#1}
                |Hello world \#1
                |\end{frame}
                |\end{document}
                |""".trimMargin(), result)
    }

    @Test
    fun testLists() {
        val result = document {
            frame {
                itemize {
                    item { +"foo" }
                    item { +"bar" }
                    item {
                        enumerate {
                            item { +"foo2" }
                            item { +"bar2" }
                        }
                    }
                }
            }
        }.toString()
        assertEquals(
                """
                |\begin{document}
                |\begin{frame}
                |\begin{itemize}
                |\item
                |foo
                |\item
                |bar
                |\item
                |\begin{enumerate}
                |\item
                |foo2
                |\item
                |bar2
                |\end{enumerate}
                |\end{itemize}
                |\end{frame}
                |\end{document}
                |""".trimMargin(), result)
    }

    @Test
    fun testTextFormatting() {
        val result = document {
            frame {
                +"hello"
                +"""|This
                    |is
                    |a
                    |
                    |very
                    |long
                    |
                    |
                    |paragraph""".trimMargin()
                +"world"
                newParagraph()
                +"foo $2+2=4$"
                textbf { +"bar" }
                unescapedWriteLn("$2+2=4$")
            }
        }.toString()
        assertEquals(
                """
                |\begin{document}
                |\begin{frame}
                |hello
                |This
                |is
                |a
                |%
                |very
                |long
                |%
                |%
                |paragraph
                |world
                |
                |foo \$2+2=4\$
                |\textbf
                |{bar
                |}$2+2=4$
                |\end{frame}
                |\end{document}
                |""".trimMargin(), result)
    }

    @Test
    fun testFormula() {
        val result = document {
            frame {
                displayedFormula {
                    -"""sum_i i^2"""
                    -"=?"
                }
                displayedFormula {
                    frac(
                            { -"foo" },
                            { -"bar" }
                    )
                }
            }
        }.toString()
        assertEquals(
                """
                |\begin{document}
                |\begin{frame}
                |\[
                |sum_i i^2=?\]
                |\[
                |\frac{foo}{bar}\]
                |\end{frame}
                |\end{document}
                |""".trimMargin(), result)
    }

    @Test
    fun testGathered() {
        val result = document {
            frame {
                displayedFormula {
                    gathered(
                            { -"foo" },
                            { -"bar" },
                            { -"baz" }
                    )
                }
            }
        }.toString()
        assertEquals(
                """
                |\begin{document}
                |\begin{frame}
                |\[
                |\begin{gathered}
                |foo \\
                |bar \\
                |baz\end{gathered}
                |\]
                |\end{frame}
                |\end{document}
                |""".trimMargin(), result)
    }
}