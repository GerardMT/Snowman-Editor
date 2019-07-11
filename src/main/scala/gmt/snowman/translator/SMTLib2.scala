package gmt.snowman.translator

import gmt.planner.encoder.Encoding
import gmt.planner.operation._

object SMTLib2 {

    def translate(p: Encoding): String = {
        val stringBuilder = new StringBuilder

        while (!p.isEmpty) {
            stringBuilder.append(translateExpression(p.pop) + "\n")
        }

        stringBuilder.mkString
    }

    private def translateExpression(e: Expression) = {
        e match {
            case Comment(s) =>
                "; " + s
            case VariableDeclaration(v) =>
                val s = v match {
                    case BooleanVariable(name) =>
                        name + "::bool"
                    case IntegerVariable(name) =>
                        name + "::int"
                }

                "(define " + s + ")"
            case ClauseDeclaration(c) =>
                "(assert " + translateClause(c) + ")"
            case Custom(s) =>
                s
        }
    }

    private def translateClause(c: Clause): String = {
        c match {
            case Not(c1) =>
                "(not " + translateClause(c1) + ")"
            case And() | And(_) =>
                throw new IllegalStateException
            case And(c1, c2 @ _*) =>
                "(and " + translateClause(c1) + " " + c2.map(f => translateClause(f)).mkString(" ") + ")"
            case Or() | Or(_) =>
                throw new IllegalStateException
            case Or(c1, c2 @ _*) =>
                "(or " + translateClause(c1) + " " + c2.map(f => translateClause(f)).mkString(" ") + ")"
            case Implies(c1, c2) =>
                "(=> " + translateClause(c1) + " " + translateClause(c2) + ")"
            case Equivalent(c1, c2) =>
                "(= " + translateClause(c1) + " " + translateClause(c2) + ")"
            case BooleanVariable(name) =>
                name
            case BooleanConstant(value) =>
                if (value) {
                    "true"
                } else {
                    "false"
                }
            case Equals(c1, c2) =>
                "(= " + translateClause(c1) + " " + translateClause(c2) + ")"
            case Add(c1, c2) =>
                "(+ " + translateClause(c1) + " " + translateClause(c2) + ")"
            case Sub(c1, c2) =>
                "(- " + translateClause(c1) + " " + translateClause(c2) + ")"
            case Smaller(c1, c2) =>
                "(< " + translateClause(c1) + " " + translateClause(c2) + ")"
            case SmallerEqual(c1, c2) =>
                "(<= " + translateClause(c1) + " " + translateClause(c2) + ")"
            case Greater(c1, c2) =>
                "(> " + translateClause(c1) + " " + translateClause(c2) + ")"
            case GreaterEqual(c1, c2) =>
                "(>= " + translateClause(c1) + " " + translateClause(c2) + ")"
            case IntegerVariable(name) =>
                name
            case IntegerConstant(value) =>
                value.toString
            case Ite(c1, c2, c3) =>
                "(ite " + translateClause(c1) + " " + translateClause(c2) + " " + translateClause(c3) + ")"
            case Abs(c) =>
                "(abs " + translateClause(c) + ")"
        }
    }
}