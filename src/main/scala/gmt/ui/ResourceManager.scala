package gmt.ui

import java.awt.Image

import gmt.snowman.level.`object`.{Character, CharacterSnow, Empty, Grass, LargeBall, LargeMediumBall, LargeMediumSmallBall, LargeSmallBall, MediumBall, MediumSmallBall, Object, SmallBall, Snow, Wall}
import javax.imageio.ImageIO


class ResourceManager {
    
    private val empty = ImageIO.read(getClass.getResource("/empty.png"))
    private val wall = ImageIO.read(getClass.getResource("/wall.png"))
    private val grass = ImageIO.read(getClass.getResource("/grass.png"))
    private val snow = ImageIO.read(getClass.getResource("/snow.png"))
    private val smallBall = ImageIO.read(getClass.getResource("/smallBall.png"))
    private val mediumBall = ImageIO.read(getClass.getResource("/mediumBall.png"))
    private val largeBall = ImageIO.read(getClass.getResource("/largeBall.png"))
    private val mediumSmallBall = ImageIO.read(getClass.getResource("/mediumSmallBall.png"))
    private val largeSmallBall = ImageIO.read(getClass.getResource("/largeSmallBall.png"))
    private val largeMediumBall = ImageIO.read(getClass.getResource("/largeMediumBall.png"))
    private val largeMediumSmallBall = ImageIO.read(getClass.getResource("/largeMediumSmallBall.png"))
    private val character = ImageIO.read(getClass.getResource("/character.png"))
    private val characterSnow = ImageIO.read(getClass.getResource("/characterSnow.png"))

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
            case Character =>
                character
            case CharacterSnow =>
                characterSnow
        }
    }
}