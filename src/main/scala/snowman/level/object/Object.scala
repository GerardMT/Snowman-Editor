package snowman.level.`object`

object Object {

    val ALL_OBJECTS = List(Empty, Wall, Grass, Snow, SmallBall, MediumBall, LargeBall, MediumSmallBall, LargeSmallBall, LargeMediumBall, LargeMediumSmallBall, Player, PlayerSnow)

    def createObject(c: Char): snowman.level.`object`.Object = {
        ALL_OBJECTS.find(f => f.char == c.toLower).get
    }
}

trait Object {
    def char: Char
}