package gmt.snowman.translator

import gmt.planner.encoder.Encoding
import gmt.planner.operation._
import gmt.planner.translator.Translator


object SMTLib2 extends Translator {

    override def translate(p: Encoding): String = {
        var expressions = ""
        var clauseAssert = true
        for (e <- p.expressions) {
            if (e.isInstanceOf[Custom]) {
                clauseAssert = false
            }
            expressions += SMTLib2.translateExpression(e, clauseAssert) + "\n"
        }

        "(set-logic LIA)" +
        p.variables.map(f => SMTLib2.translateExpression(f) + "\n").mkString +
        expressions +
        "(check-sat)\n" +
        "(get-model)\n" +
        "(exit)"
    }

    private def translateExpression(e: Expression, clauseAssert: Boolean = true) = {
        e match {
            case Comment(s) =>
                "; " + s
            case VariableDeclaration(v) =>
                val s = v match {
                    case BooleanVariable(name) =>
                        name + " Bool"
                    case IntegerVariable(name) =>
                        name + " Int"
                }

                "(declare-const " + s + ")"
            case ClauseDeclaration(c) =>
                if (clauseAssert) {
                    "(assert " + translateClause(c) + ")"
                } else {
                    translateClause(c)
                }
            case Custom(f) =>
                f()
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
                "(and (=> " + translateClause(c1) + " " + translateClause(c2) + ") (=> " + translateClause(c2) + " " + translateClause(c1) + "))"
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
            case Greater(c1, c2) =>
                "(> " + translateClause(c1) + " " + translateClause(c2) + ")"
            case IntegerVariable(name) =>
                name
            case IntegerConstant(value) =>
                value.toString
        }
    }
}