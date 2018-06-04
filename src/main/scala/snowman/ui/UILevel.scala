package snowman.ui

import java.awt.Color
import java.io.{File, FileWriter}

import snowman.level.{Coordinate, Level, MutableLevel}
import snowman.level.`object`.{Empty, Object}

import scala.collection.mutable
import scala.swing.Swing._
import scala.swing.event.{MouseClicked, MouseMoved, MousePressed}
import scala.swing.{Component, Graphics2D, GridPanel, Panel}

object UILevel {

    def create(mutableLevel: MutableLevel, resourceManager: ResourceManager, picker: Picker) = {
        new UILevel(mutableLevel.width, mutableLevel.height, mutableLevel, resourceManager, picker)
    }
}

class UILevel private (width: Int, height: Int, var mutableLevel: MutableLevel, resourceManager: ResourceManager, picker: Picker) extends GridPanel(width, height){

    create()

    def reload(level: MutableLevel) = {
        contents.remove(0, contents.size)
        mutableLevel = level

        rows_=(level.height)
        columns_=(level.width)
        create()
        revalidate()
    }

    private def create(): Unit = {
        for (y <- mutableLevel.height - 1 to 0 by - 1) {
            for (x <- 0 until mutableLevel.width) {
                val c = Coordinate(x, y)
                contents += new Tile(mutableLevel(c), c)
            }
        }
    }

    object Tile {

        val TILE_WIDTH = 60
        val TILE_HEIGHT = 60
    }

    class Tile(var obj: Object, coordinate: Coordinate) extends Panel {

        focusable = true
        listenTo(mouse.clicks, mouse.moves)

        reactions += {
            case e: MousePressed  =>
                picker.current match {
                    case Some(o) =>
                        mutableLevel(coordinate) = o
                        obj = o
                        repaint()
                    case None =>
                }
        }

        background = Color.white
        preferredSize = (Tile.TILE_WIDTH, Tile.TILE_HEIGHT)

        override def paintComponent(g: Graphics2D): Unit = {
            super.paintComponent(g)
            g.drawImage(resourceManager.getResource(Empty), 0, 0, size.width, size.height, null)
            g.drawImage(resourceManager.getResource(obj), 0, 0, size.width, size.height, null)
        }
    }
}
