package snowman.ui

import java.awt.Color
import java.io.{File, FileWriter}

import snowman.level.{Coordinate, Level, MutableLevel}
import snowman.level.`object`.Object

import scala.collection.mutable
import scala.swing.Swing._
import scala.swing.event.MousePressed
import scala.swing.{Component, Graphics2D, GridPanel, Panel}

object UILevel {

    def create(mutableLevel: MutableLevel, resourceManager: ResourceManager) = {
        new UILevel(mutableLevel.width, mutableLevel.height, mutableLevel, resourceManager)
    }
}

class UILevel private (width: Int, height: Int, var mutableLevel: MutableLevel, resourceManager: ResourceManager) extends GridPanel(width, height){

    create()

    def reload(level: MutableLevel) = {
        contents.remove(0, contents.size)
        mutableLevel = level

        create()
        revalidate()
    }

    private def create(): Unit = {
        for (x <- 0 until mutableLevel.width) {
            for (y <- 0 until mutableLevel.height) {
                contents += new Tile(mutableLevel(Coordinate(x, y)))
            }
        }
    }

    object Tile {

        val TILE_WIDTH = 50
        val TILE_HEIGHT = 50
    }

    class Tile(o: Object) extends Panel {

        focusable = true

        reactions += {
            case e: MousePressed  =>
        }

        background = Color.white
        preferredSize = (Tile.TILE_WIDTH, Tile.TILE_HEIGHT)

        override def paintComponent(g: Graphics2D): Unit = {
            super.paintComponent(g)
            g.drawImage(resourceManager.getResource(o), 0, 0, null)
        }
    }
}
