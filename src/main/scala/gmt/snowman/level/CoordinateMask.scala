package gmt.snowman.level

case class CoordinateMask(x: Boolean, y: Boolean) {

    def isEmpty: Boolean = !x && !y

    def isAnyEmpty: Boolean = !x || !y

    def unary_! = CoordinateMask(!x, !y)

    def |(that: CoordinateMask): CoordinateMask = CoordinateMask(x | that.x, y | that.y)

    def &(that: CoordinateMask): CoordinateMask = CoordinateMask(x & that.x, y & that.y)
}
