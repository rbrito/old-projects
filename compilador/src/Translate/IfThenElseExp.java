package Translate;


class IfThenElseExp extends Exp {

    Exp cond; // Condicao do if
    Exp a; // expressao do then
    Exp b; // expressao do else
    Temp.Label t = new Temp.Label(); // label do comeco do then
    Temp.Label f = new Temp.Label(); // label do comeco do else
    Temp.Label join = new Temp.Label(); // label do fim do ifthenelse

    // Construtor da classe
    IfThenElseExp (Exp cc, Exp aa, Exp bb) {
	cond = cc; a = aa; b = bb;
    }

    // Para uso da expressao em lugares onde seu valor e' usado
    // Ideia por tras do metodo:
    // 01 - criar um temporario res para o resultado do ifthenelse
    // 02 - avaliar a condicao cond.unCx, com os labels t e f
    // 03 - marcar o label t (inicio do then)
    // 04 - fazer um move do resultado a.unEx para res
    // 05 - pular para o label join (fim do then)
    // 06 - marcar o label f (inicio do else)
    // 07 - fazer um move do resultado b.unEx para res
    // 08 - pular para o label join (fim do else) <-- nao e' necessario
    // 09 - marcar o label join
    // 10 - devolver o valor guardado em res
    Tree.Exp unEx() {
	Temp.Temp res = new Temp.Temp(); // 01

	Tree.Stm aux1, aux2;
	Tree.Exp aux3;

	aux1 = new Tree.SEQ(new Tree.LABEL(t), // 03
			    new Tree.SEQ(new Tree.MOVE(new Tree.TEMP(res), a.unEx()), // 04
					 new Tree.JUMP(join))); // 05

	aux2 = new Tree.SEQ(new Tree.LABEL(f), // 06
			    new Tree.SEQ(new Tree.MOVE(new Tree.TEMP(res), b.unEx()), // 07
					 new Tree.JUMP(join))); // 08

	aux3 = new Tree.ESEQ(new Tree.SEQ(cond.unCx(t, f), // 02
					  new Tree.SEQ(aux1, aux2)),
			     new Tree.ESEQ(new Tree.LABEL(join), // 09
					   new Tree.TEMP(res))); // 10

	return aux3;
    }

    // Para uso da expressao em lugares onde seu valor nao e' usado
    // Ideia por tras do metodo (mais simples do que o unEx):
    // 01 - avaliar a condicao cond.unCx, com os labels t e f
    // 02 - marcar o label t (inicio do then)
    // 03 - avaliar a.unNx
    // 04 - pular para o label join (fim do then)
    // 05 - marcar o label f (inicio do else)
    // 06 - avaliar b.unNx
    // 07 - pular para o label join (fim do else) <- nao e' necessario
    // 08 - marcar o label join
    Tree.Stm unNx() {
	return new Tree.SEQ(cond.unCx(t, f), // 01
		       new Tree.SEQ(new Tree.SEQ(new Tree.SEQ(new Tree.LABEL(t), // 02
			new Tree.SEQ(a.unNx(), // 03
			 new Tree.JUMP(join))), // 04
			  new Tree.SEQ(new Tree.LABEL(f), // 05
			   new Tree.SEQ(b.unNx(), // 06
			    new Tree.JUMP(join)))), // 07
			     new Tree.LABEL(join))); // 08
    }
    

    // Para uso da expressao no lugar de condicionais
    // tt e' o label de destino caso o ifthenelse seja verdadeiro
    // ff e' o label de destino caso o ifthenelse seja falso
    // Felizmente, este daqui e' bem mais simples do que os dois acima:
    // 01 - avaliar cond.unCx
    // 02 - marcar o label do then
    // 03 - avaliar a.unCx(tt, ff)
    // 04 - marcar o label do else
    // 05 - avaliar b.unCx(tt, ff)
    Tree.Stm unCx(Temp.Label tt, Temp.Label ff) {
	return new Tree.SEQ(cond.unCx(t,f), // 01
			    new Tree.SEQ(new Tree.LABEL(t), // 02
					 new Tree.SEQ(a.unCx(tt, ff), // 03
						      new Tree.SEQ(new Tree.LABEL(f), // 04
								   b.unCx(tt, ff))))); // 05
    }
}
