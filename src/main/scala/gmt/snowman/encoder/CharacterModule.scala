package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation
import gmt.planner.operation.{IntegerVariable, VariableDeclaration}
import gmt.snowman.encoder.StateBase.Location
import gmt.snowman.level.Level

object CharacterModule{
    case class Character(override val x: IntegerVariable, override  val y: IntegerVariable) extends Location(x, y)

    def apply(level: Level, timeStep: Int): CharacterModule = {
        CharacterModule(Character(IntegerVariable("C_X_S" + timeStep), IntegerVariable("CY_S" + timeStep)))
    }
}

case class CharacterModule private (override val character: CharacterModule.Character) extends CharacterInterface with VariableAdder {

    override def addVariables(encoding: Encoding): Unit = {
        encoding.add(VariableDeclaration(character.x))
        encoding.add(operation.VariableDeclaration(character.y))
    }
}
