package List;

public class DoubleLinkedList
{
  private Object info;
  private DoubleLinkedList prev, next;

  public DoubleLinkedList (Object o, DoubleLinkedList prev, DoubleLinkedList next) {
    info = o;
    this.prev = prev;
    this.next = next;
  }

  public DoubleLinkedList (Object o) { this(o,null,null); }

  public Object getInfo() {return info;}
  public DoubleLinkedList getPrev() {return prev;}
  public DoubleLinkedList getNext() {return next;}
  public void setInfo (Object o) {info = o;}
  public void setPrev (DoubleLinkedList p) {prev = p;}
  public void setNext (DoubleLinkedList n) {next = n;}
}
