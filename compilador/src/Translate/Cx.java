package Translate;

abstract class Cx extends Exp {

    // Metodo que transforma o objeto corrente em uma arvore que
    // representa uma expressao que devolve valor.
    Tree.Exp unEx() {
        Temp.Temp r = new Temp.Temp();
        Temp.Label t = new Temp.Label();
        Temp.Label f = new Temp.Label();

        return new Tree.ESEQ(new Tree.SEQ(new Tree.MOVE(new Tree.TEMP(r),
                new Tree.CONST(1)), new Tree.SEQ(unCx(t, f), new Tree.SEQ(
                new Tree.LABEL(f), new Tree.SEQ(new Tree.MOVE(new Tree.TEMP(r),
                        new Tree.CONST(0)), new Tree.LABEL(t))))),
                new Tree.TEMP(r));
    }

    // Metodo abstrato para usar uma expressao condicional no lugar de
    // uma expressao condicional.
    abstract Tree.Stm unCx(Temp.Label t, Temp.Label f);

    // Metodo para usar o objeto corrente no lugar de uma expressao
    // que nao devolve valor.
    Tree.Stm unNx() {
        Temp.Label t = new Temp.Label();
        Temp.Label f = new Temp.Label();

        return new Tree.SEQ(new Tree.EXP(new Tree.CONST(0)), unCx(t, f));
    }
}
