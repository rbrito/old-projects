package Translate;


// Lista ligada de Exp's (para argumentos de funcoes e expressoes que
// inicializam records).
public class ExpList{
    Exp head;
    ExpList tail;
    
    public ExpList(Exp h, ExpList t){
	head=h; tail=t;
    }
}
