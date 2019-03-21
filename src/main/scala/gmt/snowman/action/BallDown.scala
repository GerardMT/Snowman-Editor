package gmt.snowman.action

case class BallDown(override val ball: Int) extends BallAction(ball) {

    override def toString: String = "down(" + ball + ")"
}