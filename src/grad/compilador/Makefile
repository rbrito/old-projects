JFLAGS=-g
EXPECT=6
#JIT=-Djava.compiler=tya
JIT=

all: Parse/Parse.class Semant/Semant.class Types/Type.class Absyn/Absyn.class ErrorMsg/ErrorMsg.class Symbol/Symbol.class Translate/Translate.class Temp/Temp.class Util/BoolList.class Frame/Frame.class Mips/Frame.class Tree/Exp.class Assem/Instr.class Canon/Canon.class Main/Main.class FlowGraph/AssemFlowGraph.class Graph/Graph.class RegAlloc/Liveness.class FindEscape/FindEscape.class Set/PairOrderedSet.class List/List.class

Parse/Parse.class: Parse/*.java Parse/Grm.java Parse/Yylex.java
	javac ${JFLAGS} Parse/*.java

Parse/Grm.java: Parse/Grm.cup
	cd Parse; java ${JIT} java_cup.Main -parser Grm -expect ${EXPECT} -dump_grammar -dump_states <Grm.cup >Grm.out 2>Grm.err

Parse/Yylex.java: Parse/Tiger.lex
	cd Parse; java ${JIT} JLex.Main Tiger.lex; mv Tiger.lex.java Yylex.java

Semant/Semant.class: Semant/*.java
	javac ${JFLAGS} Semant/*.java

Types/Type.class: Types/*.java
	javac ${JFLAGS} Types/*.java

Absyn/Absyn.class: Absyn/*.java
	javac ${JFLAGS} Absyn/*.java

ErrorMsg/ErrorMsg.class:  ErrorMsg/*.java
	javac ${JFLAGS} ErrorMsg/*.java

Symbol/Symbol.class: Symbol/*.java
	javac ${JFLAGS} Symbol/*.java

Translate/Translate.class: Translate/*.java
	javac ${JFLAGS} Translate/*.java

Temp/Temp.class: Temp/*.java
	javac ${JFLAGS} Temp/*.java

Util/BoolList.class: Util/*.java
	javac ${JFLAGS} Util/*.java

Frame/Frame.class: Frame/*.java
	javac ${JFLAGS} Frame/*.java

Mips/Frame.class: Mips/*.java
	javac ${JFLAGS} Mips/*.java

Tree/Exp.class: Tree/*.java
	javac ${JFLAGS} Tree/*.java

Assem/Instr.class: Assem/*.java
	javac ${JFLAGS} Assem/*.java

Canon/Canon.class: Canon/*.java
	javac ${JFLAGS} Canon/*.java

Main/Main.class: Main/*.java
	javac ${JFLAGS} Main/*.java

FlowGraph/AssemFlowGraph.class: FlowGraph/*.java
	javac ${JFLAGS} FlowGraph/*.java

Graph/Graph.class: Graph/*.java
	javac ${JFLAGS} Graph/*.java

RegAlloc/Liveness.class: RegAlloc/*.java
	javac ${JFLAGS} RegAlloc/*.java

FindEscape/FindEscape.class: FindEscape/*.java
	javac ${JFLAGS} FindEscape/*.java

Set/PairOrderedSet.class: Set/*.java
	javac ${JFLAGS} Set/*.java

List/List.class: List/*.java
	javac ${JFLAGS} List/*.java

clean:
	find . -name "*.class" -print | xargs rm -f

cleanall: clean
	find . -name "*~" -print | xargs rm -f
	rm -f Parse/Grm.java Parse/sym.java Parse/Grm.err Parse/Grm.out Parse/Yylex.java testes/*.tig.s

