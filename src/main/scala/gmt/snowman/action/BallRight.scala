package gmt.snowman.action

case class BallRight(override val ball: Int) extends BallAction(ball) {

    override def toString: String = "right(" + ball + ")"
}

