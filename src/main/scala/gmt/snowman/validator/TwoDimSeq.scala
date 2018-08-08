package gmt.snowman.validator

import gmt.snowman.level.Coordinate

import scala.collection.immutable

case class TwoDimSeq[A](seq: immutable.Seq[immutable.Seq[A]]) {

    def apply(coordinate: Coordinate): A = {
        seq(coordinate.x)(coordinate.y)
    }

    def updated(coordinate: Coordinate, value: A): TwoDimSeq[A] = {
        TwoDimSeq(seq.updated(coordinate.x, seq(coordinate.x).updated(coordinate.y, value)))
    }

    def valid(coordinate: Coordinate): Boolean = coordinate.x > 0 && coordinate.x < seq.size && coordinate.y > 0 && coordinate.y < seq.head.size
}
