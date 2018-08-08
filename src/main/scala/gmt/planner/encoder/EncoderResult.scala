package gmt.planner.encoder

case class EncoderResult[A <: EncodingData](encoding: Encoding, encodingData: A)
