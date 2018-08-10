package gmt.snowman.encoder

import gmt.snowman.action.{BallAction, SnowmanAction}

import scala.collection.immutable

case class DecodingData(actions: immutable.Seq[SnowmanAction], actionsBall: immutable.Seq[BallAction])
