package Translate;

// Classe que estende a classe Frag e que implementa fragmentos de
// programas.

public class ProcFrag extends Frag {
    public Frame.Frame frame;
    public Tree.Stm body;
    
    public ProcFrag(Tree.Stm b, Frame.Frame f){
	body = b; 
	frame = f;
    }
}
