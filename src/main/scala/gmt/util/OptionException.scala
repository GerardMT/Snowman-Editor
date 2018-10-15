package gmt.util

case class OptionException[A, B <: Exception](private val option: Option[A], private val exception: B) {

    def get: A = option match {
        case Some(p: A) =>
            p
        case None =>
            throw exception
    }

    def isDefined: Boolean = {
        option.isDefined
    }
}