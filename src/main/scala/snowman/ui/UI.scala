package snowman.ui

import snowman.level.MutableLevel

import scala.swing.{Action, FileChooser, GridBagPanel, MainFrame, Menu, MenuBar, MenuItem, ScrollPane, SimpleSwingApplication}


object UI extends SimpleSwingApplication {

    private val resourcesPath = "/home/gerardmt/Documents/Snowman Editor/resources/"
    private val gamePath = "/home/gerardmt/.steam/steam/steamapps/common/A Good Snowman Is Hard To Build/resources/"
    private val backupPath = "/home/gerardmt/Documents/Snowman Editor/backup/"

    private val game = new Game(gamePath, backupPath)

    private val resourceManager = new ResourceManager(resourcesPath)

    private var uiLevel = UILevel.create(MutableLevel.default(10, 10), resourceManager)

    private val fileChooser = new FileChooser()

    override def top: MainFrame = new MainFrame {
        title = "Hello, World!"

        menuBar = new MenuBar {
            contents += new Menu("File") {
                contents += new MenuItem(Action("Open"){
                    val response = fileChooser.showOpenDialog(null)

                    if (response == FileChooser.Result.Approve) {
                        uiLevel.reload(MutableLevel.load(fileChooser.selectedFile))
                    }
                })
                contents += new MenuItem(Action("Save"){
                    val response = fileChooser.showSaveDialog(null)

                    if (response == FileChooser.Result.Approve) {
                        uiLevel.mutableLevel.save(fileChooser.selectedFile)
                    }
                })
            }
            contents += new Menu("Game") {
                contents += new MenuItem(Action("Play"){
                    game.play(uiLevel)
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

        val pane = new ScrollPane(new GridBagPanel {
            val c = new Constraints

            c.anchor = GridBagPanel.Anchor.NorthWest
            c.gridx = 0
            c.gridy = 0
            layout(uiLevel) = c

            c.gridx = 1
            c.gridy = 0
            layout(new Picker(resourceManager)) = c
        })

        contents = pane
        maximumSize = pane.preferredSize
    }
}