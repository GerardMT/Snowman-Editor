package gmt.snowman.action

object BallAction {

    def apply(snowmanAction: SnowmanAction, ball: Int): BallAction = snowmanAction match {
        case CharacterUp =>
            BallUp(ball)
        case CharacterDown =>
            BallDown(ball)
        case CharacterRight =>
            BallRight(ball)
        case CharacterLeft =>
            BallLeft(ball)
    }
}

abstract class BallAction (val ball: Int) extends SnowmanAction {

    def toStringRef: String = toString + "(" + ball + ")"
}
