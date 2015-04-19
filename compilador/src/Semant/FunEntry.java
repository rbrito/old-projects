package Semant;

class FunEntry extends Entry { // entrada de uma funcao em venv
    public Translate.Level level;
    Temp.Label label;
    Types.RECORD formals;
    Types.Type result;

    FunEntry(Translate.Level v, Temp.Label l, Types.RECORD f, Types.Type r) {
        level = v;
        label = l;
        formals = f;
        result = r;
    }
}
