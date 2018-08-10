package gmt.snowman.encoder

import gmt.snowman.action.SnowmanAction

import scala.collection.immutable

case class DecodingData(actions: immutable.Seq[SnowmanAction], actionsBall: immutable.Seq[SnowmanAction])
