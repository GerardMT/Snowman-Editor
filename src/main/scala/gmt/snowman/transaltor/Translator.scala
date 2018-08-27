package gmt.snowman.transaltor

import gmt.planner.encoder.Encoding

trait Translator {

    def translate(p: Encoding): String
}
