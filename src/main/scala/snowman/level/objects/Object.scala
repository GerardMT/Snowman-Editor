package snowman.level.objects

object Object {

    val ALL_OBJECTS = List(Empty, Wall, Grass, Snow, SmallBall, MediumBall, LargeBall, MediumSmallBall, LargeSmallBall, LargeMediumBall, LargeMediumSmallBall, Player, PlayerSnow)

    def createObject(c: Char): snowman.level.objects.Object = {
        ALL_OBJECTS.find(f => f.char == c.toLower).get
    }

    def isBall(o: snowman.level.objects.Object): Boolean =  {
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