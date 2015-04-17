JFLAGS=-g
EXPECT=6
#JIT=-Djava.compiler=tya
JIT=
export CLASSPATH=/usr/share/java/cup.jar:.

CLASSES=$(patsubst %.java,%.class,$(wildcard src/*/*.java))

all: $(CLASSES)

%.class: %.java
	cd src; javac ${JFLAGS} $(subst src/,,$<)


## Automatically generated files
src/Parse/Grm.java: src/Parse/Grm.cup
	cd src/Parse; cup -parser Grm -expect ${EXPECT} -dump_grammar -dump_states <Grm.cup >Grm.out 2>Grm.err

src/Parse/Yylex.java: src/Parse/Tiger.lex
	cd src/Parse; jlex Tiger.lex; mv Tiger.lex.java Yylex.java


# Dependent on automatically generated files
src/Parse/Parse.class: src/Parse/*.java src/Parse/Grm.java src/Parse/Yylex.java
	cd src; javac ${JFLAGS} Parse/*.java

clean:
	find . -name "*.class" -print | xargs rm -f

cleanall: clean
	find . -name "*~" -print | xargs rm -f
	rm -f Parse/Grm.java Parse/sym.java Parse/Grm.err Parse/Grm.out Parse/Yylex.java testes/*.tig.s


.SUFFIXES: .java .class
