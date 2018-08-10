package gmt.snowman.util

import scala.collection.{immutable, mutable}
import scala.collection.mutable.ListBuffer

object AStar {

    def aStar[A](start: A, goal: A, allNodes: () => List[A], neighbours: A => List[A], heuristic: (A, A) => Float): immutable.Seq[A] = {
        val closedSet = mutable.Set.empty[A]

        val openSet = mutable.Set.empty[A]
        openSet += start

        val cameFrom = mutable.Map.empty[A, A]

        val gScore = mutable.Map.empty[A, Float]
        for (n <- allNodes()) {
            gScore(n) = Float.PositiveInfinity
        }
        gScore(start) = 0.0f

        val fScore = mutable.Map.empty[A, Float]
        for (n <- allNodes()) {
            fScore(n) = Float.PositiveInfinity
        }
        fScore(start) = heuristic(start, goal)

        while (openSet.nonEmpty) {
            var current = fScore.filter(f => openSet(f._1)).toList.minBy(_._2)._1
            if (current == goal) {
                return reconstrucPath(cameFrom, current)
            }

            openSet -= current
            closedSet += current

            for (neighbor <- neighbours(current)) {
                if (!closedSet(neighbor)) {
                    val tentative_gScore = gScore(current) + 1

                    if (!openSet(neighbor)) {
                        openSet += neighbor
                    }

                    if (tentative_gScore < gScore(neighbor)) {
                        cameFrom(neighbor) = current
                        gScore(neighbor) = tentative_gScore
                        fScore(neighbor) = gScore(neighbor) + heuristic(neighbor, goal)
                    }
                }
            }
        }

        List()
    }

    private def reconstrucPath[A](cameFrom: mutable.Map[A, A], start: A): immutable.Seq[A] = {
        val totalPath = ListBuffer(start)

        var current = start
        while (cameFrom.keys.exists(f => f == current)) {
            current = cameFrom(current)
            totalPath.append(current)
        }

        totalPath.toList
    }
}
