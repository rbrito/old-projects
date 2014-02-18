#!/bin/sh

for f in test/*.tig; do
	echo $f
	java -classpath /usr/share/java/cup.jar: Main.Main $f
done
