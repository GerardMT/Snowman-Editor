package gmt.snowman.level

case class Coordinate(x: Int, y: Int) {

    def +(that: Coordinate): Coordinate = Coordinate(x + that.x, y + that.y)

    def -(that: Coordinate): Coordinate = Coordinate(x - that.x, y - that.y)

    def unary_- = Coordinate(-x, -y)

    def ==(that: Coordinate): Boolean = x == that.x && y == that.y

    def euclidianDistance(that: Coordinate): Double = Math.sqrt(Math.pow(that.x - x, 2) +  Math.pow(that.y - y, 2))

    def manhattanDistance(that: Coordinate): Int = Math.abs(x - that.x) + Math.abs(y - that.y)
}
