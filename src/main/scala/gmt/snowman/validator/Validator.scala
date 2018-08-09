package gmt.snowman.validator

import gmt.snowman.action.SnowmanAction
import gmt.snowman.level.Level

object Validator {

    def validate(level: Level, actions: Seq[SnowmanAction]): (Boolean, Int) = {

        var playableLevel: Option[PlayableLevel] = Some(level.toPlayableLevel)
        val actionsIterator = actions.iterator

        var action = 0

        while (playableLevel.isDefined && actionsIterator.hasNext) {
            playableLevel = playableLevel.get.apply(actionsIterator.next())
            action += 1
        }

        if (playableLevel.isDefined) {
            (playableLevel.get.isGoal, action)
        } else {
            (false, action)
        }
    }
}