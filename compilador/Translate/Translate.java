package Translate;

// Os metodos dessa classe sempre tomam como argumentos as estruturas
// mais convenientes para o Analisador Semantico (ansem) e sempre
// devolvem um Translate.Exp.
//
// Exemplos:
//
// * quando o ansem esta' tratando de um inteiro, ele nao tem nada em
// maos alem de um inteiro. Neste caso, ele passa para transInt um
// inteiro e recebe um Translate.Exp contendo o inteiro;
//
// * quando o ansem esta' tratando de uma expressao e1 + e2, ele passa
// ao pacote Translate o +, e1, e2 e espera como resultado um
// Translate.Exp.

public class Translate {
    Frame.Frame frame;
    private Frag frags = null, last = null; // lista ligada de fragmentos
    private java.util.Dictionary string2label = new java.util.Hashtable();

    void FIXME(String s) {
	System.out.println(" *** " + s);
    }
    
    public Translate(Frame.Frame f) {
	frame = f;
    }
    
    
    public void procEntryExit(Level level, Exp body) {
	Frag f;
	
	if (level == null)
	    System.out.println("level is null");
	if (body == null)
	    System.out.println("body is null");
	
	f = new ProcFrag(frame.procEntryExit1(body.unNx()), level.frame);
	
	if (frags == null && last == null) {
	    frags = f;
	    last = f;
	}
	else {
	    last.next = f;
	    last = last.next;
	}
    }
    
    public Frag getResult() {
	return frags;
    }
    
    public Exp varAlloc(Access a, Exp v, Level l) {
	Tree.Exp e = a.acc.exp(new Tree.TEMP(l.frame.FP()));
	return new Nx(new Tree.MOVE(e, v.unEx()));
    }
    
    public Exp funExp(Exp body) {
	if (body instanceof Nx) { // se e' um procedimento
	    //    return new Nx(frame.procEntryExit1(body.unNx()));
	    return new Nx(body.unNx());
	}
	else {// se e' uma funcao, mover o valor de retorno para o
	    // registrador apropriado.
	    Tree.Stm s = new Tree.MOVE(new Tree.TEMP(frame.RV()), body.unEx());
	    //return new Nx(frame.procEntryExit1(s));
	    return new Nx(s);
	}
    }
    
    public Exp intExp(int val) {
	return new Ex(new Tree.CONST(val));
    }
    
    public Exp aritExp(int op, Exp e1, Exp e2) {
	int opaux;
	
	switch (op) {
	case Absyn.OpExp.PLUS:
	    opaux = Tree.BINOP.PLUS;
	    break;
	case Absyn.OpExp.MINUS:
	    opaux = Tree.BINOP.MINUS;
	    break;
	case Absyn.OpExp.MUL:
	    opaux = Tree.BINOP.MUL;
	    break;
	case Absyn.OpExp.DIV:
	    opaux = Tree.BINOP.DIV;
	    break;
	default:
	    throw new Error("aritExp: operador invalido.");
	}
	
	return new Ex(new Tree.BINOP(opaux, e1.unEx(), e2.unEx()));
    }
    
    
    public Exp relExp(int op, Exp e1, Exp e2) {
	int opaux;
	
	switch (op) {
	case Absyn.OpExp.EQ:
	    opaux = Tree.CJUMP.EQ;
	    break;
	case Absyn.OpExp.NE:
	    opaux = Tree.CJUMP.NE;
	    break;
	case Absyn.OpExp.LT:
	    opaux = Tree.CJUMP.LT;
	    break;
	case Absyn.OpExp.LE:
	    opaux = Tree.CJUMP.LE;
	    break;
	case Absyn.OpExp.GT:
	    opaux = Tree.CJUMP.GT;
	    break;
	case Absyn.OpExp.GE:
	    opaux = Tree.CJUMP.GE;
	    break;
	default:
	    throw new Error("relExp: operador invalido.");
	}
	
	return new RelCx(opaux, e1.unEx(), e2.unEx());
    }
    
    public Exp strcmpExp(int op, Exp e1, Exp e2, Level l) {
	Exp tmp =
	    new Ex(l.frame.externalCall("stringEqual",
					new Tree.ExpList(e1.unEx(),
							 new Tree.ExpList(e2.unEx(),
									  null))));
	
	
	switch (op) {
	case Absyn.OpExp.EQ:
	    return tmp;
	case Absyn.OpExp.NE:
	    return new Ex(l.frame.externalCall("not",
					       new Tree.ExpList(tmp.unEx(),
								null)));
	default:
	    throw new Error("strcmpExp: operador invalido.");
	}
    }
    
    public Exp nilExp() {
	return intExp(0);
    }
    
    // Tratamento de strings (literais)
    // 01 - criacao de um label para a string
    // 02 - criacao de um fragmento de dados para s
    // 03 - insercao do fragmento na lista ligada
    // 04 - devolver a referencia ao label criado
    public Exp stringExp(String s) {
	Temp.Label l = (Temp.Label) string2label.get(s.intern());
	
	if (l == null) { // nao ha' essa string no "banco de dados"
	    l = new Temp.Label(); // label para a string
	    
	    // insere a string no BD para futuros usos
	    string2label.put(s.intern(), l);
	    
	    // insere a string na lista ligada de fragmentos
	    if (last == null) { // lista vazia
		frags = new DataFrag(frame.string(l, s));
		last = frags;
	    }
	    else { // lista nao-vazia
		last.next = new DataFrag(frame.string(l, s));
		last = last.next;
	    }
	}
	else { // estava no bd -- nao criamos o fragmento
	}
	
	return new Ex(new Tree.NAME(l));
    }
    
    // Chamadas de funcoes.  Recebe o label da funcao a ser chamada,
    // uma ExpList contendo os argumentos e o nivel da funcao (para
    // static link).
    public Exp callExp(Temp.Label flabel, ExpList el, Level l) {
	Tree.ExpList t = null;
	String funcao = flabel.toString();
	ExpList args;

	// Constroi uma lista de argumentos para passar para a funcao
	args = el;
	while (args != null) {
	    t = new Tree.ExpList(args.head.unEx(), t);
	    args = args.tail;
	}

	// Verificamos se a funcao chamada recebe ou nao um static link
	if (funcao.equals("print") || 
	    funcao.equals("ord") ||
	    funcao.equals("size") ||
	    funcao.equals("substring") || 
	    funcao.equals("not") || 
	    funcao.equals("flush") ||
	    funcao.equals("chr") || 
	    funcao.equals("concat") ||
	    funcao.equals("getchar") || 
	    funcao.equals("exit")) {

	    return new Ex(frame.externalCall(flabel.toString(), t));
	}


	// Nao e' uma funcao da biblioteca -- portanto, devemos passar
	// um argumento a mais para a funcao, contendo o static link.
	// Deixamos a tarefa de verificar o numero de argumentos para
	// a funcao munchArgs.
	t = null;
	while (el != null) {
	    t = new Tree.ExpList(el.head.unEx(), t);
	    el = el.tail;
	}
	t = new Tree.ExpList(new Tree.TEMP(l.frame.FP()), t);
	
	return new Ex(new Tree.CALL(new Tree.NAME(flabel), t));
    }
    
    // Criacao de records
    // 01 - criar um temporario
    // 02 - alocar memoria (n words) e colocar resultado no temporario
    // 03 - preencher a memoria com a ExpList recebida
    // 04 - devolver a expressao que contem o temporario
    public Exp recordExp(int n, ExpList el, Level l) {
	Temp.Temp t = new Temp.Temp();
	int j = n - 1;
	int ws = l.frame.wordSize();
	Tree.Exp e = new Tree.BINOP(Tree.BINOP.PLUS, new Tree.TEMP(t), 
				    new Tree.CONST(ws * j));
	
	Tree.Stm s = new Tree.MOVE(new Tree.MEM(e), el.head.unEx());
	
	j--;
	el = el.tail;
	
	while (el != null) {
	    e = new Tree.BINOP(Tree.BINOP.PLUS, new Tree.TEMP(t), 
			       new Tree.CONST(ws * j));
	    s = new Tree.SEQ(new Tree.MOVE(new Tree.MEM(e), el.head.unEx()), s);
	    j--;
	    el = el.tail; // prossegue na lista ligada
	}
	
	return new Ex(new Tree.ESEQ(new Tree.SEQ(new Tree.MOVE(
						 new Tree.TEMP(t),
                                                 l.frame.externalCall("allocRecord",
                                                 new Tree.ExpList(
                                                 new Tree.CONST(n * ws), null))),
                                                 s), new Tree.TEMP(t)));
    }
    
    // Criacao de arrays
    // Basicamente, fazer uma chamada a initArray (funcao da biblioteca)
    public Exp arrayExp(Exp e1, Exp e2, Level l) {
	Temp.Temp t = new Temp.Temp();
	Tree.Exp e = 
	    l.frame.externalCall("initArray",
				 new Tree.ExpList(e1.unEx(),
						  new Tree.ExpList(e2.unEx(),
								   null)));

  	return new Ex(new Tree.ESEQ(new Tree.MOVE(new Tree.TEMP(t), e),						  new Tree.TEMP(t)));


//   	return new Ex(l.frame.externalCall("initArray", 
//   					  new Tree.ExpList(e1.unEx(),
//    					  new Tree.ExpList(e2.unEx(), null))));
    }

    public Exp seqExp(ExpList el) {
	ExpList aux;
	Tree.Stm s;
	Tree.Exp e;
	
	if (el == null) { // trata do caso em que a lista e' vazia
	    return new Nx(new Tree.EXP(new Tree.CONST(0)));
	}
	
	if (el.head instanceof Nx) {
	    s = el.head.unNx();
	    for (aux = el.tail; aux != null; aux = aux.tail) {
		s = new Tree.SEQ(aux.head.unNx(), s);
	    }
	    return new Nx(s);
	}
	
	e = el.head.unEx();
	for (aux = el.tail; aux != null; aux = aux.tail) {
	    e = new Tree.ESEQ(aux.head.unNx(), e);
	}
	
	return new Ex(e);
    }
    
    public Exp assignExp(Exp lv, Exp val) {
	return new Nx(new Tree.MOVE(lv.unEx(), val.unEx()));
    }
    
    
    public Exp ifExp(Exp cond, Exp e1, Exp e2) {
	if (e2 == null) e2 = new Nx(NOP().unNx());
	
      	if (e1 instanceof Nx || e2 instanceof Nx) {
      	    return new Nx(new IfThenElseExp(cond, e1, e2).unNx());
      	}
      	else {
  	    return new Ex(new IfThenElseExp(cond, e1, e2).unEx());
      	}
    }
    
    // "Instrucao" que nao faz nada e que nao gera efeitos colaterais
    public Exp NOP() {
	return nilExp();
    }
    
    // for's tambem sao tratados como while's
    public Exp whileExp(Exp test, Exp body, Temp.Label end) {
	Temp.Label t = new Temp.Label();
	Temp.Label a = new Temp.Label();

	return new Nx(new Tree.SEQ(new Tree.LABEL(t),
				   new Tree.SEQ(test.unCx(a, end),
				   new Tree.SEQ(new Tree.LABEL(a),
				   new Tree.SEQ(body.unNx(),
				   new Tree.SEQ(new Tree.JUMP(t),
				   new Tree.LABEL(end)))))));
    }


    public Exp breakExp(Temp.Label end) {
	return new Nx(new Tree.JUMP(end));
    }
    
    
    public Exp letExp(ExpList el, Exp body) {
	Tree.Stm st; // arvore contendo as inicializacoes
	ExpList aux;
	
	if (el != null) { // lista de declaracoes nao e' vazia
	    st = el.head.unNx();
	    
	    for (aux = el.tail; aux != null; aux = aux.tail) {
		st = new Tree.SEQ(aux.head.unNx(), st);
	    }
	}
	else { // lista vazia: gerando NOP
	    st = nilExp().unNx();
	}
	
	if (body instanceof Nx) { // let nao devolve valor
	    return new Nx(new Tree.SEQ(st, body.unNx()));
	}
	else { // let devolve valor
	    return new Ex(new Tree.ESEQ(st, body.unEx()));
	}
    }
    
    // geracao de codigo para uma variavel simples
    public Exp simpleVar(Access a, Level l) {
	// expressao inicial p/ o static link
	Tree.Exp sl = new Tree.TEMP(l.frame.FP()); 
	Level aux = l; // percorre a lista de Level's
	
	// Construimos a "torre" do static link ate' o nivel da
	// variavel -- observe-se que, ao inicio de cada iteracao, sl
	// contem uma expressao que "calcula" o valor do FP do nivel
	// atual.
	
	// FIXME: convencionando-se que o static link esta' sempre no
	// offset 0 do frame atual, podemos usar a constante 0 abaixo.
	while (aux != a.home) {
	    sl = new Tree.MEM(new Tree.BINOP(Tree.BINOP.PLUS, sl, 
					     new Tree.CONST(0)));
	    aux = aux.parent;
	}
	
	return new Ex(a.acc.exp(sl));
    }
    
    public Exp fieldVar(Exp e, int n, Level l) {
	int ws = l.frame.wordSize();

	return new Ex(new Tree.MEM(
				   new Tree.BINOP(Tree.BINOP.PLUS, e.unEx(),
					new Tree.BINOP(Tree.BINOP.MUL,
					new Tree.CONST(n),
					new Tree.CONST(ws)))));
    }

    public Exp subscriptVar(Exp e, Exp index, Level l) {
	int ws = l.frame.wordSize();

	return new Ex(new Tree.MEM(new Tree.BINOP(Tree.BINOP.PLUS, e.unEx(),
				   new Tree.BINOP(Tree.BINOP.MUL,
						  index.unEx(),
						  new Tree.CONST(ws)))));
    }
}
