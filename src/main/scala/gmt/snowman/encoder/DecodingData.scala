package gmt.snowman.encoder

import gmt.snowman.action.{BallAction, SnowmanAction}
import gmt.snowman.level.Location

import scala.collection.immutable

case class DecodingData(actions: immutable.Seq[SnowmanAction], balls: immutable.Seq[Location], actionsBall: immutable.Seq[BallAction])
