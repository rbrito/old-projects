package Parse;

public class Parse {
    public Absyn.Print p = new Absyn.Print(System.out);

    public ErrorMsg.ErrorMsg errorMsg;
    public Absyn.Exp absyn;
  
    public Parse(String filename, ErrorMsg.ErrorMsg errorMsg) {
	//errorMsg = new ErrorMsg.ErrorMsg(filename);
	{
	    java.io.InputStream inp;
	    try {
		inp=new java.io.FileInputStream(filename);
	    } catch (java.io.FileNotFoundException e) {
		throw new Error("File not found: " + filename);
	    }
	    Grm parser = new Grm(new Yylex(inp,errorMsg), errorMsg);
	    /* open input files, etc. here */
      
	    try {
		try {
		    //parser./*debug_*/parse();
		    absyn=(Absyn.Exp)parser./*debug_*/parse().value;
		} catch (java.lang.Exception e) { // pegar erros de sintaxe
		    //System.out.println(e);
		    System.exit(1);
		}
	    } catch (Throwable e) {
		e.printStackTrace();
		throw new Error(e.toString());
	    } 
	    finally {
		try {
		    inp.close();
		} catch (java.io.IOException e) {}
	    }
	    //absyn=parser.parseResult;
	    //p.prExp(absyn, 1); System.out.println("");
	}
    }

    public Absyn.Exp raizabsyn() {
	return absyn;
    }
  
}

