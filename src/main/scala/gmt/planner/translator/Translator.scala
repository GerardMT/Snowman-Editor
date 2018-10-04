package gmt.planner.translator

import gmt.planner.encoder.Encoding

trait Translator {

    def translate(e: Encoding): String
}
