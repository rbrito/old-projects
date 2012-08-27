package Semant;

class VarEntry extends Entry { // entrada de uma variavel em venv
    Translate.Access access;
    Types.Type ty;
    
    VarEntry(Translate.Access a, Types.Type t) {
	access = a;
	ty = t;
    }
}
