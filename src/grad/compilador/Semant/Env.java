package Semant;

import Temp.Label;

class Env {
  Symbol.Table venv; // environment de valores (variaveis, funcoes)
  Symbol.Table tenv; // environment de tipos
  ErrorMsg.ErrorMsg errorMsg;

  Types.INT    INT;
  Types.STRING STRING;
  Types.VOID   VOID;
  Types.NIL    NIL;

  Env(ErrorMsg.ErrorMsg err, Translate.Level level) {
    venv = new Symbol.Table();
    tenv = new Symbol.Table();

    errorMsg=err; // mensagens de erro

    //    venv.beginScope(); // FIXME: necessario?
    //tenv.beginScope(); // FIXME: necessario?

    INT    = new Types.INT();
    STRING = new Types.STRING();
    VOID   = new Types.VOID();
    NIL    = new Types.NIL();

    /* quando verificamos se uma chamada a uma funcao foi feita
       corretamente estamos apenas interessados no tipo do parametro e
       no tipo do argumento passsado como parametro -- portanto,
       usaremos simbolos ficticios na declaracao dos parametros
       (formais) de funcoes; observe-se que nao e' necessario chamar
       os metodos beginScope() de venv e de tenv neste caso, uma vez
       que s e i apenas foram inseridas nas tabelas de simbolos -- nao
       ha' tipos associados a elas -- alias, a linguagem Tiger permite
       redefinicao de variaveis o que significa que isso nao deveria
       causar nenhuma preocupacao, mesmo que s e i possuissem tipos */
    Symbol.Symbol s = Symbol.Symbol.symbol("s"); // param. ficticio string
    Symbol.Symbol i = Symbol.Symbol.symbol("i"); // param. ficticio int

    // int
    tenv.put(Symbol.Symbol.symbol("int"), INT);
    // string
    tenv.put(Symbol.Symbol.symbol("string"), STRING);


    // function print(s: string)
    venv.put(Symbol.Symbol.symbol("print"),
	     new FunEntry(level,
			  new Label("print"), new Types.RECORD(s, STRING, null),
			  VOID));
    
    // function flush()
    venv.put(Symbol.Symbol.symbol("flush"),
	     new FunEntry(level,
			  new Label("flush"), null, VOID));

    // function getchar():string
    venv.put(Symbol.Symbol.symbol("getchar"),
	     new FunEntry(level, new Label("getchar"), null, STRING));

    // function ord(s: string): int
    venv.put(Symbol.Symbol.symbol("ord"),
	     new FunEntry(level, new Label("ord"),
			  new Types.RECORD(s, STRING, null), INT));

    // function chr(i: int): string
    venv.put(Symbol.Symbol.symbol("chr"),
	     new FunEntry(level, new Label("chr"),
			  new Types.RECORD(i, INT, null), STRING));

    // function size(s: string): int
    venv.put(Symbol.Symbol.symbol("size"),
	     new FunEntry(level, new Label("size"),
			  new Types.RECORD(s, STRING, null), INT));

    // function substring(s: string, first: int, n: int) : string
    venv.put(Symbol.Symbol.symbol("substring"),
	     new FunEntry(level, new Label("substring"),
			  new Types.RECORD(s, STRING,
					   new Types.RECORD(i, INT,
							    new Types.RECORD(i,
									     INT,
									     null))), STRING));
    
    // function concat(s1:string, s2: string): string
    venv.put(Symbol.Symbol.symbol("concat"),
	     new FunEntry(level, new Label("concat"),
			  new Types.RECORD(s, STRING,
					   new Types.RECORD(s, STRING, null)),
			  STRING));

    // function not(i: integer): integer
    venv.put(Symbol.Symbol.symbol("not"),
	     new FunEntry(level, new Label("not"),
			  new Types.RECORD(i, INT, null), INT));

    // function exit(i: int)
    venv.put(Symbol.Symbol.symbol("exit"),
	     new FunEntry(level, new Label("exit"),
			  new Types.RECORD(i, INT, null), VOID));
  }
}
