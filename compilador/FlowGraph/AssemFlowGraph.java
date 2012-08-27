package FlowGraph;

import Graph.*;
import Temp.*;
import Assem.*;


/***********************************************************************************************************
 *   CLASSE AssemFlowGraph                                                                                 *
 * ------------------------------------------------------------------------------------------------------- *
 *   Cria um grafo representando o fluxo de um conjunto de instruções.                                     *
 ***********************************************************************************************************/

public class AssemFlowGraph extends FlowGraph
{
  // esta tabela associará rótulos com nós (onde estes rótulos são definidos)
  private java.util.Dictionary labelsTable = new java.util.Hashtable();
  // esta tabela associará instruções assembly a nós
  private java.util.Dictionary instrTable = new java.util.Hashtable();

  /* construtor AssemFlowGraph (InstrList instrs)
   *    a partir de um conjunto de instruções, cria um grafo representando o fluxo do programa; para isso,
   *    utilliza a informação sobre possíveis desvios que cada instrução assembly pode realizar.
   */
  public AssemFlowGraph (InstrList instrs) {
    Node last = null;
    for ( ; instrs!=null; instrs=instrs.tail) {
      Node node;
      // obtém um nó para esta instrução
      if (instrs.head instanceof LABEL)
	node = getNodeDefiningLabel(((LABEL)instrs.head).label);
      else
	node = newNode();
      instrTable.put(node,instrs.head);
      // cria uma aresta do nó da instrução anterior para o nó da atual
      if (last != null)
	addEdge(last,node);
      last = node;
      // verifica se esta instrução assembly faz desvio para algum rótulo
      if (instrs.head instanceof OPER && ((OPER)instrs.head).jumps()!=null) {
	// cria uma aresta entre este nó e todos aqueles para o qual a instrução assembly pode desviar
	OPER oper = (OPER) instrs.head;
	for (LabelList ll=oper.jumps().labels; ll!=null; ll=ll.tail)
	  addEdge (node, getNodeDefiningLabel(ll.head));
	last = null;
      }
    }
  }

  /* Node getNodeDefiningLabel (Label label)
   *    devolve o nó associado à instrução que faz a declaração do rótulo especificado (esta declaração pode
   *    não estar presente no conjunto de instruções assembly que estão sendo analisadas).
   */
  private Node getNodeDefiningLabel (Label label) {
    Node node = (Node) labelsTable.get(label);
    if (node == null) {
      // cria um nó representando onde o rótulo foi declarado
      node = newNode();
      labelsTable.put(label, node);
    }
    return node;
  }

  /* TempList def (Node n)
   *    retorna o conjunto com os temporários definidos por esta instrução assembly (nó).
   */
  public TempList def (Node n) {
    Instr instr = ((Instr)instrTable.get(n));
    if (instr!=null)
      return instr.def();
    return null;
  }

  /* TempList def (Node n)
   *    retorna o conjunto com os temporários usados por esta instrução assembly (nó).
   */
  public TempList use (Node n){
    Instr instr = ((Instr)instrTable.get(n));
    if (instr!=null)
      return instr.use();
    return null;
  }

  /* boolean isMove (Node n)
   *    verifica se este nó está associado a uma instrução MOVE.
   */
  public boolean isMove (Node n) {
    return ((Instr)instrTable.get(n)) instanceof MOVE;
  }

  /* Instr instr (Node n)
   *    retorna a instrução assembly associada a este nó.
   */
  public Instr instr (Node n) {
    return (Instr) instrTable.get(n);
  }
}
