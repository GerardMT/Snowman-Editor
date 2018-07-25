package snowman.solver

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

class Problem {

    private val _variablesSet = mutable.Set.empty[String]

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
                    _expressions.append(f)
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
