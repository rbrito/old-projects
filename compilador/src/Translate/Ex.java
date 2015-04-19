package Translate;

// Subclasse da classe Exp que implementa nos (na IR) relativos a
// expressoes que devolvem valores.
class Ex extends Exp {
    Tree.Exp exp;

    Ex(Tree.Exp e) {
        exp = e;
    }

    // Chamado quando exp e' usado em uma expressao cujo valor e'
    // utilizado
    Tree.Exp unEx() {
        return exp;
    }

    // Chamado quando o valor de exp deve ser ignorado na construcao
    // da IR
    Tree.Stm unNx() {
        return new Tree.EXP(exp);
    }

    // Chamado quando exp e' usado em lugar de uma expressao
    // condicional
    Tree.Stm unCx(Temp.Label t, Temp.Label f) {

        // Caso especial: o no' e' uma constante
        if (exp instanceof Tree.CONST) {
            if (((Tree.CONST) exp).value == 0) {
                return new Tree.JUMP(f);
            } else {
                return new Tree.JUMP(t);
            }
        } else { // caso geral
            return new Tree.CJUMP(Tree.CJUMP.NE, exp, new Tree.CONST(0), t, f);
        }
    }
}
