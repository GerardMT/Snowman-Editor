package gmt.snowman.action

object BallAction {

    def apply(snowmanAction: SnowmanAction, ball: Int): BallAction = snowmanAction match {
        case Up =>
            BallUp(ball)
        case Down =>
            BallDown(ball)
        case Right =>
            BallRight(ball)
        case Left =>
            BallLeft(ball)
    }
}

abstract class BallAction (val ball: Int)
