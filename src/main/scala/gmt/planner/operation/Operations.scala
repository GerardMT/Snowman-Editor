package gmt.planner.operation

import scala.collection.mutable.ListBuffer


object Operations {
    def isVariable(f: Clause): Boolean = f match {
        case BooleanVariable(_) | IntegerVariable(_) =>
            true
        case _ =>
            false
    }


    def isReturnInteger(c: Clause): Boolean = c match {
        case IntegerVariable(_) | IntegerConstant(_) | Add(_, _) | Sub(_, _) =>
            true
        case _ =>
            false
    }

    def isReturnBoolean(c: Clause): Boolean = c match {
        case BooleanVariable(_) | BooleanConstant(_) | And(_*) | Or(_*) | Not(_) | Implies(_,_) | Equivalent(_,_) | Equals(_,_) | Smaller(_,_) | Greater(_,_) =>
            true
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

    private def toBinary(i: Int, digits: Int): String = {
        String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')
    }

    def addCounter(variables: Seq[BooleanVariable], k: Int, newVariablesPrefix: String, comparator: (Clause, Clause) => Clause): (Clause, Seq[VariableDeclaration]) = {
        val variableDeclarations = ListBuffer.empty[VariableDeclaration]
        val ands = ListBuffer.empty[Clause]

        val newVariables = ListBuffer.empty[IntegerVariable]

        for ((v, i) <- variables.zipWithIndex) {
            val newV = IntegerVariable(newVariablesPrefix + i)
            newVariables.append(newV)
            variableDeclarations.append(VariableDeclaration(newV))
            ands.append(Equivalent(v, Equals(newV, IntegerConstant(1))))
            ands.append(Equivalent(Not(v), Equals(newV, IntegerConstant(0))))
        }

        ands.append(comparator(newVariables.reduce(Add.ADD), IntegerConstant(k)))

        (And(ands: _*), variableDeclarations)
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