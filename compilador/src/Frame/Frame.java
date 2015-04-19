package Frame;

import Temp.Temp;
import Temp.Label;
import Temp.TempMap; // por algum motivo, e' necessario importar isto
import Temp.TempList;

public abstract class Frame implements TempMap {
    abstract public Frame newFrame(Label name, Util.BoolList formals);

    public Label name;
    public AccessList formals;

    abstract public Access allocLocal(boolean escape);

    // adicionados na fase do cap. 7

    abstract public Temp FP(); // frame pointer da arquitetura

    abstract public int wordSize(); // tamanho da palavra na arquitetura

    abstract public Temp RV(); // registrador que contem return value de funcoes

    abstract public Tree.Exp externalCall(String func, // chamadas externas
            Tree.ExpList args);

    abstract public Tree.Stm procEntryExit1(Tree.Stm body);

    abstract public String string(Label lb, String s);

    // adicionados na fase do cap. 9

    abstract public String tempMap(Temp temp); // mapeia Temp's -> registr.

    abstract public Assem.InstrList procEntryExit2(Assem.InstrList body);

    abstract public Proc procEntryExit3(Assem.InstrList body);

    // O livro implementa esta funcao neste modulo, mas ela e'
    // dependente de plataforma. Por este motivo, deixamos esta funcao
    // como abstrata.
    abstract public Assem.InstrList codegen(Tree.Stm stm);

    // adicionados na fase do cap. 12
    abstract public TempList registers();

    abstract public TempList callerSaves();

    abstract public TempList calleeSaves();
}
