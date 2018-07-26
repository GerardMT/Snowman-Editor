package gmt.snowman.level.`object`

object Object {

    val ALL_OBJECTS = List(Empty, Wall, Grass, Snow, SmallBall, MediumBall, LargeBall, MediumSmallBall, LargeSmallBall, LargeMediumBall, LargeMediumSmallBall, Player, PlayerSnow)

    def createObject(c: Char): gmt.snowman.level.`object`.Object = {
        ALL_OBJECTS.find(f => f.char == c.toLower).get
    }

    def isBall(o: gmt.snowman.level.`object`.Object): Boolean =  {
        o match {
            case SmallBall | MediumBall | LargeBall | MediumSmallBall | LargeSmallBall | LargeMediumBall | LargeMediumSmallBall =>
                true
            case _ =>
                false
        }
    }
}

trait Object {
    def char: Char
}