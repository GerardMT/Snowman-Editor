package gmt.snowman.validator

import gmt.snowman.action.SnowmanAction
import gmt.snowman.level.Coordinate
import gmt.snowman.game.`object`._
import gmt.snowman.game.`object`.Object

case class PlayableLevel (private val characterCoordinate: Coordinate, private val map: TwoDimSeq[Object]) {

    def apply(action: SnowmanAction): Option[PlayableLevel] = {
        val nextCoordinate = characterCoordinate + action.shift
        if (!map.valid(nextCoordinate)) {
            None
        }

        val nextObject = map(nextCoordinate)

        if (nextObject == Grass || nextObject == Snow) {
            characterAction(nextCoordinate, nextObject)
        } else if (Object.isBall(nextObject)) {
            val nextNextCoordinate = nextCoordinate + action.shift
            if (!map.valid(nextNextCoordinate)) {
                return None
            }

            val nextNextObject = map(nextNextCoordinate)
            if (nextNextObject == Wall || nextNextObject == Empty) {
                return None
            }

            ballsAction(nextCoordinate, nextObject, nextNextCoordinate, nextNextObject)
        } else {
            None
        }
    }

    private def characterAction(nextCoordinate: Coordinate, nextObject: Object): Option[PlayableLevel] = {
        val nextMap = nextObject match {
            case Grass => map.updated(nextCoordinate, Character)
            case Snow => map.updated(nextCoordinate, CharacterSnow)
            case _ => return None
        }

        val finalMap = map(characterCoordinate) match {
            case Character => nextMap.updated(characterCoordinate, Grass)
            case CharacterSnow => nextMap.updated(characterCoordinate, Snow)
            case _ => return None
        }

        Some(PlayableLevel(nextCoordinate, finalMap))
    }

    private def ballsAction(nextCoordinate: Coordinate, nextObject: Object, nextNextCoordinate: Coordinate, nextNextObject: Object): Option[PlayableLevel] = {
        val (finalMap, newCharacterCoordinate) = nextObject match {
            case SmallBall | MediumBall | LargeBall =>
                val nextMap = nextNextObject match {
                    case MediumBall | LargeBall | LargeMediumBall  =>
                        map.updated(nextNextCoordinate, Object.pushBall(nextNextObject, nextObject))
                    case Grass =>
                        map.updated(nextNextCoordinate, nextObject)
                    case Snow =>
                        map.updated(nextNextCoordinate, Object.increaseBall(nextObject))
                    case _ =>
                        return None
                }
                val nextNextMap = nextMap.updated(nextCoordinate, Character)

                val returnMap = map(characterCoordinate) match {
                    case Character => nextNextMap.updated(characterCoordinate, Grass)
                    case CharacterSnow => nextNextMap.updated(characterCoordinate, Snow)
                    case _ => return None
                }
                (returnMap, nextCoordinate)
            case MediumSmallBall | LargeSmallBall | LargeMediumBall =>
                if (Object.isBall(nextNextObject)) {
                    return None
                }

                val (bottom, top) = Object.popBall(nextObject)
                val nextNextMap = map.updated(nextCoordinate, bottom)

                val returnMap = if (nextNextObject == Grass) {
                    nextNextMap.updated(nextNextCoordinate, top)
                } else if (nextNextObject == Snow){
                    nextNextMap.updated(nextNextCoordinate, Object.increaseBall(nextObject))
                } else {
                    return None
                }
                (returnMap , characterCoordinate)
            case _ =>
                return None
        }

        Some(PlayableLevel(newCharacterCoordinate, finalMap))
    }

    def isGoal: Boolean = {
        for (l <- map.seq) {
            for (o <- l) {
                o match {
                    case SmallBall | MediumBall | LargeBall | MediumSmallBall | LargeMediumBall | LargeSmallBall =>
                        return false
                    case _ =>
                }
            }
        }

        true
    }
}
