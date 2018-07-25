package snowman.ui

import java.awt.Image
import java.io.File

import javax.imageio.ImageIO
import snowman.level.objects.{Empty, Grass, LargeBall, LargeMediumBall, LargeMediumSmallBall, LargeSmallBall, MediumBall, MediumSmallBall, Object, Player, PlayerSnow, SmallBall, Snow, Wall}

class ResourceManager(gamePath: String) {

    private val empty = ImageIO.read(new File(gamePath + "empty.png"))
    private val wall = ImageIO.read(new File(gamePath + "wall.png"))
    private val grass = ImageIO.read(new File(gamePath + "grass.png"))
    private val snow = ImageIO.read(new File(gamePath + "snow.png"))
    private val smallBall = ImageIO.read(new File(gamePath + "smallBall.png"))
    private val mediumBall = ImageIO.read(new File(gamePath + "mediumBall.png"))
    private val largeBall = ImageIO.read(new File(gamePath + "largeBall.png"))
    private val mediumSmallBall = ImageIO.read(new File(gamePath + "mediumSmallBall.png"))
    private val largeSmallBall = ImageIO.read(new File(gamePath + "largeSmallBall.png"))
    private val largeMediumBall = ImageIO.read(new File(gamePath + "largeMediumBall.png"))
    private val largeMediumSmallBall = ImageIO.read(new File(gamePath + "largeMediumSmallBall.png"))
    private val player = ImageIO.read(new File(gamePath + "player.png"))
    private val playerSnow = ImageIO.read(new File(gamePath + "playerSnow.png"))

    def getResource(o: Object): Image = {
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
            case Player =>
                player
            case PlayerSnow =>
                playerSnow
        }
    }
}