package gmt.gui

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.{GraphicsEnvironment, GridBagConstraints, GridBagLayout, Insets, Rectangle}
import java.io.{File, FileNotFoundException}

import gmt.main.Settings
import gmt.main.Settings.{SettingsNoSolverPathException, SettingsParseException}
import gmt.mod.Game
import gmt.mod.Game.RestoreException
import gmt.planner.planner.Planner.PlannerOptions
import gmt.snowman.encoder.EncoderBase.{EncoderEnum, EncoderOptions}
import gmt.snowman.level.MutableLevel
import gmt.snowman.level.MutableLevel.LevelParserValidateException
import gmt.snowman.pddl.EncoderPDDL
import gmt.snowman.planner.SnowmanSolver
import gmt.snowman.planner.SnowmanSolver.GenerateOptions
import gmt.terminal.Terminal
import gmt.util.Files
import javax.swing._

import scala.swing.GridBagPanel.Anchor
import scala.swing.{Action, Color, Dialog, Dimension, FileChooser, GridBagPanel, MainFrame, Menu, MenuBar, MenuItem, ScrollPane, Separator}

object GUI {

    val BACKGROUND_COLOR: Color = new Color(250, 250, 250)
    val TITLE = "Snowman Editor"
}

class GUI(private val settingsFile: File) {

    private val settings: Settings = try {
        Settings.load(settingsFile)
    } catch {
        case SettingsParseException(message) =>
            showErroDialog("Settings error: " + message)
            sys.exit()
        case _: FileNotFoundException =>
            showErroDialog("Settings file not found. File created at: " + settingsFile)
            sys.exit()
    }

    private val CURRENT_DIRECTORY = System.getProperty("user.dir")

    private val game = new Game(settings)

    private val resourceManager = new ResourceManager

    private val picker = new Picker(resourceManager)

    private val uiLevel = GUILevel.create(MutableLevel.default(7, 7), resourceManager, picker)

    private val fileChooser = new FileChooser()

    val top: MainFrame = new MainFrame {
        title = GUI.TITLE

        peer.setIconImage(resourceManager.getIcon)

        menuBar = new MenuBar {
            contents += new Menu("File") {
                contents += new MenuItem(Action("New"){
                    val widthField = new JTextField("5", 5)
                    val heightField = new JTextField("5", 5)

                    val panel = new JPanel
                    panel.add(new JLabel("width:"))
                    panel.add(widthField)
                    panel.add(javax.swing.Box.createHorizontalStrut(15))

                    panel.add(new JLabel("height:"))
                    panel.add(heightField)

                    val option = JOptionPane.showConfirmDialog(null, panel, "Map size", JOptionPane.OK_CANCEL_OPTION)

                    if (option == JOptionPane.OK_OPTION) {
                        try {
                            val width = widthField.getText.toInt
                            val heith = heightField.getText.toInt

                            setTitle("New")
                            uiLevel.reload(MutableLevel.default(width, heith))
                            resize()
                        } catch {
                            case _: NumberFormatException =>
                                showErroDialog("Number must be an integer")
                        }
                    }

                })
                contents += new Separator
                contents += new MenuItem(Action("Open") {
                    fileChooser.peer.setCurrentDirectory(new File(CURRENT_DIRECTORY))
                    val response = fileChooser.showOpenDialog(null)

                    if (response == FileChooser.Result.Approve) {
                        try {
                            setTitle(fileChooser.selectedFile.getName)
                            uiLevel.reload(MutableLevel.load(Files.openTextFile(fileChooser.selectedFile)))
                            resize()
                        } catch {
                            case _: FileNotFoundException =>
                                showAccessDeniedDialog()
                        }
                    }
                })
                contents += new MenuItem(Action("Save"){
                    savePickAndTextFile(uiLevel.mutableLevel.save, CURRENT_DIRECTORY + "/level.lvl")
                })
                contents += new Separator
                contents += new Menu("Load") {
                    if (settings.gamePath.isDefined) {
                        val levelsNames: List[String] = game.levelsNames

                        for (n <- levelsNames) {
                            contents += new MenuItem(Action(n) {
                                setTitle(n)
                                uiLevel.reload(MutableLevel.load(game.getLevel(n)))
                                resize()
                            })
                        }
                    } else {
                        contents += new MenuItem("Game path not defined")
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
                    try {
                        uiLevel.mutableLevel.validateException
                    } catch {
                        case e: LevelParserValidateException =>
                            showErrorValidateLevel(e, "Validator", Dialog.Message.Info)
                    }

                })
            }
            contents += new Menu("Game") {
                contents += new MenuItem(Action("Load and Run"){
                    if (settings.gamePath.isDefined && settings.savePath.isDefined) {
                        try {
                            if (game.gameHasOriginalFiles) {
                                game.backup()
                            }

                            game.loadAndRun(uiLevel.mutableLevel.toLevel)
                        } catch {
                            case _: FileNotFoundException =>
                                showAccessDeniedDialog()
                            case e: LevelParserValidateException =>
                                showErrorValidateLevel(e)
                        }
                    } else {
                        showErrorGamePathsNotDefined()
                    }
                })

                contents += new MenuItem(Action("Run") {
                    if (settings.gamePath.isDefined && settings.savePath.isDefined) {
                        game.run()
                    } else {
                        showErrorGamePathsNotDefined()
                    }
                })
                contents += new MenuItem(Action("Restore"){
                    if (settings.gamePath.isDefined && settings.savePath.isDefined) {
                        try {
                            game.restore()
                        } catch {
                            case _: RestoreException =>
                                showErroDialog("Restore files not found")
                        }
                    } else {
                        showErrorGamePathsNotDefined()
                    }
                })
            }
            contents += new Menu("Solver") {
                contents += new Menu("SMT Basic Encoding") {
                    contents += new MenuItem(Action("Generate") {
                        try {
                            showGenerateOptionsDialog() match {
                                case Some((g, e)) =>
                                    savePickAndTextFile(SnowmanSolver.generateSMTLIB2(uiLevel.mutableLevel.toLevel, EncoderEnum.BASIC, e, g), CURRENT_DIRECTORY + "/in_basic-encoding.smtlib2")
                                case None =>
                            }
                        } catch {
                            case e: LevelParserValidateException =>
                                showErrorValidateLevel(e)
                        }
                    })
                    contents += new MenuItem(Action("Solve") {
                        try {
                            showSolverOptionsDialog() match {
                                case Some((p, e)) =>
                                    Terminal.showResult(SnowmanSolver.solveSMTYics2(settings.solverPath.get, uiLevel.mutableLevel.toLevel, EncoderEnum.BASIC, e, p, Terminal.showSolverUpdate))
                                case None =>
                            }
                        } catch {
                            case _: SettingsNoSolverPathException =>
                                showErrorSolverPathNotDeined()
                            case e: LevelParserValidateException =>
                                showErrorValidateLevel(e)
                        }
                    })
                }
                contents += new Menu("SMT Cheating Encoding") {
                    contents += new MenuItem(Action("Generate") {
                        try {
                            showGenerateOptionsDialog() match {
                                case Some((g, e)) =>
                                    savePickAndTextFile(SnowmanSolver.generateSMTLIB2(uiLevel.mutableLevel.toLevel, EncoderEnum.CHEATING, e, g), CURRENT_DIRECTORY + "/in_cheating-encoding.smtlib2")
                                case None =>
                            }
                        } catch {
                            case e: LevelParserValidateException =>
                                showErrorValidateLevel(e)
                        }
                    })

                    contents += new MenuItem(Action("Solve") {
                        try {
                            showSolverOptionsDialog() match {
                                case Some((p, e)) =>
                                    Terminal.showResult(SnowmanSolver.solveSMTYics2(settings.solverPath.get, uiLevel.mutableLevel.toLevel, EncoderEnum.CHEATING, e, p, Terminal.showSolverUpdate))
                                case None =>
                            }
                        } catch {
                            case _: SettingsNoSolverPathException =>
                                showErrorSolverPathNotDeined()
                            case e: LevelParserValidateException =>
                                showErrorValidateLevel(e)
                        }
                    })
                }
                contents += new Menu("SMT Reachability Encoding") {
                    contents += new MenuItem(Action("Generate") {
                        try {
                            showGenerateOptionsDialog() match {
                                case Some((g, e)) =>
                                    savePickAndTextFile(SnowmanSolver.generateSMTLIB2(uiLevel.mutableLevel.toLevel, EncoderEnum.REACHABILITY, e, g), CURRENT_DIRECTORY + "/in_reachability-encoding.smtlib2")
                                case None =>
                            }
                        } catch {
                            case e: LevelParserValidateException =>
                                showErrorValidateLevel(e)
                        }
                    })
                    contents += new MenuItem(Action("Solve") {
                        try {
                            showSolverOptionsDialog() match {
                                case Some((p, e)) =>
                                    Terminal.showResult(SnowmanSolver.solveSMTYics2(settings.solverPath.get, uiLevel.mutableLevel.toLevel, EncoderEnum.REACHABILITY, e, p, Terminal.showSolverUpdate))
                                case None =>
                            }
                        } catch {
                            case _: SettingsNoSolverPathException =>
                                showErrorSolverPathNotDeined()
                            case e: LevelParserValidateException =>
                                showErrorValidateLevel(e)
                        }
                    })
                }
                contents += new Separator
                contents += new MenuItem(Action("Generate PDDL adl") {
                    try {
                        savePickAndTextFile(EncoderPDDL.encodeAdl(uiLevel.mutableLevel.toLevel), CURRENT_DIRECTORY + "/problem_adl.pddl")
                    } catch {
                        case e: LevelParserValidateException =>
                            showErrorValidateLevel(e)
                    }
                })
                contents += new MenuItem(Action("Generate PDDL adl Grounded") {
                    try {
                        val (domain, problem) = EncoderPDDL.encodeAdlGrounded(uiLevel.mutableLevel.toLevel)

                        val directoryChooser = new FileChooser
                        directoryChooser.fileSelectionMode_=(FileChooser.SelectionMode.DirectoriesOnly)
                        directoryChooser.peer.setCurrentDirectory(new File(CURRENT_DIRECTORY))

                        val result = directoryChooser.showDialog(null, "Select")

                        if (result == FileChooser.Result.Approve) {
                            Files.saveTextFile(new File(directoryChooser.selectedFile.getAbsolutePath + "/domain.pddl"), domain)
                            Files.saveTextFile(new File(directoryChooser.selectedFile.getAbsolutePath + "/problem.pddl"), problem)
                        }
                    } catch {
                        case e: LevelParserValidateException =>
                            showErrorValidateLevel(e)
                    }
                })
                contents += new MenuItem(Action("Generate PDDL object-fluents") {
                    try {
                        savePickAndTextFile(EncoderPDDL.encodeObjectFluents(uiLevel.mutableLevel.toLevel), CURRENT_DIRECTORY + "/problem_object-fluents.pddl")
                    } catch {
                        case e: LevelParserValidateException =>
                            showErrorValidateLevel(e)
                    }
                })
            }
            contents += new Menu("Editor") {
                val coordinatesCheckBox = new JCheckBoxMenuItem("Coordinates")
                //noinspection ConvertExpressionToSAM
                coordinatesCheckBox.addActionListener(new ActionListener {
                    override def actionPerformed(e: ActionEvent): Unit = uiLevel.showCoordinates_(e.getSource.asInstanceOf[AbstractButton].getModel.isSelected)
                })
                peer.add(coordinatesCheckBox)
                val mateuCheckBox = new JCheckBoxMenuItem("Mateu mode")
                //noinspection ConvertExpressionToSAM
                mateuCheckBox.addActionListener(new ActionListener {
                    override def actionPerformed(e: ActionEvent): Unit = {
                        resourceManager.mateuMode_(e.getSource.asInstanceOf[AbstractButton].getModel.isSelected)
                        uiLevel.repaint()
                        picker.repaint()
                    }
                })
                peer.add(mateuCheckBox)
            }
        }

        contents = new ScrollPane(new GridBagPanel {
            val c = new Constraints

            background = GUI.BACKGROUND_COLOR

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

        def setTitle(levelName: String): Unit = {
            title_=(GUI.TITLE + " - " + levelName)
        }
    }

    if (top.size == new Dimension(0,0)) {
        top.pack()
    }

    top.visible = true


    private def savePickAndTextFile(string: String, fileName: String): Unit = {
        fileChooser.selectedFile_=(new File(fileName))
        val response = fileChooser.showSaveDialog(null)

        if (response == FileChooser.Result.Approve) {
            try {
                if (fileChooser.selectedFile.exists()) {
                    val result = JOptionPane.showConfirmDialog(null, "Text already exists. Overwrite?", "Overwrite", JOptionPane.YES_NO_OPTION)

                    if (result == JOptionPane.YES_OPTION) {
                        fileChooser.selectedFile.delete()

                        Files.saveTextFile(fileChooser.selectedFile, string)
                    }
                } else {
                    Files.saveTextFile(fileChooser.selectedFile, string)
                }
            } catch {
                case _: FileNotFoundException =>
                    showAccessDeniedDialog()
            }
        }
    }

    private def showErrorValidateLevel(exception: LevelParserValidateException, title: String = "Error", messageType: Dialog.Message.Value = Dialog.Message.Warning): Unit = {
        val characterMessate = if (exception.noCharacter) {
            "Level can and only has to have one character"
        } else {
            ""
        }

        val ballsMessage = if (exception.noMultipleBalls) {
            "Number of balls has to be multiple of 3"
        } else {
            ""
        }

        val message = characterMessate + "\n" + ballsMessage

        if (title == "") {
            showErroDialog(message)
        } else {
            Dialog.showMessage(null, message, title, messageType)
        }
    }

    private def showErroDialog(message: String): Unit = {
        Dialog.showMessage(null, message, "Error", Dialog.Message.Error)
    }

    private def showAccessDeniedDialog(): Unit = {
        showErroDialog("Access Denied")
    }


    private def showErrorSolverPathNotDeined(): Unit = {
        showErroDialog("Solver path not defined in settings")
    }

    private def showErrorGamePathsNotDefined(): Unit = {
        showErroDialog("Game path and/or solver path not defined in settings")
    }


    private class InvariantsLayout {

        private val ballSizesCheckBox = new JCheckBox("Ball sizes")
        private val ballPositionsCheckBox = new JCheckBox("Ball positions")

        def component: JComponent = {
            val panel = new JPanel
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS))
            panel.setBorder(BorderFactory.createTitledBorder("Invariants"))
            panel.add(ballSizesCheckBox)
            panel.add(ballPositionsCheckBox)

            panel
        }

        def result: EncoderOptions = {
            EncoderOptions(ballSizesCheckBox.isSelected, ballPositionsCheckBox.isSelected)
        }
    }

    private class OptionsLayout {

        private val AUTO = "auto"

        private val startTimeStepTextField = new JTextField(AUTO, 10)
        private val maxTimeStepsTextField = new JTextField("100", 10)

        def component: JComponent = {
            val startTimeStepLabel = new JLabel("Start time step:")
            val maxTimeStepsLabel = new JLabel("Max time steps:")

            val panel = new JPanel(new GridBagLayout)
            panel.setBorder(BorderFactory.createTitledBorder("Options"))
            val c = new GridBagConstraints()

            val RIGHT_PADDING = 5
            val BOTTOM_PADDING = 5

            c.anchor = GridBagConstraints.EAST
            c.gridx = 0
            c.gridy = 0
            c.insets = new Insets(0, 0, BOTTOM_PADDING, RIGHT_PADDING)
            panel.add(startTimeStepLabel, c)

            c.gridx = 1
            c.gridy = 0
            c.insets = new Insets(0, 0, BOTTOM_PADDING, 0)
            panel.add(startTimeStepTextField, c)

            c.gridx = 0
            c.gridy = 1
            c.insets = new Insets(0, 0, BOTTOM_PADDING, RIGHT_PADDING)
            panel.add(maxTimeStepsLabel, c)

            c.gridx = 1
            c.gridy = 1
            c.insets = new Insets(0, 0, BOTTOM_PADDING, 0)
            panel.add(maxTimeStepsTextField, c)

            panel
        }

        def result: PlannerOptions = {
            val startTimeSteps: Option[Int] = if (startTimeStepTextField.getText == AUTO) {
                None
            } else {
                Some(startTimeStepTextField.getText.toInt)
            }

            PlannerOptions(startTimeSteps,
                maxTimeStepsTextField.getText.toInt)
        }
    }

    private def showGenerateOptionsDialog(): Option[(GenerateOptions, EncoderOptions)] = {
        val invariantsLayout = new InvariantsLayout

        val timeStepTextField = new JTextField("1", 5)

        val timeStepLabel = new JLabel("Time Step:")

        val timeStepPanel = new JPanel(new GridBagLayout)
        timeStepPanel.setBorder(BorderFactory.createTitledBorder("Options"))
        val timeStepC = new GridBagConstraints()

        val RIGHT_PADDING = 5

        timeStepC.anchor = GridBagConstraints.EAST
        timeStepC.gridx = 0
        timeStepC.gridy = 0
        timeStepC.insets = new Insets(0, 0, 0, RIGHT_PADDING)
        timeStepPanel.add(timeStepLabel, timeStepC)

        timeStepC.gridx = 1
        timeStepC.gridy = 0
        timeStepC.insets = new Insets(0, 0, 0, 0)
        timeStepPanel.add(timeStepTextField, timeStepC)

        val panel = new JPanel(new GridBagLayout)
        val c = new GridBagConstraints
        c.anchor = GridBagConstraints.NORTH
        c.gridx = 0
        c.gridy = 0
        panel.add(timeStepPanel, c)
        c.gridx = 1
        c.gridy = 0
        panel.add(invariantsLayout.component, c)

        val option = JOptionPane.showConfirmDialog(null, panel, "Generate options", JOptionPane.OK_CANCEL_OPTION)

        if (option == JOptionPane.OK_OPTION) {
            try {
                val generateOptions = GenerateOptions(timeStepTextField.getText.toInt)

                Some((generateOptions, invariantsLayout.result))
            } catch {
                case _: NumberFormatException =>
                    showErroDialog("Number must be an integer")
                    None
            }

        } else {
            None
        }
    }

    private def showSolverOptionsDialog(): Option[(PlannerOptions, EncoderOptions)] = {
        val optionsLayout = new OptionsLayout
        val invariantsLayout = new InvariantsLayout

        val panel = new JPanel(new GridBagLayout)
        val c = new GridBagConstraints
        c.anchor = GridBagConstraints.NORTH
        c.gridx = 0
        c.gridy = 0
        panel.add(optionsLayout.component, c)
        c.gridx = 1
        c.gridy = 0
        panel.add(invariantsLayout.component, c)

        val option = JOptionPane.showConfirmDialog(null, panel, "Solver options", JOptionPane.OK_CANCEL_OPTION)

        if (option == JOptionPane.OK_OPTION) {
            try {
                Some((optionsLayout.result, invariantsLayout.result))
            } catch {
                case _: NumberFormatException =>
                    showErroDialog("Number must be an integer")
                    None
            }

        } else {
            None
        }
    }
}