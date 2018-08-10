package gmt.ui

import java.awt.{Color, Font, Rectangle}
import java.awt.event.{MouseEvent, MouseListener}

import gmt.snowman.game.`object`.{Empty, Object}
import gmt.snowman.level.{Coordinate, MutableLevel}

import scala.collection.immutable
import scala.swing.Swing._
import scala.swing.{Graphics2D, GridPanel, Panel}

object UILevel {

    def create(mutableLevel: MutableLevel, resourceManager: ResourceManager, picker: Picker): UILevel = {
        new UILevel(mutableLevel.width, mutableLevel.height, mutableLevel, resourceManager, picker)
    }
}

class UILevel private (private var width: Int, private var height: Int, var mutableLevel: MutableLevel, resourceManager: ResourceManager, picker: Picker) extends GridPanel(width, height) {

    private var paintCoordinates = false

    private var pressed = false

    create()

    def reload(level: MutableLevel): Unit = {
        contents.remove(0, contents.size)
        mutableLevel = level

        width = mutableLevel.width
        height = mutableLevel.height

        rows_=(level.height)
        columns_=(level.width)
        create()
        revalidate()
    }

    def showCoordinates_(paint: Boolean): Unit = {
        paintCoordinates = paint
        repaint()
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

        peer.addMouseListener(new MouseListener {
            override def mouseClicked(e: MouseEvent): Unit = {}

            override def mousePressed(e: MouseEvent): Unit = {
                changeObject()
                pressed = true
            }

            override def mouseReleased(e: MouseEvent): Unit = {
                pressed = false
            }

            override def mouseEntered(e: MouseEvent): Unit = {
                if (pressed) {
                    changeObject()
                }
            }
            override def mouseExited(e: MouseEvent): Unit = {}
        })

        private def changeObject(): Unit = {
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

            if (paintCoordinates) {
                drawCenteredString(g, "(%d,%d)".format(coordinate.x, coordinate.y), new Rectangle(0, 0, size.width, size.height), new Font("Monospaced", Font.BOLD, 14))
            }
        }

        private def drawCenteredString (g: Graphics2D, text: String, rect: Rectangle, font: Font): Unit = {
            val metrics = g.getFontMetrics(font)
            val x = rect.x + (rect.width - metrics.stringWidth(text)) / 2
            val y = rect.y + ((rect.height - metrics.getHeight) / 2) + metrics.getAscent

            g.setFont(font)
            g.drawString(text, x, y)
        }
    }
}
