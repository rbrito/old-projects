package RegAlloc;

import Graph.*;
import FlowGraph.*;
import Temp.*;

/***********************************************************************************************************
 * CLASSE Liveness *
 * ------------------------------------------------------------
 * ------------------------------------------- * Faz análise do liveness de cada
 * temporário do programa e constrói um grafo de interferências (este * grafo
 * possui uma aresta entre cada par de temporários que estão vivos no mesmo
 * momento, indicando * que não podem estar no mesmo registrador. *
 ***********************************************************************************************************/

public class Liveness extends InterferenceGraph {
    // armazena todos os temporários que estão vivos após o nó (instrução
    // assembly) associado (live-out)
    private java.util.Dictionary liveOutMap = new java.util.Hashtable();
    // mapeia temporários para nós no grafo de interferência e vice-versa
    private java.util.Dictionary tempToNodeMap = new java.util.Hashtable();
    private java.util.Dictionary nodeToTempMap = new java.util.Hashtable();
    // associa a cada temporário o número de vezes em que é usado em todo o
    // programa
    private java.util.Dictionary useOfTemps = new java.util.Hashtable();
    // lista com todos os moves presentes
    private MoveList moveList = null;

    /*
     * construtor Liveness (AssemFlowGraph flow) realiza a análise do liveness e
     * a contrução do grafo de interferências a partir do grafo de fluxo do
     * programa.
     */
    public Liveness(AssemFlowGraph flow) {
        makeLiveMap(flow);
        buildInterferenceGraph(flow);
        countNumberOfUses(flow);
    }

    /*
     * makeLiveMap (AssemFlowGraph flow) faz a análise do liveness de cada
     * temporário a partir do grafo de fluxo do programa; como resultado,
     * indica, após cada instrução assembly, quais os temporários que estão
     * vivos.
     */
    private void makeLiveMap(AssemFlowGraph flow) {
        // armazena todos os temporários que estão vivos ao chegar a um nó
        // (live-in)
        java.util.Dictionary liveInMap = new java.util.Hashtable();
        // associa a cada instrução assembly o conjunto dos temporários que são
        // usados e os que são definidos
        java.util.Dictionary uses = new java.util.Hashtable();
        java.util.Dictionary defs = new java.util.Hashtable();

        // para cada instrução assembly, cria um TempSet com os temporários que
        // são usados e outro com os que
        // são definidos
        for (NodeList nl = flow.nodes(); nl != null; nl = nl.tail) {
            liveInMap.put(nl.head, new TempSet());
            liveOutMap.put(nl.head, new TempSet());
            defs.put(nl.head, new TempSet(flow.def(nl.head)));
            uses.put(nl.head, new TempSet(flow.use(nl.head)));
        }

        boolean changed; // indica se houve mudança nos conjuntos live-in e/ou
                         // live-out do algum nó
        NodeList flowNodes = revert(flow.nodes()); // "inverte" os nós do grafo
                                                   // de fluxo para acelerar o
                                                   // processo de liveness
        do {
            changed = false;
            // percorre todos os nós (instruções assembly) do grafo
            for (NodeList nodes = flowNodes; nodes != null; nodes = nodes.tail) {
                Node n = nodes.head;
                // obtém os conjuntos com os temporários que estão vivos ao
                // entrar e ao sair da instrução assembly
                TempSet oldIn = (TempSet) liveInMap.get(n);
                TempSet oldOut = (TempSet) liveOutMap.get(n);
                TempSet use = (TempSet) uses.get(n);
                TempSet def = (TempSet) defs.get(n);
                // atualiza o conjunto dos temporários que estão vivos ao sair
                // deste nó
                TempSet out = new TempSet();
                for (NodeList s = n.succ(); s != null; s = s.tail) {
                    TempSet in_s = (TempSet) liveInMap.get(s.head);
                    out = out.union(in_s);
                }
                liveOutMap.put(n, out);
                // atualiza o conjunto dos temporários que estão vivos ao chegar
                // a este nó
                TempSet in = use.union(out.difference(def));
                liveInMap.put(n, in);
                // verifica se houve mudanças
                changed = changed
                        || (!in.isEqualTo(oldIn) || !out.isEqualTo(oldOut));
            }
        } while (changed);
    }

    /*
     * buildInterferenceGraph (AssemFlowGraph flow) com as informações sobre o
     * liveness de cada temporário e a partir do grafo de fluxo do programa,
     * constrói o grafop de interferências.
     */
    private void buildInterferenceGraph(AssemFlowGraph flow) {
        for (NodeList nodes = flow.nodes(); nodes != null; nodes = nodes.tail) {
            Assem.Instr instruct = flow.instr(nodes.head);

            if (instruct instanceof Assem.MOVE) {
                // Para cada MOVE a <- c, onde os temporários b1 ... bj estão
                // live-out, será gerada uma aresta
                // de interferência (a,b1) .. (a,bj) para todo bk que não seja o
                // mesmo que a.
                Assem.MOVE move = (Assem.MOVE) instruct;
                Node node1 = tnode(move.dst);
                moveList = new MoveList(tnode(move.src), node1, moveList);
                TempSet out = (TempSet) liveOutMap.get(nodes.head);
                for (Temp t = out.getFirst(); t != null; t = out.getNext(t))
                    if (t != move.src && t != move.dst) {
                        Node node2 = tnode(t);
                        addEdge(node1, node2);
                    }
            } else {
                // Para cada operação que não for MOVE que defina um temporário
                // a, cujos temporários live-out sejam
                // b1 ... bj, será gerada uma aresta de interferência (a,b1) ...
                // (a,bj)
                for (TempList t = flow.def(nodes.head); t != null; t = t.tail) {
                    Node node1 = tnode(t.head);
                    TempSet out = (TempSet) liveOutMap.get(nodes.head);
                    for (Temp u = out.getFirst(); u != null; u = out.getNext(u))
                        if (t.head != u) {
                            Node node2 = tnode(u);
                            addEdge(node1, node2);
                        }
                }
            }
        }
    }

    /*
     * countNumberOfUses (AssemFlowGraph flow) conta o número de vezez em que
     * cada temporário é utilizado em todo o programa.
     */
    private void countNumberOfUses(AssemFlowGraph flow) {
        for (NodeList nl = nodes(); nl != null; nl = nl.tail) {
            Temp t = gtemp(nl.head);
            useOfTemps.put(t, new Integer(0));
        }
        for (NodeList il = flow.nodes(); il != null; il = il.tail) {
            Assem.Instr instruct = flow.instr(il.head);
            for (TempList ul = instruct.use(); ul != null; ul = ul.tail) {
                // FIXME: n pode ser null
                Integer n = (Integer) useOfTemps.get(ul.head);
                // if (n == null)
                useOfTemps.put(ul.head, new Integer(n.intValue() + 1));
            }
        }
    }

    /*
     * Node tnode (Temp temp) devolve o nó associado a um temporário no grafo de
     * interferências.
     */
    public Node tnode(Temp temp) {
        Node n = (Node) tempToNodeMap.get(temp);
        if (n == null) {
            n = newNode();
            tempToNodeMap.put(temp, n);
            nodeToTempMap.put(n, temp);
        }
        return n;
    }

    /*
     * Temp gtemp (Node node) devolve o temporário associado a um nó do grafo de
     * interferências.
     */
    public Temp gtemp(Node node) {
        return (Temp) nodeToTempMap.get(node);
    }

    /*
     * MoveList moves() retorna uma lista com todos os MOVEs do programa.
     */
    public MoveList moves() {
        return moveList;
    }

    /*
     * show (java.io.PrintStream out) grava em out uma representação deste grafo
     * de interferências.
     */
    public void show(java.io.PrintStream out) {
        for (NodeList p = nodes(); p != null; p = p.tail) {
            Node n = p.head;
            out.print(gtemp(n));
            out.print(": ");
            for (NodeList q = n.adj(); q != null; q = q.tail) {
                out.print(gtemp(q.head));
                out.print(" ");
            }
            out.println();
        }
    }

    /*
     * int spillCost (Node n) retorna o custo associado a fazer "spill" do nó
     * (temporário) fornecido.
     */
    public int spillCost(Node n) {
        // leva em conta o número de vezes que o temporário é usado e o grau
        // deste temporário no grafo de
        // interferências
        int uses = ((Integer) useOfTemps.get(gtemp(n))).intValue();
        return 10000 * uses / n.degree();
    }

    /*
     * int numberOfUses (Temp t) retorna o número de vezes que um temporário é
     * usado.
     */
    public int numberOfUses(Temp t) {
        return ((Integer) useOfTemps.get(t)).intValue();
    }

    /*
     * NodeList revert (NodeList nl) cria uma lista invertida a partir de uma
     * outra lista.
     */
    private NodeList revert(NodeList nl) {
        NodeList x = null;
        for (; nl != null; nl = nl.tail)
            x = new NodeList(nl.head, x);
        return x;
    }
}
