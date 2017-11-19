package ru.spbau.mit

import ru.spbau.mit.tex.curlyArguments
import ru.spbau.mit.tex.document
import ru.spbau.mit.tex.squareArguments

fun main(args: Array<String>) {
    val rows = sequenceOf("foo", "bar", "baz")
    document {
        documentClass("beamer")
        usepackage("babel", "russian")
        usepackage("minted")
        usetheme("CambridgeUS")
        frame("This is frame #1's title", "fragile") {
            itemize {
                for (row in rows) {
                    item { + "$row text" }
                }
            }

            enumerate {
                item { +"~10$ for foo_bar" }
                item { +"This is #2" }
            }

            displayedFormula {
                -"ax+by=c"
            }
            +"Now let's solve it:"
            displayedFormula {
                gathered(
                        { -"ax=c-by" },
                        {
                            -"x="
                            frac({-"c-by"}, {-"a"})
                        }
                )
            }

            customEnvironment("minted", curlyArguments("kotlin")) {
                unescapedWriteLn(
                """
                |val a = 1
                |
                """.trimMargin())
            }
        }
    }.toOutputStream(System.out)
}
