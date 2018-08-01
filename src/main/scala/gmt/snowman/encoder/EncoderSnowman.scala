package gmt.snowman.encoder

import gmt.planner.encoder.Encoder
import gmt.snowman.level.`object`._
import gmt.snowman.level.{Level, `object`}


abstract class EncoderSnowman(level: Level) extends Encoder {

    protected def getBallSize(o: `object`.Object): Int = o match {
        case SmallBall =>
            1
        case MediumBall =>
            2
        case LargeBall =>
            4
        case LargeMediumBall =>
            6
        case MediumSmallBall =>
            3
        case LargeMediumSmallBall =>
            7
    }
}
