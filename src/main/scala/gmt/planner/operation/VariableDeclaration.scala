package gmt.planner.operation

case class VariableDeclaration(v: Clause) extends Expression {

    if (!v.isInstanceOf[BooleanVariable] && !v.isInstanceOf[IntegerVariable]) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "VariableDeclaration(" + v + ")"
}
