#  Time-stamp: <Makefile (1999/07/09, 13:06:25) by alair at ime.usp.br)>  

# use GNU make for this Makefile

# O  Spim supõe que o segmento .text é alocado no endereço 0x400000 e que o
# segmento .data começa em 0x10000000.
LOCATION = -T script-spim

# The Cross-compiler is supposed to be in ftp://linux.ime.usp.br/pub/src/grad/compilador/alair
GCCDIR = /usr/local/cross/decstation-ultrix/bin
CC = $(GCCDIR)/gcc
AS = $(GCCDIR)/as
LD = $(GCCDIR)/ld
OBJDUMP = /usr/local/cross/bin/decstation-ultrix-objdump
SYMBOLS = $(OBJDUMP) --syms

#C pre processor
CPP = /lib/src/grad/compilador/cpp

#the special version of spim that accepts binaries is supposed to be in
# ftp://linux.ime.usp.br/pub/src/grad/compilador/alair
SIMULA = spim -execute


CAT = /bin/cat
RM = /bin/rm

#parameters for assembler and linker
ASFLAGS = 
LDFLAGS = $(LOCATION) -N 



# implicit declarations
%.s: %.asm
	$(CPP) $(CPPFLAGS) $< > $@

#%.s: %.c
#	$(CC) $(CFLAGS) -S -o $@ $<

#%.o: %.c
#	$(CC) -c $(CFLAGS) $(CPPFLAGS) -o $@ $<

%.s: %.tig
	./compila $< $@

%.o: %.s
#	$(AS) $(ASFLAGS) -o $@ $<
	$(CAT) $< | ./limpareg > $<.tmp
	echo ' .globl tigermain' >> $<.tmp
	$(AS) $(ASFLAGS) -o $@ $<.tmp
	$(RM) $<.tmp 2>/dev/null

#%: %.o start.o
%: %.o start.o runtime.o
#	$(LD) $(LDFLAGS) -o $@ $< start.o
	$(LD) $(LDFLAGS) -o $@ $< start.o runtime.o
#	$(SYMBOLS) $@

%: %.s start.o runtime.o
	$(CAT) $< | ./limpareg > $<.tmp
	echo ' .globl tigermain' >> $<.tmp
	$(AS) $(ASFLAGS) -o $*.o $<.tmp
	$(RM) $<.tmp 2>/dev/null
	$(LD) $(LDFLAGS) -o $@ $*.o start.o runtime.o

#target variables
TIGS=$(shell /bin/ls *.tig)
ASMS=start.s runtime.s $(TIGS:.tig=.s)
OBJS=$(ASMS:.s=.o)
EXES=$(TIGS:.tig=)

# main rules
all: runtime.s $(ASMS) #$(EXES)

asms: runtime.s $(ASMS)

exes: $(EXES)

runtime.s: runtime.s.gz
	zcat $< > $@

# depend

start.s: start.asm spim-syscall.h

start.o: start.s

clean:
	-$(RM) *.o *.tmp *.s $(EXES) 2>/dev/null
