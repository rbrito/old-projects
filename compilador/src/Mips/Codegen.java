package Mips;

public class Codegen {
    Frame.Frame frame;
    private Assem.InstrList ilist = null, last = null;

    private Temp.TempList calldefs = null;

    // Construtor
    public Codegen(Frame.Frame f) {
        frame = f;
    }

    // Adiciona uma instrucao inst `a lista ligada de instrucoes ja'
    // geradas.
    private void emit(Assem.Instr inst) {
        if (last != null) {
            last.tail = new Assem.InstrList(inst, null);
            last = last.tail;
        } else {
            ilist = new Assem.InstrList(inst, null);
            last = ilist;
        }
    }

    // Sugestao do livro -- Adiciona temporario h `a lista t
    private Temp.TempList L(Temp.Temp h, Temp.TempList t) {
        return new Temp.TempList(h, t);
    }

    // Verifica qual tipo de Stm estamos tratando
    void munchStm(Tree.Stm s) {
        if (s instanceof Tree.MOVE)
            munchMove(((Tree.MOVE) s).dst, ((Tree.MOVE) s).src);
        else if (s instanceof Tree.EXP)
            munchExp(((Tree.EXP) s).exp);
        else if (s instanceof Tree.JUMP)
            munchJump(((Tree.JUMP) s).exp);
        else if (s instanceof Tree.CJUMP)
            munchCjump((Tree.CJUMP) s);
        else if (s instanceof Tree.LABEL)
            munchStm((Tree.LABEL) s);
        else
            throw new Error("Erro interno: tipo invalido de Stm em munchStm.");
    }

    // Determina se o destino do Tree.MOVE e' um temporario
    // (registrador) ou memoria.
    void munchMove(Tree.Exp dst, Tree.Exp src) {
        if (dst instanceof Tree.TEMP)
            munchMove((Tree.TEMP) dst, src);
        else if (dst instanceof Tree.MEM)
            munchMove((Tree.MEM) dst, src);
    }

    // MOVE TEMP <- expressao
    void munchMove(Tree.TEMP dst, Tree.Exp src) {
        Tree.BINOP binop;
        Tree.CONST c;
        Tree.TEMP t;

        // Casos especiais
        if (src instanceof Tree.MEM) {
            if (((Tree.MEM) src).exp instanceof Tree.BINOP) {
                binop = (Tree.BINOP) ((Tree.MEM) src).exp;

                if (binop.left instanceof Tree.CONST
                        && binop.right instanceof Tree.TEMP) {
                    c = (Tree.CONST) binop.left;
                    t = (Tree.TEMP) binop.right;

                    emit(new Assem.OPER("\tlw `d0, " + c.value + "(`s0)\n", L(
                            dst.temp, null), L(t.temp, null)));

                    return;
                }

                if (binop.right instanceof Tree.CONST
                        && binop.left instanceof Tree.TEMP) {
                    c = (Tree.CONST) binop.right;
                    t = (Tree.TEMP) binop.left;

                    emit(new Assem.OPER("\tlw `d0, " + c.value + "(`s0)\n", L(
                            dst.temp, null), L(t.temp, null)));

                    return;
                }
            }
        }

        if (src instanceof Tree.BINOP) {
            binop = (Tree.BINOP) src;

            if (binop.binop == Tree.BINOP.PLUS) {
                if (binop.left instanceof Tree.CONST
                        && binop.right instanceof Tree.TEMP) {
                    c = (Tree.CONST) binop.left;
                    t = (Tree.TEMP) binop.right;

                    emit(new Assem.OPER("\taddi `d0, `s0, " + c.value + "\n",
                            L(dst.temp, null), L(t.temp, null)));

                    return;
                }

                if (binop.right instanceof Tree.CONST
                        && binop.left instanceof Tree.TEMP) {
                    c = (Tree.CONST) binop.right;
                    t = (Tree.TEMP) binop.left;

                    emit(new Assem.OPER("\taddi `d0, `s0, " + c.value + "\n",
                            L(dst.temp, null), L(t.temp, null)));

                    return;
                }
            }
        }

        if (src instanceof Tree.CONST) {
            c = (Tree.CONST) src;
            emit(new Assem.OPER("\tli `d0, " + c.value + "\n",
                    L(dst.temp, null), null));
            return;
        }

        emit(new Assem.MOVE("\tmove `d0, `s0\n", dst.temp, munchExp(src)));
    }

    // MOVE memoria <- expressao
    void munchMove(Tree.MEM dst, Tree.Exp src) {
        Tree.BINOP binop;
        Tree.CONST c;
        Tree.TEMP t;

        // Casos especiais
        if (dst.exp instanceof Tree.BINOP) {
            binop = (Tree.BINOP) dst.exp;
            if (binop.left instanceof Tree.CONST
                    && binop.right instanceof Tree.TEMP) {
                c = (Tree.CONST) binop.left;
                t = (Tree.TEMP) binop.right;
                emit(new Assem.OPER("\tsw `s1, " + c.value + "(`s0)\n", null,
                        L(t.temp, L(munchExp(src), null))));
                return;
            }

            if (binop.left instanceof Tree.TEMP
                    && binop.right instanceof Tree.CONST) {
                c = (Tree.CONST) binop.right;
                t = (Tree.TEMP) binop.left;

                emit(new Assem.OPER("\tsw `s1, " + c.value + "(`s0)\n", null,
                        L(t.temp, L(munchExp(src), null))));
                return;
            }

        }

        // Caso geral
        emit(new Assem.OPER("\tsw `s1, 0(`s0)\n", null, L(munchExp(dst.exp),
                L(munchExp(src), null))));
    }

    // Implementa JUMP incondidional
    void munchJump(Tree.Exp s) {
        if (s instanceof Tree.NAME) {
            emit(new Assem.OPER("\tb `j0\n", null, // nao ha' reg. destino
                    null, // nao ha' reg. fonte
                    new Temp.LabelList(((Tree.NAME) s).label, null)));
        } else {
            throw new Error("Erro interno: s nao e' NAME em munchJump.");
        }
    }

    void munchCjump(Tree.CJUMP s) {

        // Geramos codigo para as duas expressoes
        Temp.Temp l = munchExp(s.left);
        Temp.Temp r = munchExp(s.right);

        // Lista dos temporarios que contem os resultados das expressoes
        Temp.TempList temps = L(l, L(r, null));

        // Lista de labels destino
        Temp.LabelList list = new Temp.LabelList(s.iftrue, new Temp.LabelList(
                s.iffalse, null));

        // Verificamos qual e' a instrucao a ser gerada
        switch (s.relop) {
        case Tree.CJUMP.EQ:
            emit(new Assem.OPER("\tbeq `s0, `s1, `j0\n", null, temps, list));
            break;
        case Tree.CJUMP.NE:
            emit(new Assem.OPER("\tbne `s0, `s1, `j0\n", null, temps, list));
            break;
        case Tree.CJUMP.LT:
            emit(new Assem.OPER("\tblt `s0, `s1, `j0\n", null, temps, list));
            break;
        case Tree.CJUMP.GT:
            emit(new Assem.OPER("\tbgt `s0, `s1, `j0\n", null, temps, list));
            break;
        case Tree.CJUMP.LE:
            emit(new Assem.OPER("\tble `s0, `s1, `j0\n", null, temps, list));
            break;
        case Tree.CJUMP.GE:
            emit(new Assem.OPER("\tbge `s0, `s1, `j0\n", null, temps, list));
            break;
        default:
            throw new Error("Erro interno: munchCjump.");
        }
    }

    // Gera "codigo" para um label
    void munchStm(Tree.LABEL s) {
        emit(new Assem.LABEL(s.label.toString() + ":\n", s.label));
    }

    // Gera codigo para uma expressao
    Temp.Temp munchExp(Tree.Exp s) {
        if (s instanceof Tree.CONST)
            return munchExp((Tree.CONST) s);
        if (s instanceof Tree.NAME)
            return munchExp((Tree.NAME) s);
        if (s instanceof Tree.TEMP)
            return munchExp((Tree.TEMP) s);
        if (s instanceof Tree.BINOP)
            return munchExp((Tree.BINOP) s);
        if (s instanceof Tree.MEM)
            return munchExp((Tree.MEM) s);
        if (s instanceof Tree.CALL)
            return munchExp((Tree.CALL) s);
        throw new Error("Erro interno: tipo invalido de no' em munchExp.");
    }

    // Gera codigo para carregar uma constante em um temporario
    Temp.Temp munchExp(Tree.CONST s) {
        Temp.Temp r = new Temp.Temp();

        emit(new Assem.OPER("\tli `d0, " + s.value + "\n", L(r, null), null));

        return r;
    }

    // Gera codigo para carregar um endereco em um temporario
    Temp.Temp munchExp(Tree.NAME s) {
        Temp.Temp r = new Temp.Temp();

        emit(new Assem.OPER("\tla `d0, " + s.label.toString() + "\n",
                L(r, null), null));

        return r;
    }

    // "Gera codigo" para um temporario
    Temp.Temp munchExp(Tree.TEMP s) {
        return s.temp;
    }

    int opera(int op, int a, int b) {
        switch (op) {
        case Tree.BINOP.PLUS:
            return a + b;
        case Tree.BINOP.MINUS:
            return a - b;
        case Tree.BINOP.MUL:
            return a * b;
        case Tree.BINOP.DIV:
            if (b != 0) {
                return a / b;
            } else {
                throw new Error("Divisao por zero no codigo fonte.");
            }
        default:
            throw new Error("Erro interno: munchExp(BINOP).");
        }
    }

    // Gera codigo para operacoes binarias
    Temp.Temp munchExp(Tree.BINOP e) {
        Temp.Temp r = new Temp.Temp();
        int res;

        // Caso especial -- caso houver tempo, tentaremos implementar
        // outros casos especiais para geracao de codigo. Com isso, o
        // codigo gerado pelo compilador pode ser reduzido.

        if (e.left instanceof Tree.CONST) { // Caso o primeiro arg. seja
                                            // constante
            if (e.right instanceof Tree.CONST) {
                res = opera(e.binop, ((Tree.CONST) e.left).value,
                        ((Tree.CONST) e.right).value);

                emit(new Assem.OPER("\tli `d0, " + res + "\n", L(r, null), null));

                return r;
            }

            if (e.binop == Tree.BINOP.PLUS) {
                emit(new Assem.OPER("\taddi `d0, `s0, "
                        + ((Tree.CONST) e.left).value + "\n", L(r, null), L(
                        munchExp(e.right), null)));
                return r;
            }

        }

        if (e.right instanceof Tree.CONST) {
            if (e.binop == Tree.BINOP.PLUS) {
                emit(new Assem.OPER("\taddi `d0, `s0, "
                        + ((Tree.CONST) e.right).value + "\n", L(r, null), L(
                        munchExp(e.left), null)));
                return r;
            }
        }

        // Lista de registradores "fonte"
        Temp.TempList list = L(munchExp(e.left), L(munchExp(e.right), null));

        // FIXME: verificar o codigo
        switch (e.binop) {
        case Tree.BINOP.PLUS:
            emit(new Assem.OPER("\tadd `d0, `s0, `s1\n", L(r, null), list));
            break;
        case Tree.BINOP.MINUS:
            emit(new Assem.OPER("\tsub `d0, `s0, `s1\n", L(r, null), list));
            break;
        case Tree.BINOP.MUL:
            emit(new Assem.OPER("\tmul `d0, `s0, `s1\n", L(r, null), list));
            break;
        case Tree.BINOP.DIV:
            emit(new Assem.OPER("\tdiv `d0, `s0, `s1\n", L(r, null), list));
            break;
        default:
            throw new Error("Erro interno: munchExp(BINOP).");
        }

        return r;
    }

    Temp.Temp munchExp(Tree.MEM e) {
        Temp.Temp r = new Temp.Temp();
        Tree.BINOP binop;
        Tree.CONST c;
        Tree.TEMP t;

        // Casos particulares
        if (e.exp instanceof Tree.BINOP) {
            binop = (Tree.BINOP) e.exp;
            if (binop.binop == Tree.BINOP.PLUS) {

                if (binop.left instanceof Tree.CONST
                        && binop.right instanceof Tree.TEMP) {

                    c = (Tree.CONST) binop.left;
                    t = (Tree.TEMP) binop.right;

                    emit(new Assem.OPER("\tlw `d0, " + c.value + "(`s0)\n", L(
                            r, null), L(t.temp, null)));

                    return r;
                }

                if (binop.right instanceof Tree.CONST
                        && binop.left instanceof Tree.TEMP) {

                    c = (Tree.CONST) binop.right;
                    t = (Tree.TEMP) binop.left;

                    emit(new Assem.OPER("\tlw `d0, " + c.value + "(`s0)\n", L(
                            r, null), L(t.temp, null)));

                    return r;
                }
            }
        }

        emit(new Assem.OPER("\tlw `d0, 0(`s0)\n", L(r, null), L(
                munchExp(e.exp), null)));

        return r;
    }

    // Temp.TempList munchArgs(int i, Tree.ExpList args) {
    // if (args == null) return null;
    // return new Temp.TempList(munchExp(args.head),
    // munchArgs(i+1, args.tail));
    // }

    Temp.TempList munchArgs(int i, Tree.ExpList args) {
        Temp.Temp arg = null;

        if (args == null) { // base da recursao
            return null;
        }

        if (i == 0) {
            arg = MipsFrame.a0;
        } else if (i == 1) {
            arg = MipsFrame.a1;
        } else if (i == 2) {
            arg = MipsFrame.a2;
        } else if (i == 3) {
            arg = MipsFrame.a3;
        } else {
            throw new Error("Erro interno: chamada de funcao com mais de 3 "
                    + "argumentos.");
        }

        if (args.head instanceof Tree.CONST) {
            Tree.CONST c = (Tree.CONST) args.head;

            emit(new Assem.OPER("\tli `d0, " + c.value + "\n", L(arg, null),
                    null));
        } else if (args.head instanceof Tree.NAME) {
            Tree.NAME l = (Tree.NAME) args.head;

            emit(new Assem.OPER("\tla `d0, " + l.label.toString() + "\n", L(
                    arg, null), null));
        } else {
            // Assem.MOVE recebe a string e dois temporarios
            emit(new Assem.MOVE("\tmove `d0, `s0\n", arg, munchExp(args.head)));
        }

        return new Temp.TempList(arg, munchArgs(i + 1, args.tail));
    }

    Temp.Temp munchExp(Tree.CALL s) {
        Temp.TempList l = munchArgs(0, s.args);

        if (s.func instanceof Tree.NAME) {
            String f = ((Tree.NAME) s.func).label.toString();
            emit(new Assem.OPER("\tjal " + f + "\n", calldefs, l));
        }

        return frame.RV();
    }

    // Codigo fornecido pelo livro
    Assem.InstrList codegen(Tree.Stm s) {
        Assem.InstrList l;
        munchStm(s);
        l = ilist;
        ilist = last = null;
        return l;
    }
}
