package gmt.ui

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.{GraphicsEnvironment, GridBagConstraints, GridBagLayout, Insets, Rectangle}
import java.io.{File, FileNotFoundException}

import gmt.planner.planner.Planner.{PlannerOptions, PlannerUpdate}
import gmt.snowman.encoder.DecodingData
import gmt.snowman.encoder.EncoderBase.{EncoderEnum, EncoderOptions}
import gmt.snowman.level.MutableLevel
import gmt.snowman.pddl.EncoderPDDL
import gmt.snowman.solver.SnowmanSolver.{AutoSolveUpdate, AutoSolveUpdateFinal, AutoSolveUpdateProgress, GenerateOptions}
import gmt.snowman.solver.{SnowmanSolver, SnowmanSolverResult}
import gmt.snowman.util.Files
import javax.swing._

import scala.swing.GridBagPanel.Anchor
import scala.swing.{Action, Color, Dialog, FileChooser, GridBagPanel, MainFrame, Menu, MenuBar, MenuItem, ScrollPane, Separator, SimpleSwingApplication}


object UI extends SimpleSwingApplication {

    private val CURRENT_DIRECTORY = System.getProperty("user.dir")

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
                contents += new MenuItem(Action("Open") {
                    fileChooser.selectedFile_=(new File(CURRENT_DIRECTORY))
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
                    savePickAndTextFile(uiLevel.mutableLevel.save, CURRENT_DIRECTORY + "/level.lvl")
                })
                contents += new Separator
                contents += new Menu("Load") {
                    settings.gamePath match {
                        case Some(s) =>
                            val levelsNames: List[String] = game.levelsNames

                            for (n <- levelsNames) {
                                contents += new MenuItem(Action(n) {
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
                                    savePickAndTextFile(SnowmanSolver.encodeSMTLIB2(uiLevel.mutableLevel.toLevel, EncoderEnum.BASIC, e, g), CURRENT_DIRECTORY + "/in_basic-encoding.smtlib2")
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
                                            showResult(SnowmanSolver.solveSMTYics2(s, uiLevel.mutableLevel.toLevel, EncoderEnum.BASIC, e, p, showSolverUpdate))
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
                                    savePickAndTextFile(SnowmanSolver.encodeSMTLIB2(uiLevel.mutableLevel.toLevel, EncoderEnum.CHEATING, e, g), CURRENT_DIRECTORY + "/in_cheating-encoding.smtlib2")
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
                                            showResult(SnowmanSolver.solveSMTYics2(s, uiLevel.mutableLevel.toLevel, EncoderEnum.CHEATING, e, p, showSolverUpdate))
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
                                    savePickAndTextFile(SnowmanSolver.encodeSMTLIB2(uiLevel.mutableLevel.toLevel, EncoderEnum.REACHABILITY, e, g), CURRENT_DIRECTORY + "/in_reachability-encoding.smtlib2")
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
                                            showResult(SnowmanSolver.solveSMTYics2(s, uiLevel.mutableLevel.toLevel, EncoderEnum.REACHABILITY, e, p, showSolverUpdate))
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
                        savePickAndTextFile(EncoderPDDL.encodeStrips(uiLevel.mutableLevel.toLevel), CURRENT_DIRECTORY + "/domain_adl.pddl")
                    }
                })
                contents += new MenuItem(Action("Generate PDDL object-fluents") {
                    if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                        savePickAndTextFile(EncoderPDDL.encodeObjectFluents(uiLevel.mutableLevel.toLevel), CURRENT_DIRECTORY + "/domain_object-fluents.pddl")
                    }
                })
                contents += new MenuItem(Action("Generate PDDL numeric-fluents") {
                    if (validateLevelShowDialog(uiLevel.mutableLevel)) {
                        savePickAndTextFile(EncoderPDDL.encodeNumericFluents(uiLevel.mutableLevel.toLevel), CURRENT_DIRECTORY + "/domain_numeric-fluents.pddl")
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
            contents += new Menu("Autorunner") {
                contents += new MenuItem(Action("Solve SMT Basic") {
                    settings.solverPath match {
                        case Some(solverPath) =>
                            showAutorunOptionsDialog() match {
                                case Some((p, e, levelsPath, outPath)) =>
                                    SnowmanSolver.autoSolveSMTYices2(solverPath, levelsPath, outPath, EncoderEnum.BASIC, e, p, showAutorSolveUpdate)
                                case None =>
                            }
                        case None =>
                            showErrorSolverPathNotDeined()
                    }
                })
                contents += new MenuItem(Action("Solve SMT Cheating") {
                    settings.solverPath match {
                        case Some(solverPath) =>
                            showAutorunOptionsDialog() match {
                                case Some((p, e, levelsPath, outPath)) =>
                                    SnowmanSolver.autoSolveSMTYices2(solverPath, levelsPath, outPath, EncoderEnum.CHEATING, e, p, showAutorSolveUpdate)
                                case None =>
                            }
                        case None =>
                            showErrorSolverPathNotDeined()

                    }
                })
                contents += new MenuItem(Action("Solve SMT Reachability") {
                    settings.solverPath match {
                        case Some(solverPath) =>
                            showAutorunOptionsDialog() match {
                                case Some((p, e, levelsPath, outPath)) =>
                                    SnowmanSolver.autoSolveSMTYices2(solverPath, levelsPath, outPath, EncoderEnum.REACHABILITY, e, p, showAutorSolveUpdate)
                                case None =>
                            }
                        case None =>
                            showErrorSolverPathNotDeined()
                    }
                })
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

    private def showAutorSolveUpdate(autoSolveUpdate: AutoSolveUpdate): Unit = {
        print("Level: " + autoSolveUpdate.level)
        autoSolveUpdate match {
            case AutoSolveUpdateProgress(_, plannerUpdate) =>
                showSolverUpdate(plannerUpdate)
            case AutoSolveUpdateFinal(_, snowmanSolverResult) =>
                showResult(snowmanSolverResult)
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


    private class InvariantsLayout {

        private val ballSizesCheckBox = new JCheckBox("Ball sizes")
        private val ballPositionsCheckBox = new JCheckBox("Ball positions")
        private val ballsDistancesCheckBox = new JCheckBox("Balls distances")

        def component: JComponent = {
            val panel = new JPanel
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS))
            panel.setBorder(BorderFactory.createTitledBorder("Invariants"))
            panel.add(ballSizesCheckBox)
            panel.add(ballPositionsCheckBox)
            panel.add(ballsDistancesCheckBox)

            panel
        }

        def result: EncoderOptions = {
            EncoderOptions(ballSizesCheckBox.isSelected, ballPositionsCheckBox.isSelected, ballsDistancesCheckBox.isSelected)
        }
    }

    private class OptionsLayout {

        private val AUTO = "auto"

        private val startTimeStepTextField = new JTextField(AUTO, 10)
        private val maxTimeStepsTextField = new JTextField("100", 10)
        private val timeoutTextField = new JTextField("3600", 10)
        private val threadsTextField = new JTextField("1", 10)

        def component: JComponent = {
            val startTimeStepLabel = new JLabel("Start time step:")
            val maxTimeStepsLabel = new JLabel("Max time steps:")
            val timeoutLabel = new JLabel("Timeout (s):")
            val threadsLabel = new JLabel("Threads:")

            val panel = new JPanel(new GridBagLayout)
            panel.setBorder(BorderFactory.createTitledBorder("Options"))
            val c = new GridBagConstraints()

            val RIGHT_PADDING = 5
            val BOTTOM_PADDING = 5

            c.anchor = GridBagConstraints.EAST
            c.gridx = 0
            c.gridy = 0
            c.insets = new Insets(0, 0, 0, RIGHT_PADDING)
            panel.add(startTimeStepLabel, c)

            c.gridx = 1
            c.gridy = 0
            c.insets = new Insets(0, 0, BOTTOM_PADDING, 0)
            panel.add(startTimeStepTextField, c)

            c.gridx = 0
            c.gridy = 1
            c.insets = new Insets(0, 0, 0, RIGHT_PADDING)
            panel.add(maxTimeStepsLabel, c)

            c.gridx = 1
            c.gridy = 1
            c.insets = new Insets(0, 0, BOTTOM_PADDING, 0)
            panel.add(maxTimeStepsTextField, c)

            c.anchor = GridBagConstraints.NORTHEAST
            c.gridx = 0
            c.gridy = 2
            c.insets = new Insets(0, 0, 0, RIGHT_PADDING)
            panel.add(timeoutLabel, c)

            c.gridx = 1
            c.gridy = 2
            c.insets = new Insets(0, 0, BOTTOM_PADDING, 0)
            panel.add(timeoutTextField, c)

            c.gridx = 0
            c.gridy = 3
            c.insets = new Insets(0, 0, 0, RIGHT_PADDING)
            panel.add(threadsLabel, c)

            c.gridx = 1
            c.gridy = 3
            c.insets = new Insets(0, 0, 0, 0)
            panel.add(threadsTextField, c)

            panel
        }

        def result: PlannerOptions = {
            val startTimeSteps: Option[Int] = if (startTimeStepTextField.getText == AUTO) {
                None
            } else {
                Some(startTimeStepTextField.getText.toInt)
            }

            PlannerOptions(startTimeSteps,
                maxTimeStepsTextField.getText.toInt,
                timeoutTextField.getText.toInt,
                threadsTextField.getText.toInt)
        }
    }

    private def showGenerateOptionsDialog(): Option[(GenerateOptions, EncoderOptions)] = {
        val invariantsLayout = new InvariantsLayout

        val timeStepTextField = new JTextField("1", 5)

        val threadsLabel = new JLabel("Time Step:")

        val timeStepPanel = new JPanel(new GridBagLayout)
        timeStepPanel.setBorder(BorderFactory.createTitledBorder("Options"))
        val timeStepC = new GridBagConstraints()

        val RIGHT_PADDING = 5

        timeStepC.anchor = GridBagConstraints.EAST
        timeStepC.gridx = 0
        timeStepC.gridy = 0
        timeStepC.insets = new Insets(0, 0, 0, RIGHT_PADDING)
        timeStepPanel.add(threadsLabel, timeStepC)

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

    private def showAutorunOptionsDialog(): Option[(PlannerOptions, EncoderOptions, String, String)] = {
        val optionsLayout = new OptionsLayout
        val invariantsLayout = new InvariantsLayout

        val LEVELS_DEFAULT_PATH = CURRENT_DIRECTORY + "/autorunner/levels/"
        val OUT_DEFAULT_PATH = CURRENT_DIRECTORY + "/autorunner/out/"

        val levelsTextField = new JTextField(LEVELS_DEFAULT_PATH, 30)
        val outTextField = new JTextField(OUT_DEFAULT_PATH, 30)

        val directoryChooser = new FileChooser
        directoryChooser.fileSelectionMode_=(FileChooser.SelectionMode.DirectoriesOnly)

        val levelsButton = new JButton("Select")
        //noinspection ConvertExpressionToSAM
        levelsButton.addActionListener(new ActionListener {
            override def actionPerformed(e: ActionEvent): Unit = {
                directoryChooser.selectedFile_=(new File(LEVELS_DEFAULT_PATH))
                val result = directoryChooser.showDialog(null, "Select")

                if (result == FileChooser.Result.Approve) {
                    levelsTextField.setText(directoryChooser.selectedFile.getAbsolutePath)
                }
            }
        })

        val outButton = new JButton("Select")
        //noinspection ConvertExpressionToSAM
        outButton.addActionListener(new ActionListener {
            override def actionPerformed(e: ActionEvent): Unit = {
                directoryChooser.selectedFile_=(new File(OUT_DEFAULT_PATH))
                val result = directoryChooser.showDialog(null, "Select")

                if (result == FileChooser.Result.Approve) {
                    outTextField.setText(directoryChooser.selectedFile.getAbsolutePath)
                }
            }
        })

        val BOTTOM_PADDING = 5
        val RIGHT_PADDING = 5

        val pathPanel = new JPanel(new GridBagLayout)
        pathPanel.setBorder(BorderFactory.createTitledBorder("Paths"))
        val pathC = new GridBagConstraints
        pathC.anchor = GridBagConstraints.EAST
        pathC.insets = new Insets(0, 0, BOTTOM_PADDING, RIGHT_PADDING)
        pathC.gridx = 0
        pathC.gridy = 0
        pathPanel.add(new JLabel("Levels directory:"), pathC)
        pathC.insets = new Insets(0, 0, BOTTOM_PADDING, RIGHT_PADDING)
        pathC.gridx = 1
        pathC.gridy = 0
        pathPanel.add(levelsTextField, pathC)
        pathC.insets = new Insets(0, 0, BOTTOM_PADDING, 0)
        pathC.gridx = 2
        pathC.gridy = 0
        pathPanel.add(levelsButton, pathC)
        pathC.insets = new Insets(0, 0, 0, RIGHT_PADDING)
        pathC.gridx = 0
        pathC.gridy = 1
        pathPanel.add(new JLabel("Out directory:"), pathC)
        pathC.insets = new Insets(0, 0, 0, RIGHT_PADDING)
        pathC.gridx = 1
        pathC.gridy = 1
        pathPanel.add(outTextField, pathC)
        pathC.insets = new Insets(0, 0, 0, 0)
        pathC.gridx = 2
        pathC.gridy = 1
        pathPanel.add(outButton, pathC)

        val panel = new JPanel(new GridBagLayout)
        val c = new GridBagConstraints
        c.anchor = GridBagConstraints.NORTHWEST
        c.gridwidth = 2
        c.gridx = 0
        c.gridy = 0
        panel.add(pathPanel, c)
        c.gridwidth = 1
        c.gridx = 0
        c.gridy = 1
        panel.add(optionsLayout.component, c)
        c.gridx = 1
        c.gridy = 1
        panel.add(invariantsLayout.component, c)

        val option = JOptionPane.showConfirmDialog(null, panel, "Autorun options", JOptionPane.OK_CANCEL_OPTION)

        if (option == JOptionPane.OK_OPTION) {
            try {
                if (!new File(levelsTextField.getText).exists()) {
                    throw new FileNotFoundException()
                }
                new File(outTextField.getText).mkdirs()

                Some((optionsLayout.result, invariantsLayout.result, levelsTextField.getText(), outTextField.getText))
            } catch {
                case _: NumberFormatException =>
                    showErroDialog("Number must be an integer")
                    None
                case _: FileNotFoundException =>
                    showErroDialog("Directory not found")
                    None
            }

        } else {
            None
        }
    }
}