package gmt.planner.translator

import gmt.snowman.encoder.Encoding

trait Translator {

    def translate(p: Encoding): String
}
