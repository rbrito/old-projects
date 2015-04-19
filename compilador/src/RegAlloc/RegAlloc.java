package RegAlloc;

import Assem.*;
import Frame.*;
import Graph.*;
import FlowGraph.*;
import Set.*;
import Temp.*;
import java.util.*;

/***********************************************************************************************************
 * CLASSE RegAlloc *
 * ------------------------------------------------------------
 * ------------------------------------------- * Realiza a alocação de
 * registradores para cada temporário. Para isso, faz uma K-coloração do grafo
 * de * interferências (onde K é o número de registradores disponíveis na
 * máquina destino). *
 ***********************************************************************************************************/

public class RegAlloc implements TempMap {
    private InterferenceGraph ig; // grafo de interferências
    private int K; // número de registradores disponíveis (para fazer
                   // K-coloração)

    private Frame frame; // frame
    private InstrList il; // instruções assembly sendo analisadas

    // as seguintes estruturas de dados representarão, através de conjuntos e
    // tabelas de hash, o grafo de
    // interferências (de agora em diante, grafo refere-se a todos estas
    // estruturas e grafo de interferências
    // ao grafo gerado durante a análise de liveness)
    private Dictionary color; // associa cada temporário a uma cor (registrador)
    private Dictionary alias; // associa dois temporários em MOVES que serão
                              // eliminados
    private Dictionary degree; // associa a cada temporário do grafo o seu grau
    private OrderedSet selectStack; // pilha de temporários para futura
                                    // coloração
    private TempSet
    // estes 9 conjuntos são disjuntos
            precolored,
            initial, simplifyWorklist, freezeWorklist,
            spillWorklist,
            spilledNodes, coalescedNodes, coloredNodes, selectStackCopy,
            // estes outros conjuntos também guardam temporários
            newTemps, fetchesTemp, calleeSave, callerSave;
    private PairOrderedSet
    // estes conjuntos são disjuntos e guardam MOVES
            coalescedMoves,
            constrainedMoves, frozenMoves, worklistMoves, activeMoves,
            // este conjunto representa o grafo de interferências
            adjSet;
    private Dictionary moveList, // associa cada temporário ao conjunto de MOVES
                                 // ao qual está relacionado
            adjList; // associa cada temporário ao conjunto de temporários com
                     // os quais interfere

    /*
     * Strring tempMap (Temp t) Procura o nome do registrador (cor) associado ao
     * temporário t.
     */
    public String tempMap(Temp t) {
        Temp c = (Temp) color.get(t);
        if (c == null)
            return null;
        return frame.tempMap(c);
    }

    /*
     * int Degree (Temp t) obtém o grau do temporário t.
     */
    private int Degree(Temp t) {
        if (precolored.has(t))
            return (1 << 30); // grau de temporários pré-coloridos é considerado
                              // infinito
        Integer i = (Integer) degree.get(t);
        return i.intValue();
    }

    /*
     * PairOrderedSet moveList (Temp t) obtém o conjunto de MOVES associados ao
     * temporário t.
     */
    private PairOrderedSet moveList(Temp t) {
        PairOrderedSet set = (PairOrderedSet) moveList.get(t);
        if (set == null) {
            set = new PairOrderedSet("moveList(" + t + ")");
            set.showErro = false;
            moveList.put(t, set);
        }
        return set;
    }

    /*
     * TempSet moveList (Temp t) obtém o conjunto de temporários que interferem
     * com o temporário t.
     */
    private TempSet adjList(Temp t) {
        TempSet set = (TempSet) adjList.get(t);
        if (set == null) {
            set = new TempSet("moveList(" + t + ")");
            adjList.put(t, set);
        }
        return set;
    }

    /*
     * InstrList getInstructions() retorna o conjunto de instruções associado à
     * funcão analisada.
     */
    public InstrList getInstructions() {
        return il;
    }

    /*
     * construtor RegAlloc (Frame f, InstrList il) realiza a alocação de
     * registradores.
     */
    public RegAlloc(Frame f, InstrList il) {
        frame = f;
        this.il = il;

        Initialize();
        Main();
        boolean removed1 = false, removed2 = false;
        do {
            removed1 = RemoveUselessInstructions();
            removed2 = RemoveUnusedLabels();
        } while (removed1 || removed2);
    }

    /*
     * Initialize() inicializa os conjuntos a serem usados.
     */
    private void Initialize() {
        initial = new TempSet("initial");
        precolored = new TempSet("precolored");
        simplifyWorklist = new TempSet("simplifyWorklist");
        freezeWorklist = new TempSet("freezeWorklist");
        spillWorklist = new TempSet("spillWorklist");
        spilledNodes = new TempSet("spilledNodes");
        coalescedNodes = new TempSet("coalescedNodes");
        coloredNodes = new TempSet("coloredNodes");
        selectStackCopy = new TempSet("selectStack");
        selectStack = new OrderedSet("selectStack");
        newTemps = new TempSet("newTemps");
        fetchesTemp = new TempSet("fetchesTemp");
        calleeSave = new TempSet(frame.calleeSaves());
        callerSave = new TempSet(frame.callerSaves());
        color = new Hashtable();

        // coloca os registradores no conjunto dos pré-coloridos
        K = 0;
        for (TempList l = frame.registers(); l != null; l = l.tail) {
            precolored.add(l.head);
            color.put(l.head, l.head);
            K++;
        }
    }

    /*
     * Main() função principal (recursiva até que não sejam criados mais
     * temporário "spilled").
     */
    private void Main() {
        Liveness();
        Build();
        MakeWorklist();
        do {
            if (!simplifyWorklist.isEmpty())
                Simplify();
            else if (!worklistMoves.isEmpty())
                Coalesce();
            else if (!freezeWorklist.isEmpty())
                Freeze();
            else if (!spillWorklist.isEmpty())
                SelectSpill();
        } while (!simplifyWorklist.isEmpty() || !worklistMoves.isEmpty()
                || !freezeWorklist.isEmpty() || !spillWorklist.isEmpty());
        AssignColors();
        if (!spilledNodes.isEmpty()) {
            RewriteProgram();
            Main();
        }
    }

    /*
     * Liveness() faz o "liveness" estático, criando o grafo de interferências.
     */
    private void Liveness() {
        ig = new Liveness(new AssemFlowGraph(frame.procEntryExit2(il)));
    }

    /*
     * Build() A partir do grafo de interferências, cria os conjuntos que serão
     * uma nova representarão deste grafo, com os quais o algoritmo de alocação
     * de registradores trabalhará.
     */
    private void Build() {
        // cria os conjuntos que guardarão os MOVES
        coalescedMoves = new PairOrderedSet("coalescedMoves");
        constrainedMoves = new PairOrderedSet("constrainedMoves");
        frozenMoves = new PairOrderedSet("frozenMoves");
        worklistMoves = new PairOrderedSet("worklistMoves");
        worklistMoves.showErro = false;
        activeMoves = new PairOrderedSet("activesMoves");
        // cria os conjuntos que representam o grafo de interferências
        adjSet = new PairOrderedSet("adjSet");
        adjList = new Hashtable();
        moveList = new Hashtable();
        degree = new Hashtable();
        alias = new Hashtable();
        // determina todos os MOVES.
        for (MoveList ml = ig.moves(); ml != null; ml = ml.tail) {
            Temp src = ig.gtemp(ml.src);
            Temp dst = ig.gtemp(ml.dst);
            moveList(src).add(dst, src);
            moveList(dst).add(dst, src);
            worklistMoves.add(dst, src);
        }
        // inicializa o grau dos temporários
        for (NodeList nl = ig.nodes(); nl != null; nl = nl.tail) {
            Temp u = ig.gtemp(nl.head);
            degree.put(u, new Integer(0));
        }
        // coloca cada temporário no devido conjunto e cria os conjuntos
        // representando as interferências.
        for (NodeList nl = ig.nodes(); nl != null; nl = nl.tail) {
            Temp u = ig.gtemp(nl.head);
            if (!precolored.has(u))
                initial.add(u);
            for (NodeList s = nl.head.adj(); s != null; s = s.tail) {
                Temp v = ig.gtemp(s.head);
                AddEdge(u, v);
            }
        }
        // TestInvariants();
    }

    /*
     * AddEdge (Temp u, Temp v) cria uma aresta de interferência entre os
     * temporários u e v.
     */
    private void AddEdge(Temp u, Temp v) {
        if ((!adjSet.has(u, v)) && (u != v)) {
            adjSet.add(u, v);
            adjSet.add(v, u);
            if (!precolored.has(u)) {
                adjList(u).add(v);
                degree.put(u, new Integer(Degree(u) + 1));
            }
            if (!precolored.has(v)) {
                adjList(v).add(u);
                degree.put(v, new Integer(Degree(v) + 1));
            }
        }
    }

    /*
     * MakeWorklist() transfere cada temporário do conjunto initial para o
     * conjunto: - spillWorklist (se for de grau significante) - freezeWorklist
     * (se for de grau não-significante, associado a MOVE) - simplifyWorklist
     * (se for de grau não-significante, não associado a MOVE)
     */
    private void MakeWorklist() {
        for (Temp n = (Temp) initial.getFirst(); n != null; n = (Temp) initial
                .getNext(n)) {
            initial.remove(n);
            if (Degree(n) >= K)
                spillWorklist.add(n);
            else if (MoveRelated(n))
                freezeWorklist.add(n);
            else
                simplifyWorklist.add(n);
        }
    }

    /*
     * TempSet Adjacent (Temp n) retorna o conjunto dos temporários que ainda
     * interferem com o temporário n.
     */
    private TempSet Adjacent(Temp n) {
        return adjList(n).difference(selectStackCopy)
                .difference(coalescedNodes);
    }

    /*
     * PairOrderedSet NodeMoves (Temp n) retorna o conjunto dos MOVES associados
     * ao temporário n.
     */
    private PairOrderedSet NodeMoves(Temp n) {
        PairOrderedSet set = moveList(n).makeCopy();
        PairOrderedSet set2 = activeMoves.makeCopy().union(worklistMoves);
        return set.intersection(set2);
    }

    /*
     * boolean MoveRelated (Temp n) verifica se o temporário n está associado a
     * algum MOVE.
     */
    private boolean MoveRelated(Temp n) {
        return !(NodeMoves(n).isEmpty());
    }

    /*
     * Simplify() escolhe um dos temporários de simplifyWorklist, remove-o do
     * grafo e o coloca na pilha para futura coloração.
     */
    private void Simplify() {
        Temp n = (Temp) simplifyWorklist.getFirst();
        simplifyWorklist.remove(n);
        selectStack.addFirst(n);
        selectStackCopy.add(n);
        TempSet adj = Adjacent(n);
        for (Temp m = (Temp) adj.getFirst(); m != null; m = (Temp) adj
                .getNext(m))
            DecrementDegree(m);
    }

    /*
     * DecrementDegree() após remover um temporário do grafo, atualiza seus
     * vizinhos.
     */
    private void DecrementDegree(Temp m) {
        int d = Degree(m);
        degree.put(m, new Integer(d - 1));
        if (d == K) {
            TempSet adj = Adjacent(m);
            adj.add(m);
            EnableMoves(adj);
            spillWorklist.remove(m);
            if (MoveRelated(m))
                freezeWorklist.add(m);
            else
                simplifyWorklist.add(m);
        }
    }

    /*
     * EnableMoves (TempSet nodes) habilita os MOVES associados aos nós
     * presentes no conjunto nodes.
     */
    private void EnableMoves(TempSet nodes) {
        for (Temp n = (Temp) nodes.getFirst(); n != null; n = (Temp) nodes
                .getNext(n)) {
            PairOrderedSet nm = NodeMoves(n);
            for (Pair m = (Pair) nm.getFirst(); m != null; m = (Pair) nm
                    .getNext(m.x, m.y)) {
                if (activeMoves.has(m.x, m.y)) {
                    activeMoves.remove(m.x, m.y);
                    worklistMoves.add(m.x, m.y);
                }
            }
        }
    }

    /*
     * Coalesce() já que há MOVES em worklistMoves, escolhe um deles e tenta
     * eliminar o MOVE fazendo com que o temporário destino fique no mesmo
     * registrador que o temporário origem.
     */
    private void Coalesce() {
        Pair m = worklistMoves.getFirst();
        Temp x = GetAlias((Temp) m.x);
        Temp y = GetAlias((Temp) m.y);
        Temp u, v;
        if (precolored.has(y)) {
            u = y;
            v = x;
        } else {
            u = x;
            v = y;
        }
        worklistMoves.remove(m.x, m.y);
        if (u == v) {
            // temporário origem e destino são o mesmo
            coalescedMoves.add(m.x, m.y);
            AddWorkList(u);
        } else if (precolored.has(v) || adjSet.has(u, v)) {
            // este MOVE não pode ser eliminado: os temporários do MOVE
            // interferem
            constrainedMoves.add(m.x, m.y);
            AddWorkList(u);
            AddWorkList(v);
        } else if ((precolored.has(u) && AdjacentOK(v, u))
                || (!precolored.has(u) && Conservative(Adjacent(u).union(
                        Adjacent(v))))) {
            // critério de George ou Briggs verificado e satisfeito
            coalescedMoves.add(m.x, m.y);
            Combine(u, v);
            AddWorkList(u);
        } else
            // este MOVE será deixado para depois
            activeMoves.add(m.x, m.y);
    }

    /*
     * AddWorklist (Temp u) o temporário u será marcado para simplificar (não
     * será mais considerado para "coalesce")
     */
    private void AddWorkList(Temp u) {
        if (!precolored.has(u) && !MoveRelated(u) && (Degree(u) < K)) {
            freezeWorklist.remove(u);
            simplifyWorklist.add(u);
        }
    }

    /*
     * AdjacentOK (Temp v, Temp r) verifica se o critério de George é
     * satisfeito, ié, se para cada vizinho z de v, z já interefere com r ou z é
     * de grau não-significante.
     */
    private boolean AdjacentOK(Temp v, Temp r) {
        boolean ok = true;
        TempSet adj = Adjacent(v);
        for (Temp t = (Temp) adj.getFirst(); t != null && ok; t = (Temp) adj
                .getNext(t))
            ok = ((Degree(t) < K) || precolored.has(t) || adjSet.has(t, r));
        return ok;
    }

    /*
     * Conservative (TempSet set) verifica se o critério de Briggs é satisfeito,
     * ié, a partir de um conjunto com os vizinhos de dois temporários de um
     * MOVE que será eliminado, o conjunto possui menos do que K temporários de
     * grau significante.
     */
    private boolean Conservative(TempSet set) {
        int k = 0;
        for (Temp n = (Temp) set.getFirst(); n != null; n = (Temp) set
                .getNext(n)) {
            if (Degree(n) >= K)
                k++;
        }
        return (k < K);
    }

    /*
     * Temp GetAlias (Temp n) retorna o alias de um temporário, ié, se o
     * temporário pertencia a um MOVE que foi eliminado, retorna o outro
     * temporário do MOVE.
     */
    private Temp GetAlias(Temp n) {
        if (coalescedNodes.has(n))
            return GetAlias((Temp) alias.get(n));
        return n;
    }

    /*
     * Combine (Temp u, Temp v) realiza os preparativos para eliminar o MOVE ao
     * qual dois temporários u e v estão associados.
     */
    private void Combine(Temp u, Temp v) {
        if (freezeWorklist.has(v))
            freezeWorklist.remove(v);
        else
            spillWorklist.remove(v);
        coalescedNodes.add(v);
        alias.put(v, u);
        moveList(u).union(moveList(v));
        TempSet adj = Adjacent(v);
        for (Temp t = (Temp) adj.getFirst(); t != null; t = (Temp) adj
                .getNext(t)) {
            int d = Degree(t);
            AddEdge(t, u);
            if ((d == K - 1) && (Degree(t) == K))
                degree.put(t, new Integer(d));
            else
                DecrementDegree(t);
        }
        if ((Degree(u) >= K) && freezeWorklist.has(u)) {
            freezeWorklist.remove(u);
            spillWorklist.add(u);
        }
    }

    /*
     * Freeze() dado que não há mais temporários para simplificar nem MOVES que
     * podem ser eliminados, desiste de eliminar MOVES associado a algum
     * temporário para tentar fazer futuras simplificações.
     */
    private void Freeze() {
        Temp u = (Temp) freezeWorklist.getFirst();
        freezeWorklist.remove(u);
        simplifyWorklist.add(u);
        FreezeMoves(u);
    }

    /*
     * FreezeMoves (Temp u) desiste de eliminar MOVES associado ao temporário u.
     */
    private void FreezeMoves(Temp u) {
        PairOrderedSet nm = (PairOrderedSet) NodeMoves(u);
        for (Pair m = (Pair) nm.getFirst(); m != null; m = (Pair) nm.getNext(
                m.x, m.y)) {
            Temp v;
            Temp aliasY = GetAlias((Temp) m.y);
            if (aliasY == GetAlias(u))
                v = GetAlias((Temp) m.x);
            else
                v = aliasY;
            activeMoves.remove(m.x, m.y);
            frozenMoves.add(m.x, m.y);
            if (NodeMoves(v).isEmpty() && (Degree(v) < K)) {
                freezeWorklist.remove(v);
                simplifyWorklist.add(v);
            }
        }
    }

    /*
     * SelectSpill() Como não puderam ser feitas simplificações, nem eliminações
     * de MOVES, nem congelamento de MOVES, escolhe um temporário para um
     * possível "spill" (lançamento no frame) para poder continuar fazendo
     * simplificações.
     */
    private void SelectSpill() {
        // evita escolher os temporários que foram criados para fazer spill de
        // outros temporários
        TempSet select = spillWorklist.difference(fetchesTemp);
        if (select.isEmpty())
            select = spillWorklist;
        // escolhe o temporário cujo custo de spill seja mínimo.
        Temp m = null;
        int cost = 1 << 30;
        for (Temp t = (Temp) select.getFirst(); t != null; t = (Temp) select
                .getNext(t)) {
            int spillCost = ig.spillCost(ig.tnode(t));
            if (spillCost < cost) {
                cost = spillCost;
                m = t;
            }
        }
        spillWorklist.remove(m);
        simplifyWorklist.add(m);
        FreezeMoves(m);
    }

    /*
     * AssignColors() uma vez removidos todos os nós e colocados na pilha,
     * remove cada temporário da pilha a medida que lhe atribui uma cor; se não
     * houver cor, então joga-o para o frame ("actual spill").
     */
    private void AssignColors() {
        while (!selectStack.isEmpty()) {
            // remove um temporário da pilha
            Temp n = (Temp) selectStack.getFirst();
            selectStack.remove(n);
            selectStackCopy.remove(n);
            // verifica quais as cores que não poderão ser usadas para o
            // temporário
            TempSet okColors = precolored.makeCopy();
            TempSet adj = adjList(n);
            for (Temp w = (Temp) adj.getFirst(); w != null; w = (Temp) adj
                    .getNext(w)) {
                Temp aliasW = GetAlias(w);
                if (coloredNodes.has(aliasW) || precolored.has(aliasW))
                    okColors.remove((Temp) color.get(aliasW));
            }
            // verifica se sobrou alguma cor para o temporário
            if (okColors.isEmpty())
                spilledNodes.add(n); // o temporário vai para o frame
            else {
                // escolhe uma das cores para o temporário
                Temp c;
                coloredNodes.add(n);
                // tenta escolher um registrador (cor) que não seja callee-save
                TempSet noCalleeSave = okColors.difference(calleeSave);
                if (!noCalleeSave.isEmpty())
                    c = (Temp) noCalleeSave.getFirst();
                else
                    c = (Temp) okColors.getFirst();
                color.put(n, c);
            }
        }
        for (Temp n = (Temp) coalescedNodes.getFirst(); n != null; n = (Temp) coalescedNodes
                .getNext(n))
            color.put(n, (Temp) color.get(GetAlias(n)));
    }

    /*
     * RewriteProgram() reescreve o programa, gerando as instruções para spill
     * se houver temporário marcados para spill.
     */
    private void RewriteProgram() {
        // o hashtable a seguir será usado para associar temporários que não
        // poderão ficar em registrador
        // ("spilled") aos lugares no frame reservados para eles
        Dictionary spilledLocations = new Hashtable();
        for (Temp s = (Temp) spilledNodes.getFirst(); s != null; s = (Temp) spilledNodes
                .getNext(s))
            spilledLocations.put(s, frame.allocLocal(true));
        // começa o processo de reescritura do programa
        Assem.InstrList rewrittenProgram = null;
        Assem.InstrList last = null;
        rewrittenProgram = null;
        last = null;
        for (InstrList p = il; p != null; p = p.tail) {
            InstrList added = replaceSpilledTemps(p.head, spilledLocations);
            if (last == null)
                last = rewrittenProgram = added;
            else
                last = last.tail = added;
            while (last.tail != null)
                last = last.tail;
        }
        il = rewrittenProgram;
        // atualiza os conjuntos
        initial = coloredNodes.union(coalescedNodes).union(newTemps);
        newTemps = new TempSet("newTemps");
        spilledNodes = new TempSet("spilledNodes");
        coloredNodes = new TempSet("coloredNodes");
        coalescedNodes = new TempSet("coalescedNodes");
    }

    /*
     * InstrList replaceSpilledTemps (Instr i, Dictionary spilledLocations) gera
     * as instruções necessário para realizar spill se houver temporários nesta
     * instrução marcados para spill.
     */
    private InstrList replaceSpilledTemps(Instr i, Dictionary spilledLocations) {

        Assem.InstrList first = null;
        Assem.InstrList last = null;

        // substitui cada temporário t usado na instrução i marcado para "spill"
        // por um novo temporário c,
        // gerando uma instrução c <= M[loc] antes da instrução i
        PairOrderedSet frameToTemp = new PairOrderedSet();
        for (TempList use = i.use(); use != null; use = use.tail) {
            if (spilledNodes.has(use.head)) {
                Access frameAddress = (Access) spilledLocations.get(use.head);
                OrderedSet auxSet = frameToTemp.get(frameAddress);
                Temp newTemp;
                boolean createFetchInstr = false;
                // verifica se a instrução c <= M[loc] já foi gerada antes
                if (auxSet.isEmpty()) {
                    // c <= M[loc] não foi gerado antes, é necessário gerá-la
                    newTemp = new Temp();
                    fetchesTemp.add(newTemp);
                    newTemps.add(newTemp);
                    frameToTemp.add(frameAddress, newTemp);
                    createFetchInstr = true;
                } else
                    // aproveita o temporário c da instrução c <= M[loc] gerada
                    // anteriormente
                    newTemp = (Temp) auxSet.getFirst();
                // substitui o temporário "spilled" pelo novo que receberá o
                // valor do frame
                if (i instanceof Assem.MOVE)
                    ((Assem.MOVE) i).src = newTemp;
                else
                    use.head = newTemp;
                // verifica se a instrução c <= M[loc] foi marcada para ser
                // gerada
                if (!createFetchInstr)
                    continue;
                // gera as instruções para ler o valor armazenado no frame e
                // colocá-lo no novo temporário
                Tree.Stm fetchStm = new Tree.MOVE(new Tree.TEMP(newTemp),
                        frameAddress.exp(new Tree.TEMP(frame.FP())));
                Assem.InstrList fetchInstr = frame.codegen(fetchStm);
                if (last == null)
                    last = first = fetchInstr;
                else
                    last = last.tail = fetchInstr;
                while (last.tail != null)
                    last = last.tail;
            }
        }
        // acrescenta a instrução assembly i, após todas as geradas para ler
        // valores do frame de temporários
        // "spilled"
        if (last == null)
            last = first = new Assem.InstrList(i, null);
        else
            last = last.tail = new Assem.InstrList(i, null);
        // substitui cada temporário t definido na instrução i e marcado para
        // "spill" por um novo temporário c,
        // gerando uma instrução M[loc] <= c após a instrução i
        for (TempList def = i.def(); def != null; def = def.tail) {
            if (spilledNodes.has(def.head)) {
                Access frameAddress = (Access) spilledLocations.get(def.head);
                Temp newTemp = new Temp();
                newTemps.add(newTemp);
                if (i instanceof Assem.MOVE)
                    ((Assem.MOVE) i).dst = newTemp;
                else
                    def.head = newTemp;
                Tree.Stm storeStm = new Tree.MOVE(
                        frameAddress.exp(new Tree.TEMP(frame.FP())),
                        new Tree.TEMP(newTemp));
                Assem.InstrList storeInstr = frame.codegen(storeStm);
                if (last == null)
                    last = first = storeInstr;
                else
                    last = last.tail = storeInstr;
                while (last.tail != null)
                    last = last.tail;
            }
        }
        return first;
    }

    /*
     * boolean RemoveUselessInstructions() tenta eliminar instruções inúteis,
     * como MOVE a,a ou JUMP para label que vem logo a seguir; retorna true se
     * conseguiu eliminar alguma instrução, false caso contrário.
     */
    private boolean RemoveUselessInstructions() {
        boolean removedInstruction = false;
        boolean changed;
        do { // enquanto houver a possibilidade de eliminar alguma instrução
            changed = false;
            InstrList rewrittenProgram = null;
            InstrList last = null;
            for (InstrList p = il; p != null; p = p.tail) {
                if (p.head instanceof MOVE) {
                    // verifica se este MOVE pode ser eliminado
                    MOVE move = (MOVE) p.head;
                    if (color.get(move.src) == color.get(move.dst)) {
                        changed = true;
                        removedInstruction = true;
                        continue; // ignora este MOVE
                    }
                } else if (p.head instanceof OPER
                        && ((OPER) p.head).def() == null
                        && ((OPER) p.head).jumps() != null && p.tail != null
                        && p.tail.head instanceof LABEL) {
                    // desvio seguido de um rótulo; verifica se o desvio é feito
                    // para o rótulo a seguir; caso
                    // afirmativo, apaga este desvio
                    LabelList ll = ((OPER) p.head).jumps().labels;
                    Label next = ((LABEL) p.tail.head).label;
                    for (; ll != null; ll = ll.tail) {
                        if (ll.head != next)
                            break;
                    }
                    if (ll == null) {
                        // ignora este desvio
                        changed = true;
                        removedInstruction = true;
                        continue;
                    }
                }
                InstrList added = new InstrList(p.head, null);
                if (last == null)
                    last = rewrittenProgram = added;
                else
                    last = last.tail = added;
            }
            il = rewrittenProgram;
        } while (changed);
        return removedInstruction;
    }

    /*
     * boolean RemoveUnusedLabels() tenta remover labels que não são usados e
     * substitui várias declarações de labels contíguas por uma só.
     */
    private boolean RemoveUnusedLabels() {
        boolean removedLabel = false;
        Dictionary labelUse = new Hashtable();
        Dictionary declaredLabel = new Hashtable();
        OrderedSet declaredLabels = new OrderedSet();
        OrderedSet lastLabelSet = null;
        // passo 1: coleta informações sobre todos os rótulos no programa
        for (InstrList p = il; p != null; p = p.tail) {
            if (p.head instanceof Assem.LABEL) {
                Label label = ((Assem.LABEL) p.head).label;
                if (lastLabelSet == null)
                    lastLabelSet = new OrderedSet();
                lastLabelSet.addFirst(label);
                declaredLabel.put(label, lastLabelSet);
                declaredLabels.add(label);
            } else {
                lastLabelSet = null;
                if (p.head.jumps() != null) {
                    for (LabelList l = p.head.jumps().labels; l != null; l = l.tail) {
                        Integer numberOfUses = (Integer) labelUse.get(l.head);
                        if (numberOfUses == null)
                            numberOfUses = new Integer(1);
                        else
                            numberOfUses = new Integer(
                                    numberOfUses.intValue() + 1);
                        labelUse.put(l.head, numberOfUses);
                    }
                }
            }
        }
        // passo 2: caso haja várias declarações de labels contíguas, escolhe um
        // dos rótulos como representante
        Label nextLabel;
        for (Label x = (Label) declaredLabels.getFirst(); x != null; x = nextLabel) {
            nextLabel = (Label) declaredLabels.getNext(x);
            Integer numberOfUse = (Integer) labelUse.get(x);
            if (numberOfUse != null && numberOfUse.intValue() > 0) {
                OrderedSet labelSet = (OrderedSet) declaredLabel.get(x);
                if (labelSet.size() > 1) {
                    labelUse.remove(x);
                    Label label = (Label) labelSet.getFirst();
                    Integer n = (Integer) labelUse.get(label);
                    if (n == null)
                        n = new Integer(numberOfUse.intValue());
                    else
                        n = new Integer(n.intValue() + numberOfUse.intValue());
                    labelUse.put(label, n);
                }
            }
        }
        // passo 3: reescreve o programa
        InstrList rewrittenProgram = null;
        InstrList last = null;
        Integer aux = new Integer(1);
        for (InstrList p = il; p != null; p = p.tail) {
            Integer numberOfUse = aux;
            if (p.head instanceof Assem.LABEL)
                numberOfUse = (Integer) labelUse
                        .get(((Assem.LABEL) p.head).label);
            else if (p.head.jumps() != null) {
                for (LabelList targets = p.head.jumps().labels; targets != null; targets = targets.tail) {
                    OrderedSet labelSet = (OrderedSet) declaredLabel
                            .get(targets.head);
                    if (labelSet != null)
                        targets.head = (Label) labelSet.getFirst();
                }
            }
            if (numberOfUse != null && numberOfUse.intValue() > 0) {
                if (last == null)
                    rewrittenProgram = last = p;
                else
                    last = last.tail = p;
            } else
                removedLabel = true;
        }
        if (last != null)
            last.tail = null;
        il = rewrittenProgram;
        return removedLabel;
    }

    /*
     * ======= FUNÇÕES DE DEBUG (PARA VERIFICAR ERROS NA IMPLEMENTAÇÃO DA
     * ALOCAÇÃO DE REGISTRADORES) =======
     */

    /*
     * TestInvariants() verifica se as 4 afirmativas (pág.254 do livro) são
     * satisfeitas.
     */
    private void TestInvariants() {
        // System.out.println("Testing invariants:");
        boolean tudoOk = true;
        {
            // System.out.print("\tDegree: ");
            boolean test = true;
            TempSet firstSet = simplifyWorklist.union(freezeWorklist).union(
                    spillWorklist);
            TempSet secondSet = firstSet.union(precolored);
            for (Temp u = (Temp) firstSet.getFirst(); u != null; u = (Temp) firstSet
                    .getNext(u)) {
                TempSet aux = adjList(u).intersection(secondSet);
                test = (test && (Degree(u) == aux.size()));
            }
            // System.out.println(test);
            tudoOk = tudoOk & test;
        }
        PairOrderedSet moves = activeMoves.makeCopy().union(worklistMoves);
        {
            // System.out.print("\tSimplify worklist: ");
            boolean test = true;
            for (Temp u = (Temp) simplifyWorklist.getFirst(); u != null; u = (Temp) simplifyWorklist
                    .getNext(u)) {
                PairOrderedSet aux = moveList(u).makeCopy().intersection(moves);
                test = (test && ((Degree(u) < K) && (aux.isEmpty())));
            }
            // System.out.println(test);
            tudoOk = tudoOk & test;
        }
        {
            // System.out.print("\tFreeze worklist: ");
            boolean test = true;
            for (Temp u = (Temp) freezeWorklist.getFirst(); u != null; u = (Temp) freezeWorklist
                    .getNext(u)) {
                PairOrderedSet aux = moveList(u).makeCopy().intersection(moves);
                test = (test && ((Degree(u) < K) && (!aux.isEmpty())));
            }
            // System.out.println(test);
            tudoOk = tudoOk & test;
        }
        {
            // System.out.print("\tSpill worklist: ");
            boolean test = true;
            for (Temp u = (Temp) spillWorklist.getFirst(); u != null; u = (Temp) spillWorklist
                    .getNext(u))
                test = (test && (Degree(u) >= K));
            // System.out.println(test);
            tudoOk = tudoOk & test;
        }
        if (!tudoOk)
            System.out.println("ERRO: INVARIANTES NÃO PRESERVADAS");

    }

    /*
     * showProgram() mostra as instruções assembly.
     */
    private void showProgram() {
        TempMap tempMap = new CombineMap(this, new DefaultMap());
        for (Assem.InstrList p = il; p != null; p = p.tail)
            System.out.print(p.head.format(tempMap));
    }

    /*
     * showSets() mostra os elementos presentes em cada conjunto de sets.
     */
    private void showSets() {
        TempSet[] sets = { precolored, initial, simplifyWorklist,
                freezeWorklist, spillWorklist, spilledNodes, coalescedNodes,
                coloredNodes };
        String[] setsName = { "precolored", "initial", "simplifyWorklist",
                "freezeWorklist", "spillWorklist", "spilledNodes",
                "coalescedNodes", "coloredNodes" };
        for (int i = 0; i < sets.length; i++)
            System.out.println(sets[i]);
        System.out.println();
    }

    /*
     * LookFor (Temp n) procura o temporário n entre os conjuntos presentes em
     * sets.
     */
    private void LookFor(Temp n) {
        TempSet[] sets = { precolored, initial, simplifyWorklist,
                freezeWorklist, spillWorklist, spilledNodes, coalescedNodes,
                coloredNodes };
        String[] setsName = { "precolored", "initial", "simplifyWorklist",
                "freezeWorklist", "spillWorklist", "spilledNodes",
                "coalescedNodes", "coloredNodes" };
        System.out.print("Looking for " + n + ": ");
        for (int i = 0; i < sets.length; i++)
            if (sets[i].has(n))
                System.out.print(setsName[i] + " ");
        System.out.println();
    }
}
