package Translate;

// Subclasse da classe Exp que implementa nos (na IR) relativos a
// expressoes que NAO devolvem valores.
class Nx extends Exp {
    Tree.Stm stm;
    
    Nx(Tree.Stm s) {
	stm = s;
    }

    // Chamado quando stm e' usado lugar de uma expressao (nao deve
    // ocorrer em codigo correto) -- este codigo serve apenas para
    // garantir que uma expressao nunca seja null
    Tree.Exp unEx() {
	return new Tree.ESEQ(stm, new Tree.CONST(1));
    }

    // Chamado quando stm e' usado em lugar de um statement
    Tree.Stm unNx() {
	return stm;
    }

    // Chamado quando stm e' usado em lugar de uma expressao
    // condicional (nao deve ocorrer em codigo correto) --
    // analogamente ao caso unEx(), este codigo serve apenas para
    // garantir que uma expressao nunca e' null
    Tree.Stm unCx(Temp.Label t, Temp.Label f) {
	return new Tree.JUMP(t);
    }
}
