package gmt.ui

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.{GraphicsEnvironment, Rectangle}
import java.beans.PropertyChangeListener
import java.io.File

import gmt.snowman.level.MutableLevel
import gmt.snowman.level.`object`.Wall
import gmt.snowman.pddl.EncoderPDDL
import gmt.snowman.solver.{SnowmanSolver, SnowmanSolverResult}
import gmt.snowman.util.Files
import javax.swing.{Box => _, _}

import scala.swing.GridBagPanel.Anchor
import scala.swing.{Action, Dialog, FileChooser, GridBagPanel, MainFrame, Menu, MenuBar, MenuItem, ScrollPane, Separator, SimpleSwingApplication}


object UI extends SimpleSwingApplication {

    private val settingsFile = new File("./snowman_editor.config")
    private val settings = Settings.read(settingsFile)

    private val game = new Game(settings)

    private val resourceManager = new ResourceManager

    private val picker = new Picker(resourceManager)

    private val uiLevel = UILevel.create(MutableLevel.default(7, 7), resourceManager, picker)

    private val fileChooser = new FileChooser()

    override def top: MainFrame = new MainFrame {
        title = "Snowman Editor"

        peer.setIconImage(resourceManager.getResource(Wall))

        menuBar = new MenuBar {
            contents += new Menu("File") {
                contents += new MenuItem(Action("New"){
                    val widthField = new JTextField(5)
                    val heightField = new JTextField(5)

                    val panel = new JPanel
                    panel.add(new JLabel("width:"))
                    panel.add(widthField)
                    panel.add(javax.swing.Box.createHorizontalStrut(15)) // a spacer

                    panel.add(new JLabel("height:"))
                    panel.add(heightField)

                    val option = JOptionPane.showConfirmDialog(null, panel, "Map size", JOptionPane.OK_CANCEL_OPTION)

                    if (option == JOptionPane.OK_OPTION) {
                        try {
                            val width = widthField.getText.toInt
                            val heith = heightField.getText.toInt

                            uiLevel.reload(MutableLevel.default(width, heith))
                            resize()
                        } catch {
                            case _: NumberFormatException =>
                                Dialog.showMessage(null, "Number must be an integer", "Error", Dialog.Message.Error)
                        }
                    }

                })
                contents += new Separator
                contents += new MenuItem(Action("Open"){
                    val response = fileChooser.showOpenDialog(null)

                    if (response == FileChooser.Result.Approve) {
                        uiLevel.reload(MutableLevel.load(Files.openTextFile(fileChooser.selectedFile)))
                    }
                    resize()
                })
                contents += new MenuItem(Action("Save"){
                    savePickAndTextFile(uiLevel.mutableLevel.save, "level.lvl")
                })
                contents += new Separator
                contents += new Menu("Load") {
                    val levelsNames: List[String] = game.levelsNames

                    if (levelsNames.isEmpty) {
                        contents += new MenuItem("Game levels file not found")
                    } else {
                        for (n <- levelsNames) {
                            contents += new MenuItem(Action(n){
                                uiLevel.reload(MutableLevel.load(game.getLevel(n)))
                                resize()
                            })
                        }
                    }
                }
            }
            contents += new Menu("Level") {
                contents += new MenuItem(Action("Info") {
                    val info = uiLevel.mutableLevel.info

                    val stringBuilder = StringBuilder.newBuilder

                    stringBuilder.append("Size: " + info.size + "\n")
                    stringBuilder.append("Width: " + info.width + "\n")
                    stringBuilder.append("Height: " + info.height + "\n")
                    stringBuilder.append("PlayableArea: " + info.playableArea + "\n")
                    stringBuilder.append("Balls: " + info.balls + "\n")

                    val (o, i) = info.objects.head
                    stringBuilder.append("Tile (%s): %d".format(o.getClass.getSimpleName , i))
                    for ((o, i) <- info.objects.tail) {
                        stringBuilder.append("\nTile (%s): %d".format(o.getClass.getSimpleName , i))
                    }

                    Dialog.showMessage(null, stringBuilder.toString, "Info")
                })
                contents += new MenuItem(Action("Validate") {
                    if(validateLevelShowDialog(uiLevel.mutableLevel, "Validator", Dialog.Message.Info)) {
                        Dialog.showMessage(null, "Level is corret", "Validator", Dialog.Message.Info)
                    }
                })
                val checkBox = new JCheckBoxMenuItem("Coordinates")
                checkBox.addActionListener(new ActionListener {
                    override def actionPerformed(e: ActionEvent): Unit = uiLevel.showCoordinates_(e.getSource.asInstanceOf[AbstractButton].getModel.isSelected)
                })
                peer.add(checkBox)
            }
            contents += new Menu("Game") {
                contents += new MenuItem(Action("Load and Run"){
                    if (game.gameHasOriginalFiles) {
                        game.backup()
                    }

                    if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                        game.loadAndPlay(uiLevel.mutableLevel.toLevel)
                    }
                })
                contents += new MenuItem(Action("Run") {
                    game.run()
                })
                contents += new MenuItem(Action("Restore"){
                    game.restore()
                })
            }
            contents += new Menu("Solver") {
                contents += new Menu("SMT Basic Encoding") {
                    contents += new MenuItem(Action("Generate") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            generateSMT(f => savePickAndTextFile(SnowmanSolver.encodeBasicEncoding(uiLevel.mutableLevel.toLevel, f), "in_basic-encoding.smtlib2"))
                        }
                    })
                    contents += new MenuItem(Action("Solve") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            showResult(SnowmanSolver.solveBasicEncoding(settings, uiLevel.mutableLevel.toLevel))
                        }
                    })
                }
                contents += new Menu("SMT Cheating Encoding") {
                    contents += new MenuItem(Action("Generate") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            generateSMT(f => savePickAndTextFile(SnowmanSolver.encodeCheatingEncoding(uiLevel.mutableLevel.toLevel, f), "in_cheating-encoding.smtlib2"))
                        }
                    })
                    contents += new MenuItem(Action("Solve") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            showResult(SnowmanSolver.solveCheatingEncoding(settings, uiLevel.mutableLevel.toLevel))
                        }
                    })
                }
                contents += new Menu("SMT Reachability Encoding") {
                    contents += new MenuItem(Action("Generate") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            generateSMT(f => savePickAndTextFile(SnowmanSolver.encodeReachabilityEncoding(uiLevel.mutableLevel.toLevel, f), "in_reachability-encoding.smtlib2"))
                        }
                    })
                    contents += new MenuItem(Action("Solve") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            showResult(SnowmanSolver.solveReachabilityEncoding(settings, uiLevel.mutableLevel.toLevel))
                        }
                    })
                }
                contents += new Separator
                contents += new Menu("PDDL adl") {
                    contents += new MenuItem(Action("Generate") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            savePickAndTextFile(EncoderPDDL.encodeStrips(uiLevel.mutableLevel.toLevel), "domain_adl.pddl")
                        }
                    })
                    contents += new MenuItem(Action("Solve") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            SnowmanSolver.solvePDDLStrips(uiLevel.mutableLevel.toLevel)
                        }
                    })
                }
                contents += new Menu("PDDL object-fluents") {
                    contents += new MenuItem(Action("Generate") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            savePickAndTextFile(EncoderPDDL.encodeObjectFluents(uiLevel.mutableLevel.toLevel), "domain_object-fluents.pddl")
                        }
                    })
                    contents += new MenuItem(Action("Solve") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            SnowmanSolver.solvePDDLObjectFluents(uiLevel.mutableLevel.toLevel)
                        }
                    })
                }
                contents += new Menu("PDDL numeric-fluents") {
                    contents += new MenuItem(Action("Generate") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            savePickAndTextFile(EncoderPDDL.encodeNumericFluents(uiLevel.mutableLevel.toLevel), "domain_numeric-fluents.pddl")
                        }
                    })
                    contents += new MenuItem(Action("Solve") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            SnowmanSolver.solvePDDLNumericFluents(uiLevel.mutableLevel.toLevel)
                        }
                    })
                }

            }
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
        centerOnScreen()

        def resize(): Unit = {
            val location = peer.getLocation
            pack()

            val maxRectangle = GraphicsEnvironment.getLocalGraphicsEnvironment.getMaximumWindowBounds

            val width = Math.min(maxRectangle.width, peer.getSize.width)
            val height = Math.min(maxRectangle.height, peer.getSize.height)

            val x = if (location.x + width > maxRectangle.width) {
                maxRectangle.width - width
            } else {
                location.x
            }

            val y = if (location.y + height > maxRectangle.height) {
                maxRectangle.height - height
            } else {
                location.y
            }

            peer.setBounds(new Rectangle(x, y, width, height))
        }
    }

    private def showResult(snowmanSolverResult: SnowmanSolverResult): Unit = { // TODO UI
        println("Solved: " + snowmanSolverResult.solved)

        snowmanSolverResult.result match {
            case Some(r) =>
                println("Valid: " + snowmanSolverResult.valid)
                println("Actions:" + r.actions.size)
                r.actions.foreach(f => println("    " + f.toString))
            case None =>
        }
    }

    private def savePickAndTextFile(string: String, fileName: String): Unit = {
        fileChooser.selectedFile_=(new File(fileName))
        val response = fileChooser.showSaveDialog(null)

        if (response == FileChooser.Result.Approve) {
            if (fileChooser.selectedFile.exists()) {
                val result = JOptionPane.showConfirmDialog(null, "Text already exists. Overwrite?", "Overwrite", JOptionPane.YES_NO_OPTION)

                if (result == JOptionPane.YES_OPTION) {
                    fileChooser.selectedFile.delete()

                    Files.saveTextFile(fileChooser.selectedFile, string)
                }
            } else {
                Files.saveTextFile(fileChooser.selectedFile, string)
            }
        }
    }

    private def textDialog(dialogText: String, labelText: String): (Int, JTextField) = {
        val textField = new JTextField(5)

        val panel = new JPanel
        panel.add(new JLabel(labelText))
        panel.add(textField)

        (JOptionPane.showConfirmDialog(null, panel, dialogText, JOptionPane.OK_CANCEL_OPTION), textField)
    }

    private def generateSMT(f: Int => Unit): Unit = {
        val (result, textField) = textDialog("Generate", "Time Steps: ")

        if (result == JOptionPane.OK_OPTION) {
            try {
                f(textField.getText.toInt)
            } catch {
                case e: NumberFormatException =>
                    Dialog.showMessage(null, "Number must be an integer", "Error", Dialog.Message.Error)
            }
        }
    }

    private def validateLevelShowDialog(mutableLevel: MutableLevel, title: String = "Error", messageType: Dialog.Message.Value = Dialog.Message.Error): Boolean = {
        val (characterValid, ballsValid) = uiLevel.mutableLevel.validate
        if (!characterValid || !ballsValid) {
            val characterMessate = if (!characterValid) {
                "Level can and only has to have one character"
            } else {
                ""
            }

            val ballsMessage = if (!ballsValid) {
                "Number of balls has to be multiple of 3"
            } else {
                ""
            }

            Dialog.showMessage(null, characterMessate + "\n" + ballsMessage, title, messageType)

            false
        } else {
            true
        }
    }
}