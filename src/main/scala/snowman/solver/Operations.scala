package snowman.solver

import scala.collection.mutable.ListBuffer

sealed trait Expression

// TODO Expressions runtime validation

case class InvalidExpressionException(message: String) extends Exception(message)

case class Comment(s: String) extends Expression
case class ClauseDeclaration(c: Clause) extends Expression {
    if (Operations.isArithmetic(c)) {
        throw InvalidExpressionException(toString)
    }

    override def toString: String = "(ClauseDeclaration(" + c + ")"
}

case class VariableDeclaration(v: Clause) extends Expression {
    v match {
        case BooleanVariable(_) | IntegerVariable(_)=>
        case _ =>
            throw InvalidExpressionException(toString)
    }

    override def toString: String = "(VariableDeclaration(" + v + ")"
}

sealed trait Clause

case class Not(c: Clause) extends Clause
case class And(c: Clause*) extends Clause
case class Or(c: Clause*) extends Clause
case class Implication(c1: Clause, c2: Clause) extends Clause
case class DoubleImplication(c1: Clause, c2: Clause) extends Clause
case class BooleanVariable(name: String)  extends Clause
case class BooleanConstant(value: Boolean) extends Clause

case class Equals(c1: Clause, c2: Clause) extends Clause
case class Add(c1: Clause, c2: Clause) extends Clause
case class Sub(c1: Clause, c2: Clause) extends Clause
case class Smaller(c1: Clause, c2: Clause) extends Clause
case class Greater(c1: Clause, c2: Clause) extends Clause
case class IntegerVariable(name: String) extends Clause
case class IntegerConstant(value: Int) extends Clause


object Operations {


    def isArithmetic(c: Clause): Boolean = c match {
        case IntegerVariable(_) | IntegerConstant(_) | Add(_, _) | Sub(_, _) =>
            true
        case _ =>
            false
    }

    def getEO(v: Seq[BooleanVariable], newVariablesPrefix: String): Seq[Expression] = {
        getAMOLog(v, newVariablesPrefix) :+ getALO(v)
    }

    def getAMOLog(v: Seq[BooleanVariable], newVariablesPrefix: String): Seq[Expression]= {
        val expressions = ListBuffer.empty[Expression]

        val nBits = (Math.log(v.length) / Math.log(2)).ceil.toInt

        var newVariables = List.empty[BooleanVariable]

        for(i <- 0 until nBits) {
            val newVariable = BooleanVariable(newVariablesPrefix + i)
            expressions.append(VariableDeclaration(newVariable))
            newVariables = newVariable :: newVariables
        }

        for (i <- v.indices) {
            val binaryString = toBinary(i, nBits)

            for (j <- 0 until binaryString.length) {
                var c: Clause = newVariables(j)

                if (binaryString(j) == '0') {
                    c = Not(c)
                }

                expressions.append(ClauseDeclaration(Or(Not(v(i)), c)))
            }
        }

        expressions
    }

    def getALO(v: Seq[BooleanVariable]): Expression = {
        ClauseDeclaration(Or(v.map(f => f): _*))
    }

    private def toBinary(i: Int, digits: Int): String = {
        String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')
    }

    private def addVariables(v: Seq[IntegerVariable]): Clause = v match {
        case Seq(h1, h2) =>
            Add(h1, h2)
        case Seq(h, t @ _*) =>
            Add(h, addVariables(t))
    }

    def addEK(variables: Seq[BooleanVariable], k: Int, newVariablesPrefix: String): Seq[Expression] = {
        val expressions = ListBuffer.empty[Expression]

        val newVariables = ListBuffer.empty[IntegerVariable]

        for ((v, i) <- variables.zipWithIndex) {
            val newV = IntegerVariable(newVariablesPrefix + i)
            newVariables.append(newV)
            expressions.append(VariableDeclaration(newV))
            expressions.append(ClauseDeclaration(DoubleImplication(v, Equals(newV, IntegerConstant(1)))))
            expressions.append(ClauseDeclaration(DoubleImplication(Not(v), Equals(newV, IntegerConstant(0)))))
        }

        expressions.append(ClauseDeclaration(Equals(addVariables(newVariables), IntegerConstant(k))))

        expressions
    }

    def simplify(c: Clause): Clause = {
        c match {
            case Not(c1) =>
                Not(simplify(c1))
            case And() =>
                throw new IllegalStateException
            case And(c1) =>
                simplify(c1)
            case And(c1, c2 @ _*) =>
                And(simplify(c1) +: c2.map(f => simplify(f)): _*)
            case Or() =>
                throw new IllegalStateException
            case Or(c1) =>
                simplify(c1)
            case Or(c1, c2 @ _*) =>
                Or(simplify(c1) +: c2.map(f => simplify(f)): _*)
            case Implication(c1, c2) =>
                Implication(simplify(c1), simplify(c2))
            case DoubleImplication(c1, c2) =>
                DoubleImplication(simplify(c1), simplify(c2))
            case l @ BooleanVariable(_) =>
                l
            case l @ BooleanConstant(_) =>
                l
            case Equals(c1, c2) =>
                Equals(simplify(c1), simplify(c2))
            case Add(c1, c2) =>
                Add(simplify(c1), simplify(c2))
            case Sub(c1, c2) =>
                Sub(simplify(c1), simplify(c2))
            case Smaller(c1, c2) =>
                Smaller(simplify(c1), simplify(c2))
            case Greater(c1, c2) =>
                Greater(simplify(c1), simplify(c2))
            case a @ IntegerVariable(_) =>
                a
            case a @ IntegerConstant(_) =>
                a
        }
    }
}