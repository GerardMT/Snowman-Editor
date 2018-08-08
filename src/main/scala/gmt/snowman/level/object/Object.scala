package gmt.snowman.level.`object`

object Object {

    val ALL_OBJECTS = List(Empty, Wall, Grass, Snow, SmallBall, MediumBall, LargeBall, MediumSmallBall, LargeSmallBall, LargeMediumBall, LargeMediumSmallBall, Character, CharacterSnow)

    val POP_PUSH_REALTIONS = List(((MediumBall, SmallBall), MediumSmallBall),
        ((LargeBall, SmallBall), LargeSmallBall),
        ((LargeBall, MediumBall), LargeMediumBall),
        ((LargeMediumBall, SmallBall), LargeMediumSmallBall))

    def createObject(c: Char): gmt.snowman.level.`object`.Object = {
        ALL_OBJECTS.find(f => f.char == c.toLower).get
    }

    def isBall(o: gmt.snowman.level.`object`.Object): Boolean = o match {
        case SmallBall | MediumBall | LargeBall | MediumSmallBall | LargeSmallBall | LargeMediumBall | LargeMediumSmallBall => true
        case _ => false
    }

    def isSnow(o: Object): Boolean = o match {
        case Snow | CharacterSnow => true
        case _ => false
    }

    def pushBall(bottom: Object, top: Object): Object = POP_PUSH_REALTIONS.find(f => f._1 == (bottom, top)).get._2

    def popBall(ball: Object): (Object, Object) = POP_PUSH_REALTIONS.find(f => f._2 == ball).get._1

    def increaseBall(ball: Object): Object = ball match {
        case SmallBall => MediumBall
        case MediumBall => LargeBall
        case LargeBall => LargeBall
    }
}

trait Object {

    def char: Char
}