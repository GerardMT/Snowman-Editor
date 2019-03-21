package gmt.planner.encoder

import gmt.planner.operation._

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

class Encoding {

    private val _variablesSet = mutable.Set.empty[String]

    private val _expressions = ListBuffer.empty[Expression]

    private var variableName: Int = 0

    def add(e: Expression*): Unit = {
        e.foreach {
            case variableDeclaration @ VariableDeclaration(v) =>
                val name = v match {
                    case variable: IntegerVariable =>
                        if (variable.name == "") {
                            variable.name = "V_" + variableName.toString
                            variableName += 1
                        }

                        variable.name
                    case variable: BooleanVariable =>
                        if (variable.name == "") {
                            variable.name = "V_" + variableName.toString
                            variableName += 1
                        }

                        variable.name
                    case _ =>
                        throw InvalidClauseException(variableDeclaration.toString)
                }

                if (_variablesSet(name)) {
                    throw new IllegalStateException("Variable already exists: " + name)
                } else {
                    _variablesSet.add(name)
                    _expressions.append(variableDeclaration)
                }
            case f =>
                _expressions.append(f)
        }
    }

    def addAll(e: Seq[Expression]): Unit = {
        e.foreach(f => add(f))
    }

    def expressions: immutable.Seq[Expression]  = _expressions.toList
}
