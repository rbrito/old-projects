package Parse;

%% 

%implements Lexer
%function nextToken
%type java_cup.runtime.Symbol
%char

%{

int comments = 0;
int in_string = 0;
StringBuffer string;

private void newline() {
  errorMsg.newline(yychar);
}

private void err(int pos, String s) {
  errorMsg.error(pos,s);
}

private void err(String s) {
  err(yychar,s);
}

private java_cup.runtime.Symbol tok(int kind, Object value) {
  return new java_cup.runtime.Symbol(kind, yychar, yychar+yylength(), value);
}

private ErrorMsg.ErrorMsg errorMsg;

Yylex(java.io.InputStream s, ErrorMsg.ErrorMsg e) {
  this(s);
  errorMsg=e;
}

%}

%eofval{
	{
	  if (comments != 0) {
	    err("Erro: comentarios nao balanceados");
	  }
	  if (in_string != 0) {
	    err("Erro: string nao terminada");
	  }
	  return tok(sym.EOF, null);
        }
%eofval}      

%line

space=" "|\t|\f

digits=[0-9]+
id=[a-zA-Z][a-zA-Z0-9_]*

string_text=[^\"\\\n]*

string_3de=\\[0-9][0-9][0-9]

comment_text=([^/*\n]|[^*\n]"/"[^*\n]|[^/\n]"*"[^/\n]|"*"[^/\n]|"/"[^*\n])*

%state COMMENT, STRING, SPACE

%%

{space}			{ }
\n			{newline();}

<YYINITIAL>"/*"		{comments++; yybegin(COMMENT);}
<YYINITIAL>"\""		{string = new StringBuffer(); in_string = 1;
			 yybegin(STRING);}

<COMMENT>"/*"		{comments++;}
<COMMENT>{comment_text}	{ }
<COMMENT>"*/"		{if (--comments == 0) { yybegin(YYINITIAL); }}

<STRING>{string_text}	{string.append(yytext());}
<STRING>\\\"|\\\\	{string.append(yytext().charAt(1));}
<STRING>"\n"		{string.append("\n");}
<STRING>"\t"		{string.append("\t");}
<STRING>"\^"[a-z]	{string.append((char) (yytext().charAt(2) - 'a' + 1));}
<STRING>{string_3de}	{Integer aux = new Integer(yytext().substring(1, 
			 yytext().length()));
                         if (aux.shortValue() > 255)
			 System.err.println(
				"Aviso: caracter ASCII maior que 255 na linha " +
				(yyline + 1) + ". Considerando: <" +
				(char) aux.shortValue() + ">.");
			 string.append((char) aux.shortValue());}
<STRING>"\^"		{System.err.println("Aviso: caracteres \\^ isolados " +
			 "em string");}
<STRING>"\""		{yybegin(YYINITIAL); in_string = 0;
			 return tok(sym.STRING, string.toString());}
<STRING>\\{space}	{yybegin(SPACE);}
<STRING>\\"\n"		{newline(); yybegin(SPACE);}

<YYINITIAL>","		{return tok(sym.COMMA, null);}
<YYINITIAL>"="		{return tok(sym.EQ, null);}
<YYINITIAL>"&"		{return tok(sym.AND, null);}
<YYINITIAL>"("		{return tok(sym.LPAREN, null);}
<YYINITIAL>")"		{return tok(sym.RPAREN, null);}
<YYINITIAL>"*"		{return tok(sym.TIMES, null);}
<YYINITIAL>"+"		{return tok(sym.PLUS, null);}
<YYINITIAL>","		{return tok(sym.COMMA, null);}
<YYINITIAL>"-"		{return tok(sym.MINUS, null);}
<YYINITIAL>"."		{return tok(sym.DOT, null);}
<YYINITIAL>"/"		{return tok(sym.DIVIDE, null);}
<YYINITIAL>":"		{return tok(sym.COLON, null);}
<YYINITIAL>":="		{return tok(sym.ASSIGN, null);}
<YYINITIAL>";"		{return tok(sym.SEMICOLON, null);}
<YYINITIAL>"<"		{return tok(sym.LT, null);}
<YYINITIAL>"<="		{return tok(sym.LE, null);}
<YYINITIAL>"<>"		{return tok(sym.NEQ, null);}
<YYINITIAL>">"		{return tok(sym.GT, null);}
<YYINITIAL>">="		{return tok(sym.GE, null);}
<YYINITIAL>"["		{return tok(sym.LBRACK, null);}
<YYINITIAL>"]"		{return tok(sym.RBRACK, null);}
<YYINITIAL>"{"		{return tok(sym.LBRACE, null);}
<YYINITIAL>"|"		{return tok(sym.OR, null);}
<YYINITIAL>"}"		{return tok(sym.RBRACE, null);}

<YYINITIAL>array	{return tok(sym.ARRAY, null);}
<YYINITIAL>break	{return tok(sym.BREAK, null);}
<YYINITIAL>do		{return tok(sym.DO, null);}
<YYINITIAL>else		{return tok(sym.ELSE, null);}
<YYINITIAL>end		{return tok(sym.END, null);}
<YYINITIAL>for		{return tok(sym.FOR, null);}
<YYINITIAL>function	{return tok(sym.FUNCTION, null);}
<YYINITIAL>if		{return tok(sym.IF, null);}
<YYINITIAL>in		{return tok(sym.IN, null);}
<YYINITIAL>let		{return tok(sym.LET, null);}
<YYINITIAL>nil		{return tok(sym.NIL, null);}
<YYINITIAL>of		{return tok(sym.OF, null);}
<YYINITIAL>then		{return tok(sym.THEN, null);}
<YYINITIAL>to		{return tok(sym.TO, null);}
<YYINITIAL>type		{return tok(sym.TYPE, null);}
<YYINITIAL>var		{return tok(sym.VAR, null);}
<YYINITIAL>while	{return tok(sym.WHILE, null);}

<YYINITIAL>{id}		{return tok(sym.ID, yytext());}
<YYINITIAL>{digits}	{return tok(sym.INT, new Integer(yytext()));}

<SPACE>{space}+		{/* nao colocamos regra para \n porque ela e' a mesma
			    independentemente do estado */}
<SPACE>\\		{yybegin(STRING);}
<SPACE>.		{System.err.println("Aviso: entrada invalida na linha " +
			 (yyline + 1) + ": <" + yytext() + ">");
			 string.append(yytext().charAt(0)); yybegin(STRING);}

.			{System.out.println("Unmatched input at line " + 
			 (yyline + 1) + ": <" + yytext() + ">");}



