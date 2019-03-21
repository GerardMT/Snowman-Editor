package gmt.snowman.action

case class BallLeft(override val ball: Int) extends BallAction(ball) {

    override def toString: String = "left(" + ball + ")"
}
