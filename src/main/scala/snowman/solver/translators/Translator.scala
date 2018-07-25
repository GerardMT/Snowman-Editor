package snowman.solver.translators

import snowman.solver.Problem

trait Translator {

    def translate(p: Problem): String
}
