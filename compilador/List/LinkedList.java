package List;

public class LinkedList
{
  private Object info;
  private LinkedList next;

  public LinkedList (Object o, LinkedList next) {
    info = o;
    this.next = next;
  }

  public LinkedList (Object o) { this(o,null); }

  public Object getInfo() {return info;}
  public LinkedList getNext() {return next;}
  public void setInfo (Object o) {info = o;}
  public void setNext (LinkedList n) {next = n;}
}
