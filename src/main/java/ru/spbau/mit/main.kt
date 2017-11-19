package ru.spbau.mit

import ru.spbau.mit.tex.curlyArguments
import ru.spbau.mit.tex.document
import ru.spbau.mit.tex.plainArgument
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

        frame("This is just a text frame") {
            // Copied from https://en.wikibooks.org/wiki/LaTeX/Presentations#References_.28Beamer.29
            +"As the reference list grows, the reference slide will divide into two slides and so on, through use of the allowframebreaks option."
            +"Individual items can be cited after adding an 'optional' label to the relevant bibitem stanza."
            +"The citation call is simply \\cite. Beamer also supports limited customization of the way references are presented (see the manual)."
            +"Those who wish to use natbib, for example, with Beamer may need to troubleshoot both their document setup and the relevant BibTeX style file."
            newParagraph()
            +"The different types of referenced work are indicated with a little symbol (e.g. a book, an article, etc.)."
            +"The Symbol is set with the commands "; textbf {+"beamertemplatebookbibitems"}; +" and beamertemplatearticlebibitems."
            +"It is also possible to use setbeamertemplate directly, like so"
        }

        frame {
            customCommand("includegraphics",
                    squareArguments(
                            "width" to """\textwidth""",
                            "height" to """0.8\textheight""",
                            plainArgument("keepaspectratio")
                    ),
                    curlyArguments("mypicture.png"))
        }
    }.toOutputStream(System.out)
}
