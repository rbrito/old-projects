package Translate;

// Classe abstrata para construcao da arvore de representacao
// intermediaria
abstract public class Exp {
    abstract Tree.Exp unEx();

    abstract Tree.Stm unNx();

    abstract Tree.Stm unCx(Temp.Label t, Temp.Label f);
}
