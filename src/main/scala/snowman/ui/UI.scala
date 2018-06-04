package snowman.ui

import java.awt.{GraphicsEnvironment, Rectangle}
import java.io.File

import javax.swing.{Box => _, _}
import snowman.level.{LevelParserException, MutableLevel}

import scala.swing.GridBagPanel.Anchor
import scala.swing.{Action, Dialog, FileChooser, GridBagPanel, MainFrame, Menu, MenuBar, MenuItem, ScrollPane, Separator, SimpleSwingApplication}


object UI extends SimpleSwingApplication {

    private val resourcesPath = "/home/gerardmt/Documents/Snowman Editor/resources/"

    private val settingsFile = new File("./snowmanEditor.config")
    private val settings = Settings.read(settingsFile)

    private val game = new Game(settings)

    if (settings.firstRun) {
        game.backup()
        settings.firstRun = false
    }

    private val resourceManager = new ResourceManager(resourcesPath)

    private val picker = new Picker(resourceManager)

    private val uiLevel = UILevel.create(MutableLevel.default(7, 7), resourceManager, picker)

    private val fileChooser = new FileChooser()

    override def top: MainFrame = new MainFrame {
        title = "Snowman Editor"

        menuBar = new MenuBar {
            contents += new Menu("File") {
                contents += new MenuItem(Action("New"){
                    val widthField = new JTextField(5)
                    val heightField = new JTextField(5)

                    val myPanel = new JPanel
                    myPanel.add(new JLabel("width:"))
                    myPanel.add(widthField)
                    myPanel.add(javax.swing.Box.createHorizontalStrut(15)) // a spacer

                    myPanel.add(new JLabel("height:"))
                    myPanel.add(heightField)

                    val result = JOptionPane.showConfirmDialog(null, myPanel, "Map size", JOptionPane.OK_CANCEL_OPTION)

                    try {
                        val width = widthField.getText.toInt
                        val heith = heightField.getText.toInt

                        uiLevel.reload(MutableLevel.default(width, heith))
                        resize()
                    } catch {
                        case e: NumberFormatException =>
                            Dialog.showMessage(null, "Number must be an integer")
                    }

                })
                contents += new Separator
                contents += new MenuItem(Action("Open"){
                    val response = fileChooser.showOpenDialog(null)

                    if (response == FileChooser.Result.Approve) {
                        uiLevel.reload(MutableLevel.load(fileChooser.selectedFile))
                    }
                    resize()
                })
                contents += new MenuItem(Action("Save"){
                    val response = fileChooser.showSaveDialog(null)

                    if (response == FileChooser.Result.Approve) {
                        uiLevel.mutableLevel.save(fileChooser.selectedFile)
                    }
                })
                contents += new Separator
                contents += new Menu("Load") {
                    for (n <- game.levels) {
                        contents += new MenuItem(Action(n){
                            uiLevel.reload(MutableLevel.load(game.getLevel(n)))
                            resize()
                        })
                    }
                }
            }
            contents += new Menu("Game") {
                contents += new MenuItem(Action("Play"){
                    try {
                        game.play(uiLevel.mutableLevel.toLevel)
                    } catch {
                        case e: LevelParserException =>
                            Dialog.showMessage(null, e.message)
                    }
                })
                contents += new MenuItem(Action("Restore"){
                    game.restore()
                })
            }
            contents += new Menu("Solver") {
                contents += new MenuItem("Solve (SMT)")
                contents += new MenuItem("Solve (SMT TP)")
                contents += new MenuItem("Solve (SMT TP Reachability)")
            }
        }

        override def closeOperation(): Unit = {
            settings.save(settingsFile)
            super.closeOperation()
        }

        contents = new ScrollPane(new GridBagPanel {
            val c = new Constraints

            c.anchor = Anchor.NorthWest
            c.gridx = 0
            c.gridy = 0
            layout(uiLevel) = c

            c.anchor = Anchor.NorthWest
            c.gridx = 1
            c.gridy = 0
            layout(picker) = c
        })

        peer.setResizable(false)

        def resize(): Unit = {
            pack()

            val maxRectangle = GraphicsEnvironment.getLocalGraphicsEnvironment.getMaximumWindowBounds

            val width = Math.min(maxRectangle.width, peer.getSize.width)
            val height = Math.min(maxRectangle.height, peer.getSize.height)

            peer.setBounds(new Rectangle(maxRectangle.x, maxRectangle.y, width, height))
        }
    }
}