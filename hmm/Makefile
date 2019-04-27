#
# Makefile simplificado feito por Rogério Theodoro de Brito em 2000-01-31.
# Ultima atualizacao: 2019-04-27.
#

CC = gcc
PROG = hmm
SRCS = $(PROG).c dado.c
OBJS = $(SRCS:.c=.o)
CFLAGS = -Wall -Wextra -Wstrict-prototypes -ansi -pedantic -O2

all: $(PROG)

$(PROG): $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) -o $@

.c.o:
	$(CC) $(CFLAGS) -c $<

clean:
	rm -f $(PROG) $(OBJS) *~ core

# deps:
# 	$(CC) -MM $(SRCS) > deps

# distclean:
# 	rm -f $(PROG) $(OBJS) *~ core deps

# ifneq ($(wildcard deps),)
# include deps
# endif
