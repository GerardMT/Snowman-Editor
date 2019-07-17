package gmt.planner.operation

import scala.collection.mutable.{ArrayBuffer, ListBuffer}


object Operations {

    case class MeaninglessEncodingException() extends Exception()

    def isReturnInteger(c: Clause): Boolean = c match {
        case IntegerVariable(_) | IntegerConstant(_) | Add(_, _) | Sub(_, _) | Abs(_) =>
            true
        case Ite(_, c1, _) =>
            isReturnInteger(c1)
        case _ =>
            false
    }

    def isReturnBoolean(c: Clause): Boolean = c match {
        case BooleanVariable(_) | BooleanConstant(_) | And(_*) | Or(_*) | Not(_) | Implies(_,_) | Equivalent(_,_) | Equals(_,_) | Smaller(_,_) | Greater(_,_)  |  SmallerEqual(_, _) | GreaterEqual(_, _) =>
            true
        case Ite(_, c1, _) =>
            isReturnBoolean(c1)
        case _=>
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

    def getALKTotalizer(input: Seq[Clause], k: Int): Seq[Expression] = {
        val (variables, expressions) = totalizer(input)
        expressions :+ ClauseDeclaration(variables(k - 1))
    }

    def getAMKTotalizer(input: Seq[Clause], k: Int): Seq[Expression] = {
        val (variables, expressions) = totalizer(input)
        expressions :+ ClauseDeclaration(Not(variables(k)))
    }

    def getEKTotalizer(input: Seq[Clause], k: Int): Seq[Expression] = {
        val (variables, expressions) = totalizer(input)
        expressions ++ List(ClauseDeclaration(variables(k - 1)), ClauseDeclaration(Not(variables(k))))
    }

    private def totalizer(input: Seq[Clause]): (Vector[Clause], Seq[Expression]) = {
        if (input.length <= 1) {
            throw MeaninglessEncodingException()
        }

        val expressions = ListBuffer.empty[Expression]

        val leftChild = (i: Int) => i * 2 + 1
        val rightChild = (i: Int) => i * 2 + 2

        val tree = new Array[Array[Clause]](2 * input.length - 1)

        for (i <- input.indices) {
            val array = new Array[Clause](1)
            array(0) = input(i)
            tree(input.length - 1 + i) = array
        }

        for (i <- input.length - 2 to 0 by -1) {
            val ls = tree(leftChild(i)).length
            val rs = tree(rightChild(i)).length

            tree(i) = new Array(ls + rs)

            for (j <- 0 until ls + rs) {
                val b = BooleanVariable()
                expressions.append(VariableDeclaration(b))
                tree(i)(j) = b
            }

            for (j <- 0 until ls) {
                expressions.append(ClauseDeclaration(Or(Not(tree(leftChild(i))(j)), tree(i)(j))))
                for (k <- 0 until rs) {
                    expressions.append(ClauseDeclaration(Or(Not(tree(leftChild(i))(j)), Not(tree(rightChild(i))(k)), tree(i)(j + k + 1))))
                }
            }

            for (k <- 0 until rs) {
                expressions.append(ClauseDeclaration(Or(Not(tree(rightChild(i))(k)), tree(i)(k))))
            }
        }

        (tree(0).toVector , expressions)
    }

    private def toBinary(i: Int, digits: Int): String = {
        String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')
    }

    def simplify(c: Clause): Clause = {
        c match {
            case And(c1) => c1
            case c @ And(_,_ @ _*) => c
            case Or(c1) => c1
            case c @ Or(_,_ @ _*) => c
        }
    }
}