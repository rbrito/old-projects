#!/usr/bin/perl -p

while (/t(\d+)/g) {
    next if $1 < 32;
    $reg = $1 - 30;

    print "Ooppps\n" if $reg > 9;

    s/t$1/\$t$reg/;
}
