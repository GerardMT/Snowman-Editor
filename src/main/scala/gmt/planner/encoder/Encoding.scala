package gmt.planner.encoder

import gmt.planner.operation.{BooleanVariable, Expression, IntegerVariable, VariableDeclaration}

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

class Encoding {

    private val _variablesSet = mutable.Set.empty[String]

    private val _variables = ListBuffer.empty[Expression]
    private val _expressions = ListBuffer.empty[Expression]

    def add(e: Expression*): Unit = {
        e.foreach {
            case f @ VariableDeclaration(v) =>
                val name = v match {
                    case BooleanVariable(n) =>
                        n
                    case IntegerVariable(n) =>
                        n
                }

                if (_variablesSet(name)) {
                    throw new IllegalStateException("Variable already exists: " + name)
                } else {
                    _variablesSet.add(name)
                    _variables.append(f)
                }
            case f =>
                _expressions.append(f)
        }
    }

    def addAll(e: Seq[Expression]): Unit = {
        e.foreach(f => add(f))
    }

    def expressions: immutable.Seq[Expression]  = _expressions.toList

    def variables: immutable.Seq[Expression] = _variables.toList
}
