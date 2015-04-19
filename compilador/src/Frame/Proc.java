package Frame;

public class Proc {
    public String prologue;
    public String epilogue;
    public Assem.InstrList body;

    public Proc(String p, Assem.InstrList b, String e) {
        prologue = p;
        body = b;
        epilogue = e;
    }
}
