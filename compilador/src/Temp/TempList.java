package Temp;

public class TempList implements List.List {
    public Temp head;
    public TempList tail;

    public TempList(Temp h, TempList t) {
        head = h;
        tail = t;
    }

    public String toString() {
        if (tail == null)
            return head.toString();
        else
            return head.toString() + "," + tail.toString();
    }

    public Object getInfo() {
        return head;
    }

    public List.List getNext() {
        return tail;
    }
}
