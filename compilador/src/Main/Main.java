package Main;

import Semant.Semant;
//import Translate.Translate;
//import Parse.Parse;

class Main {
    static Frame.Frame frame = new Mips.MipsFrame(new Temp.Label("tigermain"));

    static boolean deb = false;

    static void prStmList(Tree.Print print, Tree.StmList stms) {
        for (Tree.StmList l = stms; l != null; l = l.tail)
            print.prStm(l.head);
    }

    // Imprime mensagem de help na saida de erros
    static void help() {
        System.err.println("  Compilador Tiger");
        System.err.println();
        System.err.println("  java Main.Main [-v] [-h] <arquivo.tig>");
        System.err.println();
        System.err.println("  Opcoes:");
        System.err.println("  -v     liga o modo \"verbose\" do compilador");
        System.err.println("  -h     imprime esta mensagem de help");
        System.err.println();
        System.exit(1);
    }

    static Assem.InstrList codegen(Frame.Frame f, Tree.StmList stms) {
        Assem.InstrList first = null, last = null;

        for (Tree.StmList s = stms; s != null; s = s.tail) {
            Assem.InstrList i = f.codegen(s.head);

            if (i == null)
                continue;

            if (last == null) {
                first = last = i;
            } else {
                while (last.tail != null)
                    last = last.tail;
                last = last.tail = i;
            }
        }
        return first;
    }

    static void emitProc(java.io.PrintStream out, Translate.ProcFrag f) {
        java.io.PrintStream debug =
        /* new java.io.PrintStream(new NullOutputStream()); */
        out;
        Temp.TempMap tempmap = new Temp.CombineMap(f.frame,
                new Temp.DefaultMap());
        Tree.Print print = new Tree.Print(debug, tempmap);

        if (deb) {
            debug.println("# Before canonicalization: ");
            print.prStm(f.body);
            debug.println("# After canonicalization: ");
        }

        Tree.StmList stms = Canon.Canon.linearize(f.body);

        if (deb) {
            prStmList(print, stms);
            debug.println("# Basic Blocks: ");
        }

        Canon.BasicBlocks b = new Canon.BasicBlocks(stms);

        if (deb) {
            for (Canon.StmListList l = b.blocks; l != null; l = l.tail) {
                debug.println("#");
                prStmList(print, l.head);
            }
            print.prStm(new Tree.LABEL(b.done));
            debug.println("# Trace Scheduled: ");
        }

        Tree.StmList traced = (new Canon.TraceSchedule(b)).stms;

        if (deb) {
            prStmList(print, traced);
        }

        Assem.InstrList instrs = codegen(f.frame, traced);

        FlowGraph.AssemFlowGraph afg = null; // grafo de controle de fluxo
        RegAlloc.Liveness liveness = null; // grafo de interferencia

        if (deb) {
            debug.println("# Instructions: ");
            for (Assem.InstrList p = instrs; p != null; p = p.tail) {
                debug.print(p.head.format(tempmap));
            }
        }

        afg = new FlowGraph.AssemFlowGraph(instrs);

        if (deb) {
            debug.println("# AssemFlowGraph: ");
            afg.show(debug);
        }
        liveness = new RegAlloc.Liveness(afg);

        if (deb) {
            debug.println("# Interference Graph: ");
            liveness.show(debug);
        }

        RegAlloc.RegAlloc ra = new RegAlloc.RegAlloc(f.frame, instrs);
        Frame.Proc frameproc;

        debug.println("# Instructions after Register Allocation: ");

        frameproc = f.frame.procEntryExit3(instrs); // construimos o corpo da
                                                    // funcao

        debug.print(frameproc.prologue); // imprimimos o prologo

        for (Assem.InstrList p = frameproc.body; p != null; p = p.tail) {
            debug.print(p.head.format(ra)); // instrucoes do fragmento
        }

        debug.print(frameproc.epilogue); // imprimimos o epilogo

    }

    public static void main(String args[]) throws java.io.IOException {
        int i;
        String filename = null;

        // Para cada argumento, verificamos se ele e' uma opcao da
        // linha de comando -- paramos a analise quando
        // encontrarmos o primeiro "nao-parametro", que assumimos
        // ser um arquivo.
        for (i = 1; i <= args.length; i++) {
            if (args[i - 1].equals("-v"))
                deb = true; // verbose on
            else if (args[i - 1].equals("-h"))
                help(); // mensagem de help
            else
                break; // nao e' opcao de linha de comando -> e' arquivo
        }

        if (i <= args.length) { // o laco parou antes de acabarem os args
            filename = args[i - 1];
        } else {
            help();
        }

        ErrorMsg.ErrorMsg msg = new ErrorMsg.ErrorMsg(filename);

        Parse.Parse parse = new Parse.Parse(filename, msg);

        // Absyn.Exp absyn = parse.raizabsyn();
        // Absyn.Print p = new Absyn.Print(System.out);

        // imprime a arvore abstrata
        // p.prExp(absyn, 1); System.out.println("");

        java.io.PrintStream out = new java.io.PrintStream(
                new java.io.FileOutputStream(filename + ".s"));
        Translate.Translate translate = new Translate.Translate(frame);
        Translate.Level l = new Translate.Level(frame);

        // Semant semant = new Semant(parse.errorMsg,l,translate);
        Semant semant = new Semant(msg, l, translate);
        Translate.Frag frags = semant.transProg(parse.absyn);

        for (Translate.Frag f = frags; f != null; f = f.next)
            if (f instanceof Translate.ProcFrag) {
                // imprime codigo a partir do fragmento f
                emitProc(out, (Translate.ProcFrag) f);
            } else if (f instanceof Translate.DataFrag)
                out.print(((Translate.DataFrag) f).data);

        out.close();
    }

}

class NullOutputStream extends java.io.OutputStream {
    public void write(int b) {
    }
}
