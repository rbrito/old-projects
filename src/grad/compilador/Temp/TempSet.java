package Temp;

import java.util.*;
import Set.*;

/***********************************************************************************************************
 *   CLASSE TempSet                                                                                        *
 * ------------------------------------------------------------------------------------------------------- *
 *   Implementa um conjunto de temporários.                                                                *
 *                                                                                                         *
 *   OBS: Esta implementação supõe que cada temporário tem um número, e cada número está associado a um    *
 *        único temporário. Assim, a partir de um número, é possível saber qual o temporário associado     *
 *        e vice-versa.                                                                                    *
 ***********************************************************************************************************/

public class TempSet
{
  private int[] elements;            // array com os elementos
  String name;                       // nome do conjunto (opcional)

  /* construtor TempSet():
   *    cria um novo conjunto, sem nome.
   */
  public TempSet() {
    this((String)null);
  }

  /* construtor TempSet (String setName)
   *    cria um novo conjunto, com o nome especificado.
   */
  public TempSet (String setName) {
    initialize(getRecommendedSize());
    name = setName;
  }

  /* construtor TempSet (String setName, int size)
   *    cria um novo conjunto, com o nome e tamanho especificados.
   */
  private TempSet (String setName, int size) {
    initialize(size);
    name = setName;
  }

  /* construtor TempSet (TempList l)
   *    cria um novo conjunto, a partir de uma lista de temporários.
   */
  public TempSet (TempList l) {
    initialize(getRecommendedSize());
    for ( ; l!=null; l=(TempList)l.getNext()) {
      int n = l.head.getNum();
      int index = n/32;
      int position = n%32;
      elements[index] = elements[index] | (1<<(31-position));
    }
  }

  /* construtor TempSet (OrderedSet set)
   *    cria um novo conjunto, a partir de um conjunto OrderedSet de temporários.
   */
  public TempSet (OrderedSet set) {
    initialize(getRecommendedSize());
    for (Temp t=(Temp)set.getFirst(); t!=null; t=(Temp)set.getNext(t)) {
      int n = t.getNum();
      int index = n/32;
      int position = n%32;
      elements[index] = elements[index] | (1<<(31-position));
    }
  }

  /* construtor TempSet (TempSet set)
   *    cria um novo conjunto, idêntico ao conjunto fornecido.
   */
  public TempSet (TempSet set) {
    initialize(set.elements.length);
    name = set.name;
    for (int i=0; i<set.elements.length; i++)
      elements[i] = set.elements[i];
  }

  /* initialize (int size)
   *     cria um array do tamanho especificado para guardar os elementos.
   */
  private void initialize (int size) {
    if (size<=0)
      size = 1;
    elements = new int[size];
    for (int i=0; i<elements.length; i++)
      elements[i] = 0;
  }

  /* int getRecommendedSize()
   *    recomenda um tamanho para o conjunto (ié, 2 vezes o tamanho necessário para guardar todos os
   *    temporários criados até o momento, para evitar que o conjunto precise ser redimensionado ao
   *    acrescentar elementos.
   */
  private static int getRecommendedSize() {
    return ((Temp.getCount()/32)+1)*2;
  }

  /* TempSet makeCopy()
   *    cria uma cópia deste conjunto.
   */
  public TempSet makeCopy() {
    return new TempSet(this);
  }

  /* makeRoom (int size)
   *    redimensiona o array que armazena os elementos para o tamanho especificado.
   */
  private void makeRoom (int newSize) {
    int[] newArray = new int[newSize];
    for (int i=0; i<elements.length; i++)
      newArray[i] = elements[i];
    for (int i=elements.length; i<newSize; i++)
      newArray[i] = 0;
    elements = newArray;
  }

  /* makeRoom()
   *    redimensiona o tamanho do array que armazena os elementos.
   */
  private void makeRoom() {
    makeRoom(getRecommendedSize());
  }

  /* boolean isEmpty()
   *    verifica se este conjunto está vazio.
   */
  public boolean isEmpty() {
    for (int i=0; i<elements.length; i++) {
      if (elements[i] != 0)
	return false;
    }
    return true;
  }

  /* boolean add (Temp t)
   *    acrescenta este temporário ao conjunto; retorna false se o elemento já estava no conjunto, e true,
   *    caso contrário.
   */
  public boolean add (Temp t) {
    if (t == null)
      return false;
    int num = t.getNum();
    int index = num/32;
    int position = 31-(num%32);
    int mask = (1<<position);
    if (elements.length <= index)
      makeRoom();
    if ((elements[index] & mask) == 0) {
      elements[index] = elements[index] | mask;
      return true;
    }
    return false;
  }

  /* boolean remove (Temp t)
   *    remove o temporário deste conjunto; retorna false se o elemento não estava presente no conjunto, e
   *    true caso contrário.
   */
  public boolean remove (Temp t) {
    if (t == null)
      return false;
    int num = t.getNum();
    int index = num/32;
    if (index < elements.length) {
      int position = 31-(num%32);
      int mask = (1<<position);
      if ((elements[index] & mask) != 0) {
        elements[index] = elements[index] ^ mask;
        return true;
      }
    }
    return false;
  }

  /* boolean has (Temp t)
   *    verifica se o temporário pertence a este conjunto.
   */
  public boolean has (Temp t) {
    if (t == null)
      return false;
    int num = t.getNum();
    int index = num/32;
    if (index < elements.length) {
      int position = 31-(num%32);
      int mask = (1<<position);
      if ((elements[index] & mask) != 0)
        return true;
    }
    return false;
  }

  /* int size()
   *    devolve o número de elementos deste conjunto.
   */
  public int size() {
    int count = 0;
    for (int i=0; i<elements.length; i++) {
      if (elements[i] != 0) {
	int aux = elements[i];
        for (int j=0; j<32; j++) {
	  if ((aux & 1) != 0) count++;
	  aux = aux >> 1;
	}
      }
    }
    return count;
  }

  /* TempSet union (TempSet set)
   *    retorna o conjunto resultado a união deste conjunto com o fornecido.
   */
  public TempSet union (TempSet set) {
    if (set == null)
      return makeCopy();

    int minSize, maxSize;   // tamanho do menor e do maior conjunto
    TempSet greaterSet;     // o conjunto que tem mais elementos
    if (elements.length > set.elements.length) {
      maxSize = elements.length;
      minSize = set.elements.length;
      greaterSet = this;
    }
    else {
      maxSize = set.elements.length;
      minSize = elements.length;
      greaterSet = set;
    }
    TempSet unionSet = new TempSet();
    for (int i=0; i<minSize; i++)
      unionSet.elements[i] = (elements[i] | set.elements[i]);
    for (int i=minSize; i<maxSize; i++)
      unionSet.elements[i] = greaterSet.elements[i];
    return unionSet;
  }

  /* TempSet intersection (TempSet set)
   *    retorna o conjunto resultado a intersecção deste conjunto com o fornecido.
   */
  public TempSet intersection (TempSet set) {
    if (set == null)
      return new TempSet();

    int minSize, maxSize;   // tamanho do menor e do maior conjunto
    if (elements.length > set.elements.length) {
      maxSize = elements.length;
      minSize = set.elements.length;
    }
    else {
      maxSize = set.elements.length;
      minSize = elements.length;
    }
    TempSet interSet = new TempSet();
    for (int i=0; i<minSize; i++)
      interSet.elements[i] = (elements[i] & set.elements[i]);
    return interSet;
  }

  /* TempSet difference (TempSet set)
   *    retorna o conjunto resultado a diferença entre este conjunto e o fornecido.
   */
  public TempSet difference (TempSet set) {
    if (set == null)
      return makeCopy();

    int minSize, maxSize;    // tamanho do menor e do maior conjunto
    if (elements.length > set.elements.length) {
      maxSize = elements.length;
      minSize = set.elements.length;
    }
    else {
      maxSize = set.elements.length;
      minSize = elements.length;
    }
    TempSet diffSet = new TempSet();
    for (int i=0; i<minSize; i++)
      diffSet.elements[i] = (elements[i] & (0xFFFFFFFF^set.elements[i]));
    if (set.elements.length == minSize)
      for (int i=minSize; i<maxSize; i++)
	diffSet.elements[i] = elements[i];
    return diffSet;
  }

  /* boolean isEqualTo (TempSet set)
   *    verifica se este conjunto e o fornecido têm os mesmos elementos.
   */
  public boolean isEqualTo (TempSet set) {
    if (set == null)
      return false;

    int minSize, maxSize;    // tamanho do menor e do maior conjunto
    TempSet greaterSet;      // o conjunto com mais elementos
    if (elements.length > set.elements.length) {
      maxSize = elements.length;
      minSize = set.elements.length;
      greaterSet = this;
    }
    else {
      maxSize = set.elements.length;
      minSize = elements.length;
      greaterSet = set;
    }
    TempSet diffSet = new TempSet();
    for (int i=0; i<minSize; i++) {
      if (elements[i] != set.elements[i])
        return false;
    }
    for (int i=minSize; i<maxSize; i++) {
      if (greaterSet.elements[i] != 0)
        return false;
    }
    return true;
  }

  /* int getProx (int i)
   *    devolve o número do próximo temporário, a partir da posição i; caso não existe, devolve -1.
   */
  private int getProx (int i) {
    if (i<0)
      i = 0;
    else
      i++;
    int maxTemps = 32*elements.length;
    while (i<maxTemps) {
      int index = i/32;
      int position = i%32;
      if ((elements[index] & (1<<(31-position))) != 0)
	return i;
      // como o elemento não foi encontrado, tenta acelerar a busca (útil em conjunto com poucos elementos)
      if ((position == 0) && (elements[index] == 0)) {
	i += 32;
	continue;
      }
      if (position % 16 == 0) {
	int copy = (elements[index] << position);
	if ((copy & 0xFFFF0000) == 0) {
	  i += 16;
	  continue;
	}
      }
      i++;
    }
    return -1;
  }

  /* int getPrev (int i)
   *    devolve o número do temporário anterior, a partir da posição i; caso não exista, devolve -1.
   */
  private int getPrev (int i) {
    int maxTemps = 32*elements.length;
    if (i >= maxTemps)
      i = maxTemps-1;
    else
      i--;
    while (i>=0) {
      int index = i/32;
      int position = i%32;
      if ((elements[index] & (1<<(31-position))) != 0)
	return i;
      // como o elemento não foi encontrado, tenta acelerar a busca (útil em conjunto com poucos elementos)
      if ((position == 31) && (elements[index] == 0)) {
	i -= 32;
	continue;
      }
      if ((position == 15)|| (position == 31)) {
	int copy = (elements[index] << (position-15));
	if ((copy & 0xFFFF0000) == 0) {
	  i -= 16;
	  continue;
	}
      }
      i--;
    }
    return -1;
  }

  /* TempSet getFirst()
   *    retorna o primeiro temporário deste conjunto (o de menor número).
   */
  public Temp getFirst() {
    int at = getProx(-1);
    if (at == -1)
      return null;
    return Temp.getTemp(at);
  }

  /* TempSet getLast()
   *    retorna o último temporário deste conjunto (o de maior número).
   */
  public Temp getLast() {
    int at = getPrev(elements.length*32);
    if (at == -1)
      return null;
    return Temp.getTemp(at);
  }

  /* TempSet getPrev (Temp t)
   *    retorna o temporário anterior ao fornecido (o de número imediatamente menor ao fornecido).
   */
  public Temp getPrev (Temp t) {
    if (t==null)
      return null;
    int at = getPrev(t.getNum());
    if (at == -1)
      return null;
    return Temp.getTemp(at);
  }

  /* TempSet getNext (Temp t)
   *    retorna o próximo temporário ao fornecido (o de número imediatamente maior ao fornecido).
   */
  public Temp getNext (Temp t) {
    if (t==null)
      return null;
    int at = getProx(t.getNum());
    if (at == -1)
      return null;
    return Temp.getTemp(at);
  }

  /* String toString()
   *    retorna uma String com a representação deste conjunto (nome do conjunto + elementos).
   */
  public String toString() {
    String s = "";
    if ( (name != null) && (name != "") )
      s = name+": ";
    s = s+"{";
    Temp t = getFirst();
    if (t != null) {
      s = s+t;
      for (t=getNext(t); t!=null; t=getNext(t))
	s = s+","+t;
    }
    return s+"}";
  }
}
