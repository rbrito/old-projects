#!/bin/sh

for f in test/*.tig; do
	echo $f
	java Main.Main $f
done
