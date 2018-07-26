package gmt.planner.operation

case class ClauseDeclaration(c: Clause) extends Expression {

    if (!Operations.isReturnLogical(c)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "ClauseDeclaration(" + c + ")"
}
