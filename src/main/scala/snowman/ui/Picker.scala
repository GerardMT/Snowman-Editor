package snowman.ui

import snowman.level

import scala.swing.Swing._
import scala.swing.event.MousePressed
import scala.swing.{BoxPanel, Graphics2D, GridPanel, Label, Orientation, Panel}

class Picker(resourceManager: ResourceManager) extends BoxPanel(Orientation.Vertical) {

    private var _currentPicker: Option[PickerObject] = None

    border = EmptyBorder(10, 10, 10, 10)

    contents += new Label("Picker")
    contents += new GridPanel((snowman.level.`object`.Object.ALL_OBJECTS.length / 2.0f).ceil.toInt, 2) {
        for (o <- snowman.level.`object`.Object.ALL_OBJECTS) {
            contents += new PickerObject(o)
        }
    }

    def current: Option[snowman.level.`object`.Object] = {
        _currentPicker match {
            case Some(p) =>
                Some(p.o)
            case None =>
                None
        }
    }

    private def setCurrent(pickerObject: PickerObject): Unit = {
        _currentPicker match {
            case Some(p) =>
                p.unselect()
            case None =>
        }

        _currentPicker = Some(pickerObject)
    }

    object PickerObject {

        val TILE_WIDTH = 50
        val TILE_HEIGHT = 50
    }

    class PickerObject(val o: level.`object`.Object) extends Panel {

        private var selected = false

        def unselect(): Unit = {
            selected = false
            repaint()
        }

        preferredSize = (PickerObject.TILE_WIDTH, PickerObject.TILE_HEIGHT)

        focusable = true
        listenTo(mouse.clicks)

        reactions += {
            case e: MousePressed  =>
                setCurrent(this)
                selected = true
                repaint()
        }

        override def paintComponent(g: Graphics2D): Unit = {
            super.paintComponent(g)
            g.drawImage(resourceManager.getResource(o), 0, 0, size.width, size.height,null)

            if (selected) {
                drawCenteredCircle(g, size.width / 2, size.height / 2, size.width / 2)
            }
        }

        def drawCenteredCircle(g: Graphics2D, x: Int, y: Int, r: Int): Unit = {
            val x1 = x - (r / 2)
            val y1 = y - (r / 2)
            g.fillOval(x1, y1, r, r)
        }
    }
}
