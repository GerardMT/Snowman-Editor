package gmt.snowman.action

case class BallUp(override val ball: Int) extends BallAction(ball) {

    override def toString: String = "up(" + ball + ")"
}
