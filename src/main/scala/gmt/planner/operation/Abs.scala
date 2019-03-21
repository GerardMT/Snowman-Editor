package gmt.planner.operation

object Abs {
    val ABS: Clause => Clause = c => Abs(c)
}

case class Abs(c: Clause) extends Clause {

    if (!Operations.isReturnInteger(c) ) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Abs(" + c + ")"
}
