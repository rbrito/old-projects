package Translate;

// Classe que implementa expressoes que contem 1 (e apenas 1) operador
// relacional.
public class RelCx extends Cx {
    int op; // operador relacional
    Tree.Exp left; // lado esquerdo da expressao
    Tree.Exp right; // lado direito da expressao

    // Construtor
    RelCx(int o, Tree.Exp l, Tree.Exp r) {
        op = o;
        left = l;
        right = r;
    }

    // Metodo para traducao da expressao relacional na sua IR
    Tree.Stm unCx(Temp.Label t, Temp.Label f) {
        return new Tree.CJUMP(op, left, right, t, f);
    }
}
