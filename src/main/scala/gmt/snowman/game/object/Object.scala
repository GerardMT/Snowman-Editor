package gmt.snowman.game.`object`

object Object {

    val ALL_OBJECTS = List(Empty, Wall, Grass, Snow, SmallBall, MediumBall, LargeBall, MediumSmallBall, LargeSmallBall, LargeMediumBall, LargeMediumSmallBall, Character, CharacterSnow)

    val POP_PUSH_RELATIONS = List(((MediumBall, SmallBall), MediumSmallBall),
        ((LargeBall, SmallBall), LargeSmallBall),
        ((LargeBall, MediumBall), LargeMediumBall),
        ((LargeMediumBall, SmallBall), LargeMediumSmallBall))

    def createObject(c: Char): gmt.snowman.game.`object`.Object = {
        ALL_OBJECTS.find(f => f.char == c.toLower).get
    }

    def isBall(o: gmt.snowman.game.`object`.Object): Boolean = o match {
        case SmallBall | MediumBall | LargeBall | MediumSmallBall | LargeSmallBall | LargeMediumBall | LargeMediumSmallBall => true
        case _ => false
    }

    def isSnow(o: Object): Boolean = o match {
        case Snow | CharacterSnow => true
        case _ => false
    }

    def unpackBalls(ball: Object): List[Object] = ball match {
        case SmallBall =>
            List(SmallBall)
        case MediumBall =>
            List(MediumBall)
        case LargeBall =>
            List(LargeBall)
        case MediumSmallBall =>
            List(MediumBall, SmallBall)
        case LargeSmallBall =>
            List(LargeBall, SmallBall)
        case LargeMediumBall =>
            List(LargeBall, MediumBall)
        case LargeMediumSmallBall =>
            List(LargeBall, MediumBall, SmallBall)
    }

    def pushBall(bottom: Object, top: Object): Object = POP_PUSH_RELATIONS.find(f => f._1 == (bottom, top)).get._2

    def popBall(ball: Object): (Object, Object) = POP_PUSH_RELATIONS.find(f => f._2 == ball).get._1

    def increaseBall(ball: Object): Object = ball match {
        case SmallBall => MediumBall
        case MediumBall => LargeBall
        case LargeBall => LargeBall
    }

    def isPlayableArea(o: Object): Boolean = o match {
        case Empty | Wall => false
        case _ => true
    }
}

trait Object {

    def char: Char
}