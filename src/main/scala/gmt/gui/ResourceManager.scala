package gmt.gui

import java.awt.Image

import gmt.snowman.game.`object`.{Character, CharacterSnow, Empty, Grass, LargeBall, LargeMediumBall, LargeMediumSmallBall, LargeSmallBall, MediumBall, MediumSmallBall, Object, SmallBall, Snow, Wall}
import javax.imageio.ImageIO


class ResourceManager {

    private var mateuMode = false

    private val empty = ImageIO.read(getClass.getResource("/gui/empty.png"))
    private val wall = ImageIO.read(getClass.getResource("/gui/wall.png"))
    private val grass = ImageIO.read(getClass.getResource("/gui/grass.png"))
    private val snow = ImageIO.read(getClass.getResource("/gui/snow.png"))
    private val smallBall = ImageIO.read(getClass.getResource("/gui/smallBall.png"))
    private val mediumBall = ImageIO.read(getClass.getResource("/gui/mediumBall.png"))
    private val largeBall = ImageIO.read(getClass.getResource("/gui/largeBall.png"))
    private val mediumSmallBall = ImageIO.read(getClass.getResource("/gui/mediumSmallBall.png"))
    private val largeSmallBall = ImageIO.read(getClass.getResource("/gui/largeSmallBall.png"))
    private val largeMediumBall = ImageIO.read(getClass.getResource("/gui/largeMediumBall.png"))
    private val largeMediumSmallBall = ImageIO.read(getClass.getResource("/gui/largeMediumSmallBall.png"))
    private val character = ImageIO.read(getClass.getResource("/gui/character.png"))
    private val characterSnow = ImageIO.read(getClass.getResource("/gui/characterSnow.png"))

    private val pickerSelected = ImageIO.read(getClass.getResource("/gui/pickerSelected.png"))

    private val emptyMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/empty.png"))
    private val wallMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/wall.png"))
    private val grassMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/grass.png"))
    private val snowMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/snow.png"))
    private val smallBallMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/smallBall.png"))
    private val mediumBallMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/mediumBall.png"))
    private val largeBallMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/largeBall.png"))
    private val mediumSmallBallMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/mediumSmallBall.png"))
    private val largeSmallBallMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/largeSmallBall.png"))
    private val largeMediumBallMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/largeMediumBall.png"))
    private val largeMediumSmallBallMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/largeMediumSmallBall.png"))
    private val characterMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/character.png"))
    private val characterSnowMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/characterSnow.png"))

    private val pickerSelectedMateu = ImageIO.read(getClass.getResource("/gui/mateu-mode/pickerSelected.png"))

    def mateuMode_(enable: Boolean): Unit = mateuMode = enable

    def getResource(o: Object): Image = {
        if (mateuMode) {
            o match {
                case Empty =>
                    emptyMateu
                case Wall =>
                    wallMateu
                case Grass =>
                    grassMateu
                case Snow =>
                    snowMateu
                case SmallBall =>
                    smallBallMateu
                case MediumBall =>
                    mediumBallMateu
                case LargeBall =>
                    largeBallMateu
                case MediumSmallBall =>
                    mediumSmallBallMateu
                case LargeSmallBall =>
                    largeSmallBallMateu
                case LargeMediumBall =>
                    largeMediumBallMateu
                case LargeMediumSmallBall =>
                    largeMediumSmallBallMateu
                case Character =>
                    characterMateu
                case CharacterSnow =>
                    characterSnowMateu
            }
        } else {
            o match {
                case Empty =>
                    empty
                case Wall =>
                    wall
                case Grass =>
                    grass
                case Snow =>
                    snow
                case SmallBall =>
                    smallBall
                case MediumBall =>
                    mediumBall
                case LargeBall =>
                    largeBall
                case MediumSmallBall =>
                    mediumSmallBall
                case LargeSmallBall =>
                    largeSmallBall
                case LargeMediumBall =>
                    largeMediumBall
                case LargeMediumSmallBall =>
                    largeMediumSmallBall
                case Character =>
                    character
                case CharacterSnow =>
                    characterSnow
            }
        }
    }

    def getPickerSelected: Image = {
        if (mateuMode) {
            pickerSelectedMateu
        } else {
            pickerSelected
        }
    }

    def getIcon: Image = wallMateu
}