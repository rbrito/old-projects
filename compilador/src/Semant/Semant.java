package Semant;

import Translate.*;
import Types.*;

public class Semant { // unica classe publica no pacote Semant
    Env env; // tabelas de tipos e de valores
    Level level; // apontador para o nivel atual
    Translate trans; // estrutura de dados para geracao da IR

    // construtores
    public Semant(ErrorMsg.ErrorMsg err, Level lv, Translate t) {
        this(new Env(err, lv), lv, t);
    }

    Semant(Env e, Level l, Translate t) {
        env = e;
        level = l;
        trans = t;
    }

    int loop = 0; // controle para breaks

    // funcoes axiliares
    Exp checkInt(ExpTy et, int pos) {
        if (!(et.ty.actual() == env.INT))
            env.errorMsg.error(pos, "Erro: tipo inteiro obrigatorio.");

        return et.exp;
    }

    void FIXME(String s) {
        System.out.println(" *** " + s);
    }

    public Frag transProg(Absyn.Exp e) {
        ExpTy r = transExp(e, null); // FIXME: null esta' correto???
        trans.procEntryExit(level, r.exp);
        return trans.getResult();
    }

    // tratamento de expressoes
    ExpTy transExp(Absyn.OpExp e, Temp.Label breakl) {
        ExpTy aux1, aux2;
        Types.Type aux1t, aux2t;

        aux1 = transExp(e.left, breakl);
        aux2 = transExp(e.right, breakl);
        aux1t = aux1.ty.actual(); // tipo do operador da esquerda
        aux2t = aux2.ty.actual(); // tipo do operador da direita

        switch (e.oper) {

        case Absyn.OpExp.PLUS: // operacoes apenas com inteiros
        case Absyn.OpExp.MINUS:
        case Absyn.OpExp.MUL:
        case Absyn.OpExp.DIV:
            checkInt(aux1, e.left.pos);
            checkInt(aux2, e.right.pos);

            return new ExpTy(trans.aritExp(e.oper, aux1.exp, aux2.exp), env.INT);

        case Absyn.OpExp.EQ: // compara inteiros, strings, records e arrays
        case Absyn.OpExp.NE:

            // testando casos validos
            if ((aux1t == env.INT && aux2t == env.INT)
                    || (aux1t instanceof Types.ARRAY && aux2t instanceof Types.ARRAY)
                    || (aux1t instanceof Types.RECORD && aux2t == env.NIL)
                    || (aux1t == env.NIL && aux2t instanceof Types.RECORD)
                    || (aux1t instanceof Types.RECORD && aux1t == aux2t)) {

                return new ExpTy(trans.relExp(e.oper, aux1.exp, aux2.exp),
                        env.INT);
            }

            // testando comparacoes de strings
            if (aux1t == env.STRING && aux2t == env.STRING) {

                return new ExpTy(trans.strcmpExp(e.oper, aux1.exp, aux2.exp,
                        level), env.INT);
            }

            env.errorMsg.error(e.pos, "Erro: operandos de tipos"
                    + " incompativeis.");

            return new ExpTy(trans.nilExp(), env.INT);

        case Absyn.OpExp.LT: // compara inteiros e strings
        case Absyn.OpExp.LE:
        case Absyn.OpExp.GT:
        case Absyn.OpExp.GE:

            if (!((aux1t == env.INT && aux2t == env.INT) || (aux1t == env.STRING && aux2t == env.STRING))) {
            }

            if (aux1t == env.INT && aux2t == env.INT) {
                return new ExpTy(trans.relExp(e.oper, aux1.exp, aux2.exp),
                        env.INT);
            }
            if (aux1t == env.STRING && aux2t == env.STRING) {
                return new ExpTy(trans.strcmpExp(e.oper, aux1.exp, aux2.exp,
                        level), env.INT);
            }
            env.errorMsg.error(e.pos, "Erro: operandos nao podem ser"
                    + " comparados.");
            return new ExpTy(trans.nilExp(), env.INT);

        default:
            throw new Error("transExp: operador invalido.");
        }
    }

    // Devolve o tipo da variavel
    ExpTy transExp(Absyn.VarExp e, Temp.Label breakl) {
        return transVar(e.var);
    }

    // Devolve nil
    ExpTy transExp(Absyn.NilExp e, Temp.Label breakl) {
        return new ExpTy(trans.nilExp(), env.NIL);
    }

    // Devolve tipo inteiro
    ExpTy transExp(Absyn.IntExp e, Temp.Label breakl) {
        return new ExpTy(trans.intExp(e.value), env.INT);
    }

    // Devolve tipo string
    ExpTy transExp(Absyn.StringExp e, Temp.Label breakl) {
        return new ExpTy(trans.stringExp(e.value), env.STRING);
    }

    ExpTy transExp(Absyn.CallExp e, Temp.Label breakl) {
        // verifica parametro por parametro se o argumento passado para a funcao
        // tem o mesmo tipo que o parametro da funcao e devolve o tipo de
        // retorno
        // da funcao

        ExpList elist = null;

        Entry s = (Entry) env.venv.get(e.func); // pega a entrada na tabela venv
        if (s instanceof FunEntry) {

            // testar o tipo dos campos

            Absyn.ExpList args = e.args; // argumentos passados
            Types.RECORD esp = ((FunEntry) s).formals; // argumentos esperados
            ExpTy aux;

            while ((args != null) && (esp != null)) {
                aux = transExp(args.head, breakl); // traduz um argumento

                elist = new ExpList(aux.exp, elist);

                // testar se o tipo do argumento e' igual ao tipo esperado
                // ou se o argumento passado e' nil e o argumento esperado
                // e' um record (o esperado nunca e' nil)
                if ((aux.ty.actual() == esp.fieldType.actual())
                        || (aux.ty.actual() == env.NIL && esp.fieldType
                                .actual() instanceof RECORD)) {
                    args = args.tail; // processa proximo argumento
                    esp = esp.tail;
                } else {
                    env.errorMsg.error(e.pos,
                            "Erro: tipo incorreto de argumento " + "a funcao.");
                    return new ExpTy(trans.nilExp(), env.INT);
                }
            }

            if ((args == null) && (esp == null)) { // numero correto de
                                                   // argumentos
                return new ExpTy(trans.callExp(((FunEntry) s).label, elist,
                        level), ((FunEntry) s).result);
            } else { // alguma lista acabou antes da outra
                env.errorMsg.error(e.pos,
                        "Erro: numero incorreto de argumentos.");
                return new ExpTy(trans.nilExp(), env.INT);
            }
        } else { // s nao e' entrada de funcao na tabela de simbolos
            if (s instanceof VarEntry) {
                env.errorMsg.error(e.pos,
                        "Erro: variavel <" + e.func.toString()
                                + "> usada como funcao.");
            } else {
                env.errorMsg.error(e.pos, "Erro: funcao <" + e.func.toString()
                        + "> nao definida.");
            }
            return new ExpTy(trans.nilExp(), env.INT);
        }
    }

    // Traduz records
    ExpTy transExp(Absyn.RecordExp e, Temp.Label breakl) {
        Absyn.FieldExpList f;
        ExpTy et;
        Types.Type t;
        Types.RECORD taux;
        ExpList elist = null;
        int n = 0;

        // procuramos o tipo do registro na tabela de tipos
        t = (Types.Type) env.tenv.get(e.typ);

        if ((t == null) || !(t.actual() instanceof Types.RECORD)) {
            env.errorMsg.error(e.pos, "Erro: tipo <" + e.typ.toString()
                    + "> nao definido.");
            return new ExpTy(trans.nilExp(), env.INT);
        }

        f = e.fields; // f aponta para a lista de expressoes
        taux = (Types.RECORD) t.actual(); // t aponta para a lista de campos

        // verificamos se cada expressao tem o mesmo tipo que o campo do
        // registro
        while ((f != null) && (taux != null)) {
            et = transExp(f.init, breakl); // traduzimos a exp. de inicializacao

            if ((et.ty).actual() != taux.fieldType.actual()
                    && (taux.fieldType.actual() instanceof Types.RECORD && (et.ty)
                            .actual() != env.NIL)) {
                env.errorMsg.error(e.pos,
                        "Erro: expressao de tipo incompativel "
                                + "atribuida ao campo <" + f.name.toString()
                                + ">.");
                return new ExpTy(trans.nilExp(), env.INT);
            }

            f = f.tail;
            taux = taux.tail;

            elist = new ExpList(et.exp, elist);
            n++;
        }

        // se alguma lista acabou antes da outra...
        if ((f != null) || (taux != null)) {
            env.errorMsg.error(e.pos, "Erro: numero incorreto de campos.");
            return new ExpTy(trans.nilExp(), env.INT);
        }

        // todas expressoes corretamente checadas aqui
        return new ExpTy(trans.recordExp(n, elist, level),
                (Types.RECORD) t.actual());
    }

    // Cria expressao de inicializacao de arrays
    ExpTy transExp(Absyn.ArrayExp e, Temp.Label breakl) {
        Types.Type t;
        Types.Type tipoElemento;
        ExpTy size;
        ExpTy init;

        size = transExp(e.size, breakl); // sera' usada na proxima fase
        init = transExp(e.init, breakl); // idem

        // apenas size precisa ser inteiro -- init pode ser record, p.e.
        checkInt(size, e.pos);

        // vetor de tipo t
        t = (Types.Type) env.tenv.get(e.typ);
        if ((t == null) || !(t.actual() instanceof Types.ARRAY)) {
            env.errorMsg.error(e.pos, "Erro: <" + e.typ.toString()
                    + "> nao e' tipo de array.");
            return new ExpTy(trans.nilExp(), env.INT);
        }

        if (init.ty == null) {
            // FIXME
            env.errorMsg.error(e.pos, "Erro: tipo de inicializacao de array "
                    + "incorreto.");
            return new ExpTy(trans.nilExp(), env.INT);

        }

        tipoElemento = ((Types.ARRAY) t.actual()).element;

        if ((init.ty.actual() != tipoElemento.actual())
                && !(init.ty.actual() instanceof Types.NIL && tipoElemento
                        .actual() instanceof Types.RECORD)) {
            env.errorMsg.error(e.pos, "Erro: tipos de expressao e "
                    + "elemento de array diferem.");
            return new ExpTy(trans.nilExp(), env.INT);
        }

        return new ExpTy(trans.arrayExp(size.exp, init.exp, level), t.actual());
    }

    // Traduz sequencia de expressoes
    ExpTy transExp(Absyn.SeqExp e, Temp.Label breakl) {
        Absyn.ExpList el;
        ExpTy last;
        ExpList elist = null;

        el = e.list;
        last = null; // "valor" da ultima expressao avaliada

        while (el != null) {
            last = transExp(el.head, breakl); // avalia a expressao corrente
            el = el.tail; // segue na lista ligada
            elist = new ExpList(last.exp, elist);
        }

        // se nao entrou no laco acima (lista de expressoes vazia), last ==
        // null
        if (last != null) {
            return new ExpTy(trans.seqExp(elist), last.ty.actual());
        } else {
            return new ExpTy(trans.seqExp(elist), env.VOID);
        }
    }

    // Traduz uma atribuicao
    ExpTy transExp(Absyn.AssignExp e, Temp.Label breakl) {
        ExpTy expr = transExp(e.exp, breakl);
        ExpTy var = transVar(e.var);

        if (var.ty.actual() != expr.ty.actual()
                && !(expr.ty.actual() instanceof Types.NIL && var.ty.actual() instanceof Types.RECORD)) {
            env.errorMsg.error(e.pos, "Erro: expressao e variavel de tipos "
                    + "incompativeis.");

            return new ExpTy(trans.nilExp(), env.INT);
        }

        return new ExpTy(trans.assignExp(var.exp, expr.exp), env.VOID);
    }

    // Traduz uma expressao if-then-else
    ExpTy transExp(Absyn.IfExp e, Temp.Label breakl) {
        ExpTy cond = transExp(e.test, breakl); // traduz a condicao
        checkInt(cond, e.test.pos);

        ExpTy et1 = transExp(e.thenclause, breakl); // traduz o then

        if (e.elseclause == null) {
            // O apendice diz: "the entire if-exp produces no value"
            if (et1.ty.actual() != env.VOID) {
                env.errorMsg.error(e.pos,
                        "Erro: if-exp nao pode produzir valor.");

                return new ExpTy(trans.nilExp(), env.VOID);
            }

            return new ExpTy(trans.ifExp(cond.exp, et1.exp, null), env.VOID);
        } else {
            ExpTy et2 = transExp(e.elseclause, breakl); // traduz o else

            if ((et1.ty.actual() != et2.ty.actual())
                    && !(et1.ty.actual() instanceof Types.NIL && et2.ty
                            .actual() instanceof Types.RECORD)
                    && !(et1.ty.actual() instanceof Types.RECORD && et2.ty
                            .actual() instanceof Types.NIL)) {

                env.errorMsg.error(e.elseclause.pos, "<then> e <else> clauses "
                        + "precisam ter o mesmo tipo.");

                return new ExpTy(trans.nilExp(), env.INT);
            }

            return new ExpTy(trans.ifExp(cond.exp, et1.exp, et2.exp), et1.ty);
        }
    }

    // o FOR sera' transformado em WHILE
    ExpTy transExp(Absyn.ForExp e, Temp.Label breakl) {

        // declaracao da variavel
        Absyn.DecList newdec = new Absyn.DecList(e.var, null);

        Absyn.Exp varexp = new Absyn.VarExp(e.pos, new Absyn.SimpleVar(e.pos,
                e.var.name));

        // novo while
        Absyn.Exp newtest = new Absyn.OpExp(e.pos, varexp, Absyn.OpExp.LE, e.hi);

        Absyn.ExpList newbody = new Absyn.ExpList(e.body, new Absyn.ExpList(
                new Absyn.AssignExp(e.pos, new Absyn.SimpleVar(e.pos,
                        e.var.name), new Absyn.OpExp(e.pos, varexp,
                        Absyn.OpExp.PLUS, new Absyn.IntExp(e.pos, 1))), null));

        Absyn.Exp newwhile = new Absyn.WhileExp(e.pos, newtest,
                new Absyn.SeqExp(e.pos, newbody));

        return transExp(new Absyn.LetExp(e.pos, newdec, newwhile), breakl);
    }

    // Geracao de codigo para um while -- o label recebido e'
    // simplesmente desconsiderado (criamos um novo label neste
    // procedimento para onde um break deve pular.
    ExpTy transExp(Absyn.WhileExp e, Temp.Label breakl) {
        ExpTy test, body;
        Temp.Label end;

        test = transExp(e.test, breakl);

        checkInt(test, e.test.pos);

        loop++;
        end = new Temp.Label(); // final do while -- break deve pular para la'

        body = transExp(e.body, end);

        // "... If the result is nonzero, then exp2 (which must
        // produce no value) is executed..."
        if (body.ty.actual() != env.VOID) {
            env.errorMsg.error(e.pos,
                    "Erro: corpo do laco nao pode produzir valor.");
            return new ExpTy(trans.nilExp(), env.VOID);
        }

        loop--;

        return new ExpTy(trans.whileExp(test.exp, body.exp, end), env.VOID);
    }

    ExpTy transExp(Absyn.BreakExp e, Temp.Label breakl) {

        if (loop <= 0) { // erro -- deveria estar dentro de um loop
            env.errorMsg.error(e.pos, "Erro: break fora de laco.");
            return new ExpTy(trans.nilExp(), env.INT);
        }

        return new ExpTy(trans.breakExp(breakl), env.VOID);
    }

    ExpTy transExp(Absyn.LetExp e, Temp.Label breakl) {
        ExpList elist = null;

        // inicia novo escopo para as definicoes do let
        env.venv.beginScope();
        env.tenv.beginScope();

        // processa as definicoes deste let
        for (Absyn.DecList p = e.decs; p != null; p = p.tail) {
            Exp aux = transDec(p.head); // FIXME: e' para corrigir algo???

            if (aux == null)
                throw new Error(
                        "Erro interno: aux == null em transExp(LetExp).");

            elist = new ExpList(aux, elist);
        }

        // Se nao ha' declaracoes, criar uma ExpList contendo apenas
        // uma instrucao que nao faz nada.
        if (elist == null) {
            elist = new ExpList(trans.nilExp(), null);
        }

        // processa o corpo do let
        ExpTy et = transExp(e.body, breakl);

        // finaliza escopo do let
        env.venv.endScope();
        env.tenv.endScope();

        return new ExpTy(trans.letExp(elist, et.exp), et.ty);
    }

    // tratamento de expressoes
    ExpTy transExp(Absyn.Exp e, Temp.Label breakl) {
        if (e instanceof Absyn.VarExp)
            return transExp((Absyn.VarExp) e, breakl);
        if (e instanceof Absyn.NilExp)
            return transExp((Absyn.NilExp) e, breakl);
        if (e instanceof Absyn.IntExp)
            return transExp((Absyn.IntExp) e, breakl);
        if (e instanceof Absyn.StringExp)
            return transExp((Absyn.StringExp) e, breakl);
        if (e instanceof Absyn.CallExp)
            return transExp((Absyn.CallExp) e, breakl);
        if (e instanceof Absyn.OpExp)
            return transExp((Absyn.OpExp) e, breakl);
        if (e instanceof Absyn.RecordExp)
            return transExp((Absyn.RecordExp) e, breakl);
        if (e instanceof Absyn.SeqExp)
            return transExp((Absyn.SeqExp) e, breakl);
        if (e instanceof Absyn.AssignExp)
            return transExp((Absyn.AssignExp) e, breakl);
        if (e instanceof Absyn.IfExp)
            return transExp((Absyn.IfExp) e, breakl);
        if (e instanceof Absyn.WhileExp)
            return transExp((Absyn.WhileExp) e, breakl);
        if (e instanceof Absyn.ForExp)
            return transExp((Absyn.ForExp) e, breakl);
        if (e instanceof Absyn.BreakExp)
            return transExp((Absyn.BreakExp) e, breakl);
        if (e instanceof Absyn.LetExp)
            return transExp((Absyn.LetExp) e, breakl);
        if (e instanceof Absyn.ArrayExp)
            return transExp((Absyn.ArrayExp) e, breakl);
        throw new Error("transExp");
    }

    // tratamento de variaveis
    ExpTy transVar(Absyn.Var v) {
        if (v instanceof Absyn.SimpleVar)
            return transVar((Absyn.SimpleVar) v);
        if (v instanceof Absyn.FieldVar)
            return transVar((Absyn.FieldVar) v);
        if (v instanceof Absyn.SubscriptVar)
            return transVar((Absyn.SubscriptVar) v);
        throw new Error("transVar");
    }

    ExpTy transVar(Absyn.SimpleVar v) { // variavel simples
        // verificamos a entrada em venv relativa a v.name
        Entry s = (Entry) env.venv.get(v.name);

        // verificamos se o tipo de entrada e' de uma variavel (pode ser de
        // funcao)
        if (s instanceof VarEntry) {// s e' entrada de uma variavel
            return new ExpTy(trans.simpleVar(((VarEntry) s).access, level),
                    ((VarEntry) s).ty);
        }

        if (s instanceof FunEntry) { // v.name e' uma funcao
            env.errorMsg.error(v.pos, "Erro: <" + v.name.toString()
                    + "> e' funcao.");
        } else { // v.name nem funcao e' -- e' null?
            env.errorMsg.error(v.pos, "Erro: variavel <" + v.name.toString()
                    + "> nao definida.");
        }

        // se houve algum erro, simplesmente devolva uma resposta
        return new ExpTy(trans.nilExp(), env.INT);
    }

    ExpTy transVar(Absyn.FieldVar v) { // variavel do tipo a.x[5][3].b
        int n = 0;

        // verificamos se v.var e' record
        ExpTy et = transVar(v.var);

        if (et.ty.actual() instanceof Types.RECORD) {
            Types.RECORD aux = (Types.RECORD) et.ty.actual();

            // percorrendo a lista ligada
            while ((aux != null) && (aux.fieldName != v.field)) {
                aux = aux.tail;
                n++;
            }

            if (aux != null) { // encontrou o campo
                return new ExpTy(trans.fieldVar(et.exp, n, level),
                        aux.fieldType.actual());
            } else { // nao encontrou o campo no record
                env.errorMsg.error(v.pos, "Erro: variavel sem campo.");
                return new ExpTy(trans.nilExp(), env.INT);
            }
        }

        // nao existe este campo na variavel
        env.errorMsg.error(v.pos,
                "Erro: variavel sem campo <" + v.field.toString() + ">.");
        return new ExpTy(trans.nilExp(), env.INT);
    }

    ExpTy transVar(Absyn.SubscriptVar v) { // variavel do tipo a[exp]
        ExpTy et = transVar(v.var); // procuramos pelo tipo de v.var

        if (et.ty instanceof Types.ARRAY) {

            // FIXME: null esta' correto abaixo???
            ExpTy index = transExp(v.index, null); // v.index deve ser inteiro

            if (!(index.ty.actual() == env.INT)) {
                env.errorMsg.error(v.pos,
                        "Erro: expressao inteira obrigatoria.");
                return new ExpTy(trans.nilExp(), env.INT);
            }

            // e' array e indice e' inteiro
            return new ExpTy(trans.subscriptVar(et.exp, index.exp, level),
                    ((Types.ARRAY) et.ty).element);
        }

        // se houve erro
        env.errorMsg.error(v.pos, "Erro: variavel nao e' do tipo array.");
        return new ExpTy(trans.nilExp(), env.INT);
    }

    Exp transDec(Absyn.Dec d) {
        if (d instanceof Absyn.FunctionDec)
            return transDec((Absyn.FunctionDec) d);
        if (d instanceof Absyn.VarDec)
            return transDec((Absyn.VarDec) d);
        if (d instanceof Absyn.TypeDec)
            return transDec((Absyn.TypeDec) d);
        throw new Error("transDec");
    }

    Exp transDec(Absyn.VarDec d) {
        ExpTy init = transExp(d.init, null); // FIXME: null e' correto??
        Type tipo;
        VarEntry v;

        if (d.typ == null) { // declaracao com tipo implicito
            if (init.ty.actual() == env.NIL) {
                env.errorMsg.error(d.pos,
                        "Erro: expressao com valor <nil> usada "
                                + "para inicializar var. com tipo implicito.");

                return null;
            } else {
                v = new VarEntry(level.allocLocal(d.escape), init.ty);
                env.venv.put(d.name, v);
                // FIXME: verificar os parametros de varAlloc
                return trans.varAlloc(v.access, init.exp, level);
            }
        } else { // declaracao com tipo explicito
            tipo = transTy(d.typ); // FIXME: incluir aqui o breakl????

            if ((tipo instanceof Types.NAME) && ((Types.NAME) tipo).isLoop()) {
                env.errorMsg.error(d.pos,
                        "Erro: o tipo <" + d.typ.name.toString() + "> de <"
                                + d.name.toString() + "> contem ciclo.");
                return null; // para nao continuar definindo variavel
            }

            if ((init.ty.actual() == env.NIL)
                    && !(tipo.actual() instanceof Types.RECORD)) {
                env.errorMsg.error(d.pos, "Erro: expressao com valor <nil> so "
                        + "pode inicializar var. de tipo record.");
                return null;
            }

            if (tipo.actual() == init.ty.actual() || init.ty == env.NIL) {
                v = new VarEntry(level.allocLocal(d.escape), tipo.actual());
                env.venv.put(d.name, v);
                return trans.varAlloc(v.access, init.exp, level);
            } else { // tipos diferentes
                env.errorMsg.error(d.pos, "Erro: variavel e expressao de "
                        + "inicializacao de tipos incompativeis.");
                return null;
            }
        }
    }

    // Funcao auxiliar para criar lista de booleanos conforme lista de
    // escapes
    private Util.BoolList makeParamList(Absyn.FieldList p) {
        if (p == null) {
            return null;
        } else {
            return new Util.BoolList(p.escape, makeParamList(p.tail));
        }
    }

    Exp transDec(Absyn.FunctionDec d) {
        Absyn.FunctionDec f;
        Types.RECORD formals;
        Absyn.FieldList p;
        Types.Type result;
        Types.Type aux;

        Level lv = null;
        Temp.Label lb;
        Util.BoolList bl = null;
        ExpTy body;

        // primeiro passo
        f = d;
        while (f != null) {
            if (f.result == null) { // procedimento
                result = env.VOID;
            } else { // funcao
                result = transTy(f.result);
            }

            // codigo comum
            formals = transTypeFields(f.params);

            // criar BoolList com o mesmo numero de parametros de formals
            // em principio, todos `escapam'
            bl = makeParamList(f.params);

            lb = new Temp.Label();
            lv = new Level(level, lb, bl);

            env.venv.put(f.name, new FunEntry(lv, lb, formals, result.actual()));

            f = f.next;
        }

        // segundo passo
        f = d;
        while (f != null) {
            env.venv.beginScope();

            if (f.result == null) { // procedimento
                result = env.VOID;
            } else { // funcao
                result = transTy(f.result);
            }

            // checagem dos parametros
            for (p = f.params; p != null; p = p.tail) {
                aux = (Types.Type) env.tenv.get(p.typ);
                env.venv.put(p.name, new VarEntry(level.allocLocal(p.escape),
                        aux.actual()));
            }

            FunEntry fe = (FunEntry) env.venv.get(f.name);
            // newsem sera' usado para processar o corpo da funcao
            Semant newsem = new Semant(env, fe.level, trans);
            // traduz o corpo da funcao para a IR
            body = newsem.transExp(f.body, null); // FIXME: null e' correto??

            if (f.result != null) {
                if (body.ty.actual() != result.actual()) {
                    env.errorMsg.error(f.pos,
                            "Erro: Tipos declarado e devolvido"
                                    + " incompativeis em funcao.");
                }
            }

            // termina o processamento do corpo da funcao
            // FIXME: verificar o level
            trans.procEntryExit(fe.level, trans.funExp(body.exp));

            env.venv.endScope();
            f = f.next;
        }

        // "Codigo" para a declaracao da funcao (nao e' o corpo). O
        // corpo fica em um fragmento especifico.
        return trans.nilExp();
    }

    Exp transDec(Absyn.TypeDec d) {
        Absyn.TypeDec lista;
        Types.Type aux;
        Types.NAME auxty;

        // primeiro passo
        lista = d;
        while (lista != null) {
            env.tenv.put(lista.name, new Types.NAME(lista.name));
            lista = lista.next;
        }

        // segundo passo
        lista = d;
        while (lista != null) {
            aux = transTy(lista.ty);

            auxty = (Types.NAME) env.tenv.get(lista.name);
            auxty.bind(aux);

            lista = lista.next;
        }

        if (((Types.NAME) env.tenv.get(d.name)).isLoop()) {
            env.errorMsg.error(d.pos,
                    "Advertencia: Definicao de <" + d.name.toString()
                            + "> contem ciclo.");
        }

        return trans.nilExp(); // "codigo" para declaracao de tipo
    }

    Type transTy(Absyn.Ty t) {
        if (t instanceof Absyn.NameTy)
            return transTy((Absyn.NameTy) t);
        if (t instanceof Absyn.RecordTy)
            return transTy((Absyn.RecordTy) t);
        if (t instanceof Absyn.ArrayTy)
            return transTy((Absyn.ArrayTy) t);
        throw new Error("transTy");
    }

    Type transTy(Absyn.NameTy t) {
        Type tipo = (Type) env.tenv.get(t.name);

        if (tipo == null) {
            env.errorMsg.error(t.pos, "Erro: tipo <" + t.name.toString()
                    + "> nao definido.");
            return env.INT;
        }

        return tipo;
    }

    RECORD transTypeFields(Absyn.FieldList f) {
        if (f == null) {
            return null;
        } else {
            Type t = (Type) env.tenv.get(f.typ);

            if (t == null) {
                env.errorMsg.error(f.pos, "Erro: tipo <" + f.typ.toString()
                        + "> nao definido.");
                return null;
            }

            return new RECORD(f.name, t, transTypeFields(f.tail));
        }
    }

    Type transTy(Absyn.RecordTy t) {

        return transTypeFields(t.fields);
    }

    Type transTy(Absyn.ArrayTy t) {
        Type tipo = (Type) env.tenv.get(t.typ);

        if (tipo == null) {
            env.errorMsg.error(t.pos, "Erro: tipo <" + t.typ.toString()
                    + "> nao definido.");
            return null;
        }

        return new ARRAY(tipo);
    }
}

class ExpTy {
    Exp exp;
    Type ty;

    ExpTy(Exp e, Type t) {
        exp = e;
        ty = t;
    }
}
