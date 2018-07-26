package gmt.planner.operation

case class Or(c: Clause*) extends Clause {

    if (c.forall(f => !Operations.isReturnLogical(f))) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Or(" + c.head + c.tail.map(f => ", " + f.toString) + ")"
}
