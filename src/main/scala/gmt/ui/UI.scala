package gmt.ui

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.{GraphicsEnvironment, GridBagConstraints, GridBagLayout, Insets, Rectangle}
import java.io.{File, FileNotFoundException}

import gmt.planner.planner.Planner.{PlannerOptions, PlannerUpdate}
import gmt.snowman.encoder.DecodingData
import gmt.snowman.encoder.EncoderBase.EncoderOptions
import gmt.snowman.level.MutableLevel
import gmt.snowman.pddl.EncoderPDDL
import gmt.snowman.solver.SnowmanSolver.GenerateOptions
import gmt.snowman.solver.{SnowmanSolver, SnowmanSolverResult}
import gmt.snowman.util.Files
import javax.swing._

import scala.swing.GridBagPanel.Anchor
import scala.swing.{Action, Color, Dialog, FileChooser, GridBagPanel, MainFrame, Menu, MenuBar, MenuItem, ScrollPane, Separator, SimpleSwingApplication}


object UI extends SimpleSwingApplication {

    val BACKGROUND_COLOR: Color = new Color(167, 191, 150)

    private val settingsFile = new File("./snowman_editor.config")

    private var optionSettings: Option[Settings] =  None

    try {
        if (!settingsFile.exists()) {
            showErroDialog("Settings file not found. File created at: " + settingsFile)

            val default = Settings.default

            default.save(settingsFile)
            optionSettings = Some(default)
        } else {
            optionSettings = Some(Settings.read(settingsFile))
        }
    } catch {
        case SettingsParseException(message) =>
            showErroDialog("Settings error: " + message)
            System.exit(0)
        case _: FileNotFoundException =>
            showAccessDeniedDialog()
            System.exit(0)
    }

    private val settings = optionSettings.get

    private val game = new Game(settings)

    private val resourceManager = new ResourceManager

    private val picker = new Picker(resourceManager)

    private val uiLevel = UILevel.create(MutableLevel.default(7, 7), resourceManager, picker)

    private val fileChooser = new FileChooser()

    override def top: MainFrame = new MainFrame {
        title = "Snowman Editor"

        peer.setIconImage(resourceManager.getIcon)

        menuBar = new MenuBar {
            contents += new Menu("File") {
                contents += new MenuItem(Action("New"){
                    val widthField = new JTextField(5)
                    val heightField = new JTextField(5)

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

                            uiLevel.reload(MutableLevel.default(width, heith))
                            resize()
                        } catch {
                            case _: NumberFormatException =>
                                showErroDialog("Number must be an integer")
                        }
                    }

                })
                contents += new Separator
                contents += new MenuItem(Action("Open"){
                    val response = fileChooser.showOpenDialog(null)

                    if (response == FileChooser.Result.Approve) {
                        try {
                            uiLevel.reload(MutableLevel.load(Files.openTextFile(fileChooser.selectedFile)))
                            resize()
                        } catch {
                            case _: FileNotFoundException =>
                                showAccessDeniedDialog()
                        }
                    }
                })
                contents += new MenuItem(Action("Save"){
                    savePickAndTextFile(uiLevel.mutableLevel.save, "level.lvl")
                })
                contents += new Separator
                contents += new Menu("Load") {
                    settings.gamePath match {
                        case Some(s) =>
                            val levelsNames: List[String] = game.levelsNames

                            for (n <- levelsNames) {
                                contents += new MenuItem(Action(n){
                                    uiLevel.reload(MutableLevel.load(game.getLevel(n)))
                                    resize()
                                })
                            }
                        case None =>
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
                    if(validateLevelShowDialog(uiLevel.mutableLevel, "Validator", Dialog.Message.Info)) {
                        Dialog.showMessage(null, "Level is corret", "Validator", Dialog.Message.Info)
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

                            if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                                game.loadAndRun(uiLevel.mutableLevel.toLevel)
                            }
                        } catch {
                            case _: FileNotFoundException =>
                                showAccessDeniedDialog()
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
                            case _: FileNotFoundException =>
                                showAccessDeniedDialog()
                        }
                    } else {
                        showErrorGamePathsNotDefined()
                    }
                })
            }
            contents += new Menu("Solver") {
                contents += new Menu("SMT Basic Encoding") {
                    contents += new MenuItem(Action("Generate") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            showGenerateOptionsDialog() match {
                                case Some((g, e)) =>
                                    savePickAndTextFile(SnowmanSolver.generateBasicEncoding(uiLevel.mutableLevel.toLevel, e, g), "in_basic-encoding.smtlib2")
                                case None =>
                            }
                        }
                    })
                    contents += new MenuItem(Action("Solve") {
                        settings.solverPath match {
                            case Some(s) =>
                                if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                                    showSolverOptionsDialog() match {
                                        case Some((p, e)) =>
                                            showResult(SnowmanSolver.solveBasicEncoding(s, uiLevel.mutableLevel.toLevel, e, p, showSolverUpdate))
                                        case None =>
                                    }
                                }
                            case None =>
                                showErrorSolverPathNotDeined()
                        }
                    })
                }
                contents += new Menu("SMT Cheating Encoding") {
                    contents += new MenuItem(Action("Generate") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            showGenerateOptionsDialog() match {
                                case Some((g, e)) =>
                                    savePickAndTextFile(SnowmanSolver.generateCheatingEncoding(uiLevel.mutableLevel.toLevel, e, g), "in_cheating-encoding.smtlib2")
                                case None =>
                            }
                        }
                    })

                    contents += new MenuItem(Action("Solve") {
                        settings.solverPath match {
                            case Some(s) =>
                                if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                                    showSolverOptionsDialog() match {
                                        case Some((p, e)) =>
                                            showResult(SnowmanSolver.solveCheatingEncoding(s, uiLevel.mutableLevel.toLevel, e, p, showSolverUpdate))
                                        case None =>
                                    }
                                }
                            case None =>
                                showErrorSolverPathNotDeined()
                        }
                    })
                }
                contents += new Menu("SMT Reachability Encoding") {
                    contents += new MenuItem(Action("Generate") {
                        if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                            showGenerateOptionsDialog() match {
                                case Some((g, e)) =>
                                    savePickAndTextFile(SnowmanSolver.generateReachabilityEncoding(uiLevel.mutableLevel.toLevel, e, g), "in_reachability-encoding.smtlib2")
                                case None =>
                            }
                        }
                    })
                    contents += new MenuItem(Action("Solve") {
                        settings.solverPath match {
                            case Some(s) =>
                                if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                                    showSolverOptionsDialog() match {
                                        case Some((p, e)) =>
                                            showResult(SnowmanSolver.solveReachabilityEncoding(s, uiLevel.mutableLevel.toLevel, e, p, showSolverUpdate))
                                        case None =>
                                    }
                                }
                            case None =>
                                showErrorSolverPathNotDeined()
                        }
                    })
                }
                contents += new Separator
                contents += new MenuItem(Action("Generate PDDL adl") {
                    if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                        savePickAndTextFile(EncoderPDDL.encodeStrips(uiLevel.mutableLevel.toLevel), "domain_adl.pddl")
                    }
                })
                contents += new MenuItem(Action("Generate PDDL object-fluents") {
                    if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                        savePickAndTextFile(EncoderPDDL.encodeObjectFluents(uiLevel.mutableLevel.toLevel), "domain_object-fluents.pddl")
                    }
                })
                contents += new MenuItem(Action("Generate PDDL numeric-fluents") {
                    if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                        savePickAndTextFile(EncoderPDDL.encodeNumericFluents(uiLevel.mutableLevel.toLevel), "domain_numeric-fluents.pddl")
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

            background = BACKGROUND_COLOR

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

    private def showSolverUpdate(plannerUpdate: PlannerUpdate[DecodingData]): Unit = { // TODO UI
        println("Timesteps: " + plannerUpdate.timeStepResult.timeSteps + " sat: " + plannerUpdate.timeStepResult.sat + " Time: " + plannerUpdate.timeStepResult.milliseconds + " TotalTime: " + plannerUpdate.totalMilliseconds + " (ms)")
    }

    private def showResult(snowmanSolverResult: SnowmanSolverResult): Unit = { // TODO UI
        println("Solved: " + snowmanSolverResult.solved)

        snowmanSolverResult.result match {
            case Some(r) =>
                println("Valid: " + snowmanSolverResult.valid)
                println("Actions: " + r.actions.size)
                r.actions.foreach(f => println("    " + f.toString))
                println("Ball references from initial state:")
                r.balls.zipWithIndex.foreach(f => println("    Ball (" + f._2 + "): " + f._1))
                println("Ball Actions: " + r.actionsBall.size)
                r.actionsBall   .foreach(f => println("    " + f.toString))

            case None =>
        }
    }

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

    private def validateLevelShowDialog(mutableLevel: MutableLevel, title: String = "Error", messageType: Dialog.Message.Value = Dialog.Message.Warning): Boolean = {
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

            val message = characterMessate + "\n" + ballsMessage

            if (title == "") {
                showErroDialog(message)
            } else {
                Dialog.showMessage(null, message, title, messageType)
            }

            false
        } else {
            true
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

    private def invariantsLayout(): (JCheckBox, JCheckBox, JCheckBox, JPanel) = {
        val ballSizesCheckBox = new JCheckBox("Ball sizes")
        val ballPositionsCheckBox = new JCheckBox("Ball positions")
        val ballsDistancesCheckBox = new JCheckBox("Balls distances")

        val panel = new JPanel
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS))
        panel.setBorder(BorderFactory.createTitledBorder("Invariants"))
        panel.add(ballSizesCheckBox)
        panel.add(ballPositionsCheckBox)
        panel.add(ballsDistancesCheckBox)

        (ballSizesCheckBox, ballPositionsCheckBox, ballsDistancesCheckBox, panel)
    }

    private def showGenerateOptionsDialog(): Option[(GenerateOptions, EncoderOptions)] = {
        val timeStepTextField = new JTextField("1", 5)

        val threadsLabel = new JLabel("Time Step:")

        val timeStepPanel = new JPanel(new GridBagLayout)
        timeStepPanel.setBorder(BorderFactory.createTitledBorder("Options"))
        val timeStepC = new GridBagConstraints()

        val rightPadding = 5

        timeStepC.anchor = GridBagConstraints.EAST
        timeStepC.gridx = 0
        timeStepC.gridy = 0
        timeStepC.insets = new Insets(0, 0, 0, rightPadding)
        timeStepPanel.add(threadsLabel, timeStepC)

        timeStepC.gridx = 1
        timeStepC.gridy = 0
        timeStepC.insets = new Insets(0, 0, 0, 0)
        timeStepPanel.add(timeStepTextField, timeStepC)
        val (ballSizesCheckBox, ballPositionsCheckBox, ballsDistancesCheckBox, invariantPanel)  = invariantsLayout()

        val panel = new JPanel
        panel.setLayout(new GridBagLayout)
        val panelC = new GridBagConstraints
        panelC.anchor = GridBagConstraints.NORTH
        panelC.gridx = 0
        panelC.gridy = 0
        panel.add(timeStepPanel, panelC)
        panelC.gridx = 1
        panelC.gridy = 0
        panel.add(invariantPanel, panelC)

        val option = JOptionPane.showConfirmDialog(null, panel, "Solver options", JOptionPane.OK_CANCEL_OPTION)

        if (option == JOptionPane.OK_OPTION) {
            try {
                val generateOptions = GenerateOptions(timeStepTextField.getText.toInt)

                val encoderOptions = EncoderOptions(ballSizesCheckBox.isSelected,
                    ballPositionsCheckBox.isSelected,
                    ballsDistancesCheckBox.isSelected)

                Some((generateOptions, encoderOptions))
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
        val AUTO = "auto"

        val startTimeStepTextField = new JTextField(AUTO, 10)
        val maxTimeStepsTextField = new JTextField("100", 10)
        val timeoutTextField = new JTextField("3600", 10)
        val threadsTextField = new JTextField("1", 10)
        
        val startTimeStepLabel = new JLabel("Start time step:")
        val maxTimeStepsLabel = new JLabel("Max time steps:")
        val timeoutLabel = new JLabel("Timeout (s):")
        val threadsLabel = new JLabel("Threads:")

        val timestepPanel = new JPanel(new GridBagLayout)
        timestepPanel.setBorder(BorderFactory.createTitledBorder("Options"))
        val timeStepC = new GridBagConstraints()

        val rightPadding = 5
        val bottomPadding = 5
        
        timeStepC.anchor = GridBagConstraints.EAST
        timeStepC.gridx = 0
        timeStepC.gridy = 0
        timeStepC.insets = new Insets(0, 0, 0, rightPadding)
        timestepPanel.add(startTimeStepLabel, timeStepC)

        timeStepC.gridx = 1
        timeStepC.gridy = 0
        timeStepC.insets = new Insets(0, 0, bottomPadding, 0)
        timestepPanel.add(startTimeStepTextField, timeStepC)

        timeStepC.gridx = 0
        timeStepC.gridy = 1
        timeStepC.insets = new Insets(0, 0, 0, rightPadding)
        timestepPanel.add(maxTimeStepsLabel, timeStepC)

        timeStepC.gridx = 1
        timeStepC.gridy = 1
        timeStepC.insets = new Insets(0, 0, bottomPadding, 0)
        timestepPanel.add(maxTimeStepsTextField, timeStepC)

        timeStepC.anchor = GridBagConstraints.NORTHEAST
        timeStepC.gridx = 0
        timeStepC.gridy = 2
        timeStepC.insets = new Insets(0, 0, 0, rightPadding)
        timestepPanel.add(timeoutLabel, timeStepC)

        timeStepC.gridx = 1
        timeStepC.gridy = 2
        timeStepC.insets = new Insets(0, 0, bottomPadding, 0)
        timestepPanel.add(timeoutTextField, timeStepC)

        timeStepC.gridx = 0
        timeStepC.gridy = 3
        timeStepC.insets = new Insets(0, 0, 0, rightPadding)
        timestepPanel.add(threadsLabel, timeStepC)

        timeStepC.gridx = 1
        timeStepC.gridy = 3
        timeStepC.insets = new Insets(0, 0, 0, 0)
        timestepPanel.add(threadsTextField, timeStepC)

        val (ballSizesCheckBox, ballPositionsCheckBox, ballsDistancesCheckBox, invariantPanel)  = invariantsLayout()

        val panel = new JPanel
        panel.setLayout(new GridBagLayout)
        val panelC = new GridBagConstraints
        panelC.anchor = GridBagConstraints.NORTH
        panelC.gridx = 0
        panelC.gridy = 0
        panel.add(timestepPanel, panelC)
        panelC.gridx = 1
        panelC.gridy = 0
        panel.add(invariantPanel, panelC)

        val option = JOptionPane.showConfirmDialog(null, panel, "Solver options", JOptionPane.OK_CANCEL_OPTION)

        if (option == JOptionPane.OK_OPTION) {
            try {
                val startTimeSteps: Option[Int] = if (startTimeStepTextField.getText == AUTO) {
                    None
                } else {
                    Some(startTimeStepTextField.getText.toInt)
                }

                val plannerOptions = PlannerOptions(startTimeSteps,
                    maxTimeStepsTextField.getText.toInt,
                    timeoutTextField.getText.toInt,
                    threadsTextField.getText.toInt)

                val encoderOptions = EncoderOptions(ballSizesCheckBox.isSelected,
                    ballPositionsCheckBox.isSelected,
                    ballsDistancesCheckBox.isSelected)

                Some((plannerOptions, encoderOptions))
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