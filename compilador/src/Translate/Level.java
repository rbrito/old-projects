package Translate;

import Temp.Label;

public class Level {
    Level parent; // nivel anterior (ponteiro da lista ligada)
    Frame.Frame frame; // frame do nivel atual
    public AccessList formals; // lista de acessos
    
    
    // construtor -- recebe o nivel anterior, o label da nova funcao e
    // uma lista de booleanos indicando os parametros que "escapam" ou
    // nao
    public Level(Level p, Label label, Util.BoolList fmls) {
	// preenchemos a lista ligada de levels com o level anterior
	parent = p;

	// alocamos o novo frame
	//frame = new Mips.MipsFrame(label);
	
	// inicializamos o frame com um parametro a mais, o static
	// link, que sempre "escapa".
	frame = p.frame.newFrame(label, new Util.BoolList(true, fmls));
    }
    
    // construtor alternativo para o frame da "funcao principal", que
    // contem todo o programa Tiger
    public Level(Frame.Frame f) {
	frame = f;
    }
    
    // Alocando variavel local no nivel corrente, com metodo de acesso
    // dado por escape
    public Access allocLocal(boolean escape) {
	return new Access(this, frame.allocLocal(escape));
    }
}
