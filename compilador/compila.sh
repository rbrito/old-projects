#!/bin/sh

for f in test/*.tig; do
	echo $f
	java -classpath /usr/share/java/src/grad/compilador/cup.jar: Main.Main $f
done
