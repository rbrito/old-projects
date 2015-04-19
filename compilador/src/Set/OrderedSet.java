package Set;

import java.util.Dictionary;
import java.util.Hashtable;
import List.*;

/***********************************************************************************************************
 * CLASSE OrderedSet *
 * ----------------------------------------------------------
 * --------------------------------------------- * Implementa um conjunto
 * genérico. * * OBS: este conjunto pode comportar-se como uma fila ou pilha, se
 * forem usados os métodos addFirst * e addLast ao invés do método add. *
 ***********************************************************************************************************/

public class OrderedSet {
    /*
     * Detalhes sobre a implementação: Serão usadas duas estruturas: uma lista
     * duplamente ligada e uma tabela de hash. Na tabela de hash, cada elemento
     * do conjunto estará associado à sua posição na lista duplamente ligada.
     */
    Dictionary dictionary; // tabela de hash
    private DoubleLinkedList first, last; // ponteiros para o primeiro e último
                                          // elementos da lista ligada
    int size; // tamanho do conjunto (número de elementos)
    String name; // nome do conjunto (opcional)

    public boolean show = false; // flag de debug: se ativada, mostra o conjunto
                                 // a cada atualização
    public boolean showErro = true; // flag de debug: se ativada, mostra
                                    // mensagem de erro ao acrescentar um
                                    // elemento que já existe ou ao tentar
                                    // remove um elemento
                                    // que não existe no conjunto

    /*
     * construtor OrderedSet() cria um conjunto vazio, sem nome.
     */
    public OrderedSet() {
        this((String) null);
    }

    /*
     * construtor OrderedSet (String name) cria um conjunto vazio, com o nome
     * especificado.
     */
    public OrderedSet(String setName) {
        initialize();
        name = setName;
    }

    /*
     * construtor OrderedSet (List l) cria um conjunto a partir de uma lista.
     */
    public OrderedSet(List l) {
        initialize();
        for (; l != null; l = l.getNext())
            addLast(l.getInfo());
    }

    /*
     * initialize() inicializa o conjunto (o esvazia)
     */
    private void initialize() {
        first = null;
        last = null;
        size = 0;
        dictionary = new Hashtable();
    }

    /*
     * boolean isEmpty() verifica se o conjunto está vazio.
     */
    public boolean isEmpty() {
        return (size == 0);
    }

    /*
     * int size() retorna o número de elementos deste conjunto.
     */
    public int size() {
        return size;
    }

    /*
     * OrderedSet makeCopy() retorna uma cópia deste conjunto.
     */
    public OrderedSet makeCopy() {
        OrderedSet set = new OrderedSet();
        for (DoubleLinkedList p = first; p != null; p = p.getNext())
            set.add(p.getInfo());
        return set;
    }

    /*
     * boolean add (Object o) acrescenta um elemento ao conjunto (por default,
     * no final dele).
     */
    public boolean add(Object o) {
        return addLast(o);
    }

    /*
     * boolean addFirst (Object o) acrescenta um elemento ao conjunto, no
     * ínicio; retorna false se o elemento pertence a este conjunto, e true caso
     * contrário.
     */
    public boolean addFirst(Object o) {
        if (o == null || has(o)) {
            if (showErro || show)
                System.out.println("ERRO ao acrescentar " + o + " em " + this);
            return false;
        }
        if (show)
            System.out.println("  Inserindo " + o + " em " + this);
        DoubleLinkedList added = new DoubleLinkedList(o, null, first);
        if (last == null)
            last = added;
        else
            first.setPrev(added);
        first = added;
        dictionary.put(o, added);
        size++;
        if (show)
            System.out.println("   Inserido " + o + " em " + this);
        return true;
    }

    /*
     * boolean addLast (Object o) acrescenta um elemento ao conjunto, no final;
     * retorna false se o elemento pertence a este conjunto, e true caso
     * contrário.
     */
    public boolean addLast(Object o) {
        if (o == null || has(o)) {
            if (showErro || show)
                System.out.println("ERRO ao acrescentar " + o + " em " + this);
            return false;
        }
        if (show)
            System.out.println("  Inserindo " + o + " em " + this);
        DoubleLinkedList added = new DoubleLinkedList(o, last, null);
        if (first == null)
            first = added;
        else
            last.setNext(added);
        last = added;
        dictionary.put(o, added);
        size++;
        if (show)
            System.out.println("   Inserido " + o + " em " + this);
        return true;
    }

    /*
     * boolean remove (Object o) remove o elemento deste conjunto; retorna false
     * se o elemento não pertence a este conjunto, e true caso contrário.
     */
    public boolean remove(Object o) {
        if (o == null || size == 0) {
            if (showErro || show)
                System.out.println("ERRO ao remover " + o + " de " + this);
            return false;
        }
        DoubleLinkedList element = (DoubleLinkedList) dictionary.get(o);
        if (element == null) {
            if (showErro || show)
                System.out.println("ERRO ao remover " + o + " de " + this);
            return false;
        }
        if (show)
            System.out.println("  Removendo " + o + " de " + this);

        if (element.getPrev() != null)
            element.getPrev().setNext(element.getNext());
        else
            first = element.getNext();

        if (element.getNext() != null)
            element.getNext().setPrev(element.getPrev());
        else
            last = element.getPrev();

        dictionary.remove(o);
        size--;
        if (show)
            System.out.println("   Removido " + o + " de " + this);
        return true;
    }

    /*
     * boolean has (Object o) verifica se o elemento pertence a este conjunto.
     */
    public boolean has(Object o) {
        return (dictionary.get(o) != null);
    }

    /*
     * OrderedSet union (OrderedSet) altera este conjunto, fazendo a união com o
     * conjunto fornecido; retorna este conjunto.
     */
    public OrderedSet union(OrderedSet set) {
        if (set == null)
            return this;
        boolean oldshow = show;
        boolean oldshowErro = showErro;
        show = false;
        showErro = false;
        for (DoubleLinkedList p = set.first; p != null; p = p.getNext())
            addLast(p.getInfo());
        show = oldshow;
        showErro = oldshowErro;
        return this;
    }

    /*
     * OrderedSet intersection (OrderedSet) altera este conjunto, fazendo a
     * intersecção com o conjunto fornecido; retorna este conjunto.
     */
    public OrderedSet intersection(OrderedSet set) {
        if (set == null)
            return null;
        boolean oldshow = show;
        boolean oldshowErro = showErro;
        show = false;
        showErro = false;
        for (DoubleLinkedList p = first; p != null; p = p.getNext()) {
            Object info = p.getInfo();
            if (set.dictionary.get(info) == null)
                remove(info);
        }
        show = oldshow;
        showErro = oldshowErro;
        return this;
    }

    /*
     * OrderedSet difference (OrderedSet) altera este conjunto, removendo os
     * elementos deste conjunto presentes no conjunto fornecido; retorna este
     * conjunto.
     */
    public OrderedSet difference(OrderedSet set) {
        if (set == null)
            return null;
        boolean oldshow = show;
        boolean oldshowErro = showErro;
        show = false;
        showErro = false;
        for (DoubleLinkedList p = first; p != null; p = p.getNext()) {
            Object info = p.getInfo();
            if (set.dictionary.get(info) != null)
                remove(info);
        }
        show = oldshow;
        showErro = oldshowErro;
        return this;
    }

    /*
     * boolean isEqualTo (OrderedSet set) verifica se este conjunto tem os
     * mesmos elementos que o fornecido.
     */
    public boolean isEqualTo(OrderedSet set) {
        if (set == null || size != set.size)
            return false;
        for (DoubleLinkedList p = first; p != null; p = p.getNext()) {
            if (set.dictionary.get(p.getInfo()) == null)
                return false;
        }
        for (DoubleLinkedList p = set.first; p != null; p = p.getNext()) {
            if (dictionary.get(p.getInfo()) == null)
                return false;
        }
        return true;
    }

    /*
     * Object getFirst() retorna o primeiro elemento deste conjunto.
     */
    public Object getFirst() {
        if (first == null)
            return null;
        return first.getInfo();
    }

    /*
     * Object getLast() retorna o último elemento deste conjunto.
     */
    public Object getLast() {
        if (last == null)
            return null;
        return last.getInfo();
    }

    /*
     * Object getPrev (Object o) retorna o elemento deste conjunto anterior ao
     * fornecido.
     */
    public Object getPrev(Object o) {
        DoubleLinkedList e = (DoubleLinkedList) dictionary.get(o);
        if ((e == null) || (e.getPrev() == null))
            return null;
        return e.getPrev().getInfo();
    }

    /*
     * Object getPrev (Object o) retorna o elemento deste conjunto posterior ao
     * fornecido.
     */
    public Object getNext(Object o) {
        DoubleLinkedList e = (DoubleLinkedList) dictionary.get(o);
        if ((e == null) || (e.getNext() == null))
            return null;
        return e.getNext().getInfo();
    }

    /*
     * String toString() retorna uma String com a representação deste conjunto
     * (nome do conjunto + elementos).
     */
    public String toString() {
        String s = "";
        if (name != null && name != "")
            s = name + ": ";
        s = s + "{";
        DoubleLinkedList p = first;
        if (p != null) {
            s = s + p.getInfo();
            for (p = p.getNext(); p != null; p = p.getNext())
                s = s + "," + p.getInfo();
        }
        return s + "}";
    }
}
