package Mips;

import Temp.Temp;
import Temp.Label;
import Temp.TempList;

public class MipsFrame extends Frame.Frame {
    int offset; // mantem o offset da primeira posicao nao usada
    // no frame atual (em relacao ao FP)

    /* A arquitetura MIPS possui um total de 32 registradores,
       numerados de 0 a 31 e referenciados como $0 ate' $31.
       Entretanto, a especificacao da plataforma tambem determina
       nomes para esses registradores, que podem ser usados em lugar
       de seus numeros. Por exemplo, $zero em vez de $0. Abaixo segue
       a lista de temporarios reservados para os registradores da
       arquitetura.
       
       Uma possivel mudanca na classe Temp.Temp que seria bem-vinda
       aqui seria a adicao de mais um construtor que recebesse uma
       string como um parametro e que, em vez de guardar t123, como a
       descricao interna do temporario guardasse a string passada como
       parametro. Desta forma, poderiamos dar o seguinte comando:
       
       private final Temp zero = new Temp.Temp("$zero");
       
       e o metodo tempMap simplesmente chamaria o metodo toString dos
       temporarios (por exemplo, zero.toString()) em vez de fazer
       diversos if para determinar o "nome em Assembly" do registrador
       zero. */
    
    private static final Temp zero = new Temp(); // sempre igual a 0
    private static final Temp   at = new Temp(); // reservado
    private static final Temp   v0 = new Temp(); // res. de funcoes
    private static final Temp   v1 = new Temp(); // res. de funcoes
    public  static final Temp   a0 = new Temp(); // arg. de funcoes
    public  static final Temp   a1 = new Temp(); // arg. de funcoes
    public  static final Temp   a2 = new Temp(); // arg. de funcoes
    public  static final Temp   a3 = new Temp(); // arg. de funcoes
    private static final Temp   t0 = new Temp(); // caller save
    private static final Temp   t1 = new Temp(); // caller save
    private static final Temp   t2 = new Temp(); // caller save
    private static final Temp   t3 = new Temp(); // caller save
    private static final Temp   t4 = new Temp(); // caller save
    private static final Temp   t5 = new Temp(); // caller save
    private static final Temp   t6 = new Temp(); // caller save
    private static final Temp   t7 = new Temp(); // caller save
    private static final Temp   t8 = new Temp(); // caller save
    private static final Temp   t9 = new Temp(); // caller save
    private static final Temp   s0 = new Temp(); // callee save
    private static final Temp   s1 = new Temp(); // callee save
    private static final Temp   s2 = new Temp(); // callee save
    private static final Temp   s3 = new Temp(); // callee save
    private static final Temp   s4 = new Temp(); // callee save
    private static final Temp   s5 = new Temp(); // callee save
    private static final Temp   s6 = new Temp(); // callee save
    private static final Temp   s7 = new Temp(); // callee save
    private static final Temp   k0 = new Temp(); // res. p/ o kernel
    private static final Temp   k1 = new Temp(); // res. p/ o kernel
    private static final Temp   gp = new Temp(); // global pointer
    private static final Temp   sp = new Temp(); // stack pointer
    private static final Temp   fp = new Temp(); // frame pointer
    private static final Temp   ra = new Temp(); // return address
    
    // Lista de registradores "especiais" do MIPS:
    // zero, at, v0, v1, k0, k1, gp, sp, fp, ra
    private static final TempList specialregs =
	L(zero, L(at, L(v0, L(v1, L(k0, L(k1, L(gp, L(sp, L(fp, L(ra,
								  null))))))))));
    
    // Lista de registradores de argumentos de funcoes: a[0-3]
    private static final TempList argregs = L(a0, L(a1, L(a2,
							  L(a3, null))));
    
    // Lista de registradores callee-save: s[0-7]
    private static final TempList calleesaves =
	L(s0, L(s1, L(s2, L(s3, L(s4, L(s5, L(s6, L(s7, null))))))));
    
    // Lista de registradores caller-save: t[0-9]
    private static final TempList callersaves =
	L(t0, L(t1, L(t2, L(t3, L(t4, L(t5, L(t6, L(t7,	L(t8, L(t9,
								null))))))))));
    
    // Lista de registradores modificados em chamadas de funcoes:
    // v0, v1, fp, ra, t[0-9]
    private static final TempList calldefs =
	L(ra, L(v0, L(v1, L(argregs, callersaves))));

    // Sugerido pelo livro -- para uso na "liveness analysis"
    // Todos os possiveis registradores vivos ao final de uma funcao
    private static final TempList returnSink = L(specialregs, calleesaves);
    
    private static final TempList allregs = 
	L(zero, L(at, L(v0, L(v1, L(a0, L(a1, L(a2, L(a3, L(t0, L(t1, L(t2,
	L(t3, L(t4, L(t5, L(t6, L(t7, L(s0, L(s1, L(s2, L(s3, L(s4, L(s5,
	L(s6, L(s7, L(t8, L(t9, L(k0, L(k1, L(gp, L(sp, L(fp, L(ra,
	null))))))))))))))))))))))))))))))));
	
    public TempList registers() {
	return allregs;
    }
    
    
    public TempList callerSaves() {
	return callersaves;
    }

    public TempList calleeSaves() {
	return calleesaves;
    }

    public MipsFrame(Label l) {
	name = l;
	offset = 0;
    }
    
    // Sugerido pelo livro -- a implementacao foi movida de
    // Frame.Frame para Mips.MipsFrame pois ela e' dependente de
    // plataforma (depende de Mips.Codegen).
    public Assem.InstrList codegen(Tree.Stm stm) {
	return (new Codegen(this)).codegen(stm);
    }
    
    // Sugerido o pelo livro -- cria lista de Temporarios
    private static TempList L(Temp h, TempList t) {
	return new TempList(h, t);
    }

    // Concatena duas listas de instrucoes em uma so'.
    static Assem.InstrList append(Assem.InstrList a, Assem.InstrList b) {
	if (a == null) {
	    return b;
	}
	else {
	    Assem.InstrList p;
	    for (p = a; p.tail != null; p = p.tail) {}
	    p.tail = b;
	    return a;
	}
    }
    
    // Concatena duas listas de temporarios em uma so'.
    static TempList L(TempList a, TempList b) {
	TempList aux = null, init = null, last = null;
	
	aux = a;
	while (aux != null) {
	    
	    if (init == null) { // se a lista estava vazia
		last = new TempList(aux.head, null);
		init = last; 
	    }
	    else { // se a lista nao estava vazia
		last.tail = new TempList(aux.head, null);
		last = last.tail;
	    }
	    
	    aux = aux.tail; // prossegue na lista "fonte"
	}
	
	aux = b;
	while (aux != null) {
	    
	    if (init == null) { // se a lista estava vazia
		last = new TempList(aux.head, null);
		init = last; 
	    }
	    else { // se a lista nao estava vazia
		last.tail = new TempList(aux.head, null);
		last = last.tail;
	    }
	    
	    aux = aux.tail; // prossegue na lista "fonte"
	}
	
	return init;
    }
    
    // Funcao que mapeia registradores a seus nomes
    //
    // Uma possivel melhoria aqui seria fazer um loop na variavel
    // specialregs tentando encontrar temp e, se encontrado, devolver
    // o valor devolvido por seu metodo toString() -- p.e.
    //
    // Temp i = specialregs;
    // while (i != null && i.head != temp) { i = i.tail; }
    // if (i != null) return i.toString();
    // return null;
    public String tempMap(Temp temp) {
	
	if (temp == zero) return "$zero";
	if (temp == at) return "$at";
	if (temp == v0) return "$v0";
	if (temp == v1) return "$v1";
	if (temp == a0) return "$a0";
	if (temp == a1) return "$a1";
	if (temp == a2) return "$a2";
	if (temp == a3) return "$a3";
	if (temp == t0) return "$t0";
	if (temp == t1) return "$t1";
	if (temp == t2) return "$t2";
	if (temp == t3) return "$t3";
	if (temp == t4) return "$t4";
	if (temp == t5) return "$t5";
	if (temp == t6) return "$t6";
	if (temp == t7) return "$t7";
	if (temp == t8) return "$t8";
	if (temp == t9) return "$t9";
	if (temp == s0) return "$s0";
	if (temp == s1) return "$s1";
	if (temp == s2) return "$s2";
	if (temp == s3) return "$s3";
	if (temp == s4) return "$s4";
	if (temp == s5) return "$s5";
	if (temp == s6) return "$s6";
	if (temp == s7) return "$s7";
	if (temp == k0) return "$k0";
	if (temp == k1) return "$k1";
	if (temp == gp) return "$gp";
	if (temp == sp) return "$sp";
	if (temp == fp) return "$fp";
	if (temp == ra) return "$ra";
	
	return null;
    }
    
    // Inicializa campos de um novo frame
    // FIXME: offset deveria ser inicializado aqui??
    public Frame.Frame newFrame(Label name, Util.BoolList formals) {
	Frame.Frame f = new MipsFrame(name);
	AccessList al, aux;
	Tree.Stm s = null, m = null;
	//offset = 0; // offset deste novo frame
	Temp arg = a0;

	//f.name = name;
	f.formals = makeList(formals);

	return f;
    }
    
    // Cria lista de acessos a partir de lista de booleanos
    // descrevendo se parametros escapam ou nao -- static links nao
    // sao manipulados neste modulo (e, sim, no Translate).
    AccessList makeList(Util.BoolList bl) {
	if (bl == null) {
	    return null;
	}
	else {
	    return new AccessList(allocLocal(bl.head), makeList(bl.tail));
	}
    }
    
    // Devolve um acesso para uma nova variavel, dependendo do
    // parametro escape
    public Frame.Access allocLocal(boolean escape) {
	if (escape) {
	    offset -= wordSize();
	    return new InFrame(offset + wordSize());
	}
	else {
	    return new InReg(new Temp());
	}
    }
    

    // Strings em Tiger sao, na realidade, pares ordenados da forma:
    // (n, s), onde n e o numero de caracteres da string e s e a
    // sequencia de caracteres da string.
    public String string(Label lb, String s) {
	return "\t.data\n" + lb.toString() + ":\n\t.word " + s.length() +
	    "\n\t.asciiz \"" + s + "\"\n";
    }

    
    // Devolve temporario relativo ao FP deste frame.
    public Temp FP() {
	return fp;
    }
    
    // Devolve temporario relativo ao RV da arquitetura
    //
    // Obs.: A arquitetura MIPS determina que dois registradores sao
    // destinados a conter o valor de retorno das funcoes, mas dadas
    // as limitadoes do projeto (todos os argumentos e variaveis com
    // tamanho de uma palavra da arquitetura) estamos apenas usando o
    // registrador $v0 como resultado de funcoes.
    public Temp RV() {
	return v0; // ver observacao acima
    }
    
    // Devolve o tamanho da palavra da arquitetura.
    public int wordSize() {
	final int WS = 4; // tamanho da palavra da arquitetura
	
	return WS;
    }
    
    // Devolve IR correspondente a uma chamada externa `a funcao func
    // FIXME: manter uma tabela contendo as strings/labels, de maneira
    // a evitar realocacoes no futuro.
    public Tree.Exp externalCall(String func, Tree.ExpList args) {
	return new Tree.CALL(new Tree.NAME(new Label(func)), args);
    }

    // Funcao provisoria -- sera' modificada na ultima fase
    public Tree.Stm procEntryExit1(Tree.Stm body) {
	AccessList sl = null, tail = null;
	TempList t = null, l = null;

	TempList calleesaves2 =
	L(a0, L(a1, L(a2, L(a3, L(ra, L(s0, L(s1, L(s2, L(s3, L(s4, L(s5, L(s6, L(s7, null)))))))))))));


	//	L(s7, L(s6, L(s5, L(s4, L(s3, L(s2, L(s1, L(s0, L(ra, L(a3, L(a2, L(a1, L(a0, null))))))))))))); 


	// salvando registradores calleesaves
	for (l = calleesaves2; l != null; l = l.tail) {
	    if (sl == null) {
		tail = new AccessList(allocLocal(true), null);
		sl = tail;
	    }
	    else {
		tail.tail = new AccessList(allocLocal(true), null);
		tail = tail.tail;
	    }

	    body = new Tree.SEQ(new Tree.MOVE(tail.head.exp(new Tree.TEMP(FP())),
					      new Tree.TEMP(l.head)), body);
	}
	
	// restaurando registradores calleesaves
	for (l = calleesaves2; l != null; l = l.tail) {
	    body = new Tree.SEQ(body, 
				new Tree.MOVE(new Tree.TEMP(l.head),
					      sl.head.exp(new Tree.TEMP(FP()))));
	    
	    sl = sl.tail;
	}

	return body;
    }
    
    // Metodo dado pelo livro -- adiciona uma instrucao ficticia ao
    // final de uma sequencia de instrucoes, para efeitos de "liveness
    // analysis".
    public Assem.InstrList procEntryExit2(Assem.InstrList body) {
	return append(body,
		      new Assem.InstrList(new
					  Assem.OPER("", null,
						     returnSink), null));
    }
    
    // Metodo dado pelo livro -- FIXME: o que e' Frame.Proc?
    public Frame.Proc procEntryExit3(Assem.InstrList body) {
	Assem.InstrList p = 
	    new Assem.InstrList(
				new Assem.LABEL(name.toString() + ":\n", name), 
				new Assem.InstrList(
				       new Assem.OPER("\tmove `d0, `s0\n",
						      L(fp, null), L(sp, null)), 
				       null));

	Assem.OPER e = new Assem.OPER("\tjr `s0\n", null, L(ra, null));
	Assem.InstrList new_body = append(p, body);

	new_body = append(new_body, new Assem.InstrList(e, null));

	return new Frame.Proc("\t.text\n", new_body, "\n");
    }
    
}

class AccessList extends Frame.AccessList {
    Frame.Access head;
    AccessList tail;
    
    // Construtor
    public AccessList(Frame.Access h, AccessList t) {
	head = h;
	tail = t;
    }
}


class InFrame extends Frame.Access {
    public int offset;

    // Construtor -- recebe o offset da variavel no stack frame como
    // parametro.
    public InFrame(int o) {
	offset = o;
    }

    // Devolve IR do acesso atual, a partir de framePtr e do offset
    // contido na classe, ou seja, calcula-se M[framePtr + offset].
    public Tree.Exp exp(Tree.Exp framePtr) {
	// ver discussao no livro sobre l-values na pagina 167
	return new Tree.MEM(new Tree.BINOP(Tree.BINOP.PLUS, framePtr,
					   new Tree.CONST(offset)));
    }
}

// Neste projeto esta classe nao sera' utilizada, pois todas as
// variaveis sao alocadas em memoria (frame), em vez de registradores.
class InReg extends Frame.Access {
    Temp temp;

    // Construtor -- recebe o temporario como parametro.
    public InReg(Temp t) {
	temp = t;
    }

    // Devolve IR do acesso atual, a partir de framePtr (que e'
    // ignorado).
    public Tree.Exp exp(Tree.Exp framePtr) {
	return new Tree.TEMP(temp);
    }
}
