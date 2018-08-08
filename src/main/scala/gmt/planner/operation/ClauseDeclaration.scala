package gmt.planner.operation

case class ClauseDeclaration(c: Clause) extends Expression {

    if (!Operations.isReturnBoolean(c)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "ClauseDeclaration(" + c + ")"
}
