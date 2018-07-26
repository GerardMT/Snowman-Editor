package gmt.snowman.level

case class Coordinate(x: Int, y: Int) {

    def +(that: Coordinate): Coordinate = Coordinate(x + that.x, y + that.y)

    def -(that: Coordinate): Coordinate = Coordinate(x - that.x, y - that.y)

    def unary_- = Coordinate(-x, -y)

    def ==(that: Coordinate): Boolean = x == that.x && y == that.y
}
