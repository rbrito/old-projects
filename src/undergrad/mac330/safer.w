@i portugues.w

@* SAFER. Esta é a primeira fase do projeto de implementação do algoritmo
de criptografia chamado SAFER K-64 (Secure And Fast Encryption Routine),
desenvolvido por J.~Massey para a empresa Cylink Corporation, que utiliza
chaves de $64$ bits para criptografia.

O algoritmo baseia-se na repetição de uma determinada seqüência de operações
chamada {\it round} para efetuar a criptografia da entrada. Para uma boa
margem de segurança, recomenda-se que sejam executados pelo menos $6$ rounds
para criptografar um dado arquivo. Cada round do algoritmo é uma seqüência de
$6$ operações, cuja descrição precisa veremos adiante.

O algoritmo SAFER será programado tendo-se em mente arquiteturas com bytes de
$8$ bits. Em particular, como as chaves tem tamanho $64$ bits, elas ocupam
tamanho de $8$ bytes. A seguir, temos a estrutura de nosso programa:

@c

@<Definições do Pré-Processador@>@;
@<Funções Auxiliares@>@;
int main(int argc, char *argv[])
{
  @<Variáveis@>@;
  @<Inicialização@>@;
  @<Algoritmo SAFER@>@;
  @<Finalização@>@;
  return 0;
}

@ Cada round de nosso programa consiste de uma seqüência de $6$
operações básicas, sendo que duas chaves são utilizadas por cada round
(mais especificamente, as chaves serão necessárias no primeiro e terceiro
 passos do round).

O algoritmo SAFER opera sobre blocos de $8$ bytes, isto é, a entrada é
dividida em blocos de $8$ bytes. Uma macro do compilador chamada |BLKSIZE|
``conterá'' este valor.  Nossa decisão neste programa para criptografar
arquivos cujos tamanhos não sejam múltiplos de $8$ bytes é a de simplesmente
adicionar espaços ao fim do arquivo até que seu tamanho seja um múltiplo de
$8$.  Abaixo apresentamos a rotina de leitura da entrada. A função |leitura|
recebe dois parâmetros, |fp| e |v|. O parâmetro |fp| é um ponteiro para uma
estrutura de arquivo de entrada, enquanto o parâmetro |v| é um ponteiro para
uma string de caracteres. A função devolve como saída o número de bytes lidos
da entrada e os bytes lidos na string apontada por |v| (caso a entrada acabe
antes de |BLKSIZE| bytes serem lidos, |v| será preenchida com os espaços em
branco).

@<Fun...@>=

int leitura(FILE *fp, unsigned char *v)
{
  register int i = 0, j, c;

  while (((c = getc(fp)) != EOF) && (i < BLKSIZE)) {
    v[i] = c;
    i++;
  }

  if (c != EOF) /* um caracter a mais do que devia foi pego */
    ungetc(c, fp);

  for (j = i; j < BLKSIZE; j++)
    v[j] = ' '; /* preenchendo com brancos */

  return i;
}

@ Função de escrita.
@<Fun...@>=
int escrita(FILE *fp, unsigned char *v, int n)
{
  register int i = 0, erro = 0;

  while ((i < n) && (!erro)) {
    erro = (putc(v[i], fp) == EOF);
    i++;
  }

  return !erro;
}

@ Antes de que nós nos esqueçamos, daremos algumas declarações do
pré-processador, definindo duas macros, |BLKSIZE| e |KEYSIZE|.
@<Def...@>=
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#define BLKSIZE 8
#define KEYSIZE BLKSIZE

@ A sintaxe do programa é a seguinte: \.{safer <entrada> <saida> <chave> [rounds]},
onde:
\item{$\bullet$} \.{entrada} é o nome do arquivo de entrada;
\item{$\bullet$} \.{saida} é o nome do arquivo de saída;
\item{$\bullet$} \.{chave} é uma chave alfanumérica de comprimento exatamente
$8$ bytes;
\item{$\bullet$} \.{rounds} é um parâmetro opcional, numérico, indicando quantos
rounds do algoritmo serão executados por bloco de $8$ bytes do arquivo de entrada.

Para decodificação, a sintaxe é: \.{saferdec <entrada> <saida> <chave> [rounds]}, onde:
\item{$\bullet$} \.{entrada} é o nome do arquivo a já codificado;
\item{$\bullet$} \.{saida} é o nome do arquivo resultante da decodificação;
\item{$\bullet$} \.{chave} é a mesma chave especificada no momento da codificação do
arquivo \.{entrada}.
\item{$\bullet$} \.{rounds} é um parâmetro opcional, numérico, indicando quantos
rounds do algoritmo serão executados por bloco de $8$ bytes do arquivo de entrada.

@ A inicialização do programa consiste em tratamento de parâmetros da linha de
comando e inicialização de algumas variáveis.
@<Ini...@>=
@<Tratamento de Linha de Comando@>@;
@<Pré-cálculo de Variáveis@>@;

@ Aqui segue o tratamento dos parâmetros de entrada que o programa deve
receber (nome de arquivos e chave) da linha de comando.
@<Tra...@>=
if ((argc != 4) && (argc != 5)) {
  fprintf(stderr,
	"Numero invalido de parametros.\nSintaxe: %s <entrada> <saida> <chave> [rounds]\n",
	argv[0]);
  exit(1);
}
else { /* o número de parâmetros está correto */
  if (strstr(argv[0], "saferdec") != 0) { /* devemos decodificar */
    codifica = 0;
    printf("%s: decodificando arquivo de entrada...\n", argv[0]);
  }
  else printf("%s: codificando arquivo de entrada...\n", argv[0]);

  if (!(entrada = fopen(argv[1], "rb"))) {
    fprintf(stderr, "%s: erro abrindo arquivo de entrada\n", argv[0]);
    exit(1);
  }
  
  if (!(saida = fopen(argv[2], "wb"))) {
    fprintf(stderr, "%s: erro abrindo arquivo de saida\n", argv[0]);
    exit(1);
  }
  
  if (strlen(argv[3]) != KEYSIZE) {
    fprintf(stderr, "%s: chave deve ter exatamente %d bytes.\n", argv[0], KEYSIZE);
    exit(1);
  }

  if (argc == 5) { /* foi especificado um número de rounds para o algoritmo */
    if (sscanf(argv[4], "%d", &rounds) != 1) { /* problemas lendo parâmetro */
      fprintf(stderr,
              "%s: erro lendo rounds da linha de comando. Usando %d rounds...\n", argv[0], ROUNDS);
    }
    else if (rounds <= 5) {
      fprintf(stderr, "%s: aviso -- poucos rounds especificados\n", argv[0]);
    }
  }
}

@ Tendo-se em vista a constante |ROUNDS| usada na passagem anterior, devemos
declará-la ao pré-processador.
@<Def...@>=
#define ROUNDS 6 /* valor mínimo recomendado, por razões de segurança */

@ Declararemos também as variáveis |rounds| e |codifica|. A variável |rounds|
será inicializada com o valor default |ROUNDS|, para o caso de o usuário não
declarar nenhum número de rounds na linha de comando. A variável |codifica|
serve apenas para indicar se devemos criptografar ou descriptografar o arquivo
dado. Ela é inicializada com o valor default $1$, significando que o programa
irá codificar o arquivo da entrada.
@<Var...@>=
int rounds = ROUNDS, codifica = 1;

@ O próximo passo com que devemos nos preocupar é com a inicialização das variáveis
do programa e com as funções pré-calculadas.
Como $257$ é um número primo, o grupo multiplicativo do corpo $GF(257)$
possui $256$ elementos, a saber $\{1, \ldots, 256\}.$ O elemento $45$ é uma
raiz primitiva de $GF(257),$ isto é, $45^i$ assume todos os valores de $\{1,
\ldots, 256\},$ quando $i$ varia de $0$ a $255$ (o grupo multiplicativo é
cíclico). Como estamos assumindo que os bytes com que trabalhamos possuem $8$
bits, eles podem assumir $256$ valores, que são de $0$ a $255$. Essa discussão
nos mostra que a função $\exp: \{0, \ldots, 255\} \longrightarrow \{1, \ldots,
256\}$ dada por $\exp(i) = 45^i$ é uma bijeção (naturalmente, $\exp$ possui
uma inversa que é $\log: \{1, \ldots, 256\} \longrightarrow \{0, \ldots,
255\}$ e tal que $\log(\exp(i)) = i.$). Como essas funções têm cômputo
relativamente caro e precisaremos chamá-las com grande freqüência durante todo
o algoritmo, armazenaremos seus valores em dois vetores, |exp[]| e |log[]|.
@<Pré...@>=
exp[0] = 1; log[exp[0]] = 0;
for (i = 1; i < 256; i++) {
  if (exp[i-1])
    exp[i] = (45 * exp[i-1]) % 257;
  else
    exp[i] = (45 * 256) % 257;

  log[exp[i]] = i;
}

@ Uma das tarefas auxiliares que precisamos implementar para o funcionamento
do código é a rotação de bits à esquerda de uma palavra de tamanho |BLKSIZE|
bytes. A Linguagem C fornece uma operação de shift de bits à esquerda de
palavras (com tamanho dependente da implementação). Nossa função de rotação é
geral o suficiente para funcionar com palavras de qualquer tamanho. A função
|rotação| devolve em |t| o resultado da rotação dos bytes de |s|. O byte mais
significativo é o de posição $0$. O número de bits que sofrerá rotação
(especificado na macro |ROTACAO| deve ser menor ou igual a 7.
@<Fun...@>=
void rotacao(unsigned char *s, unsigned char *t)
{
    unsigned char oldbits = 0, bits = 0;
    register int i;

    for (i = BLKSIZE - 1; i >= 0; i--) {
       bits = s[i] & ((0xff >> (8 - ROTACAO)) << (8 - ROTACAO));
       t[i] = s[i] << ROTACAO;
       t[i] |= oldbits;
       oldbits = bits >> (8 - ROTACAO);
    }

    t[BLKSIZE - 1] = t[BLKSIZE - 1] | oldbits;
}

@
@<Def...@>=
#define ROTACAO 3

@ O algoritmo SAFER necessita, para cada bloco de |BLKSIZE| bytes da entrada,
de duas chaves para cada round e de mais uma chave para uma operação final nos
blocos.  Logo, |2*rounds+1| chaves são necessárias por bloco (as chaves são as
mesmas para todos os blocos, sendo que a primeira chave é a fornecida pelo
usuário, na hora da codificação/decodificação.
Guardaremos as chaves em um vetor. Como ignoraremos sua primeira posição,
(para não usarmos a posição de índice $0$) e o número de rounds é variável (já
que pode ser especificado pelo usuário), o vetor de chaves será alocado com
|2*rounds+2| posições.
@<Pré...@>=
  if (!(k = malloc((2*rounds+2) * sizeof(char *)))) { /* erro de alocação de memória */
    fprintf(stderr, "%s: erro de alocacao de memoria para as chaves. Abortando...\n", argv[0]);
    exit(1);
  }

  for (i = 0; i < 2*rounds+2; i++) { /* é possível pular a primeira posição
  (não será utilizada), mas seremos conservadores\dots */
    if (!(k[i] = malloc(8 * sizeof(char)))) { /* erro de alocacao de memória */
      fprintf(stderr, "%s: erro de alocacao de memoria para as chaves. Abortando...\n", argv[0]);
      exit(1);
    }
  }

  for (i = 0; i < 8; i++) /* copiando a chave fornecida pelo usuário na primeira posição */
      k[1][i] = argv[3][i];

  for (i = 2; i < 2*rounds+2; i++)  /* rotação de |3| bits de |k[i-1]| em |k[i]| */
      rotacao(k[i-1], k[i]);

  for (i = 2; i < 2*rounds+2; i++)
      for (j = 0; j < KEYSIZE; j++)
	k[i][j] += exp[exp[(9*i + j + 1) % 256]]; /* o |+1| é por conta de |j| variar de $1$ a $8$ */


@ Agora definiremos algumas variáveis que foram usadas nas seções anteriores.
@<Var...@>=
FILE *entrada, *saida; /* arquivos de entrada e saída */
unsigned char exp[256], log[256]; /* vetores para pré-cálculo de |exp| e |log| */
register int i, j; /* contadores de uso geral */
unsigned char **k; /* o ``vetor'' das chaves */

@* O Algoritmo. Como diversos outros algoritmos de criptografia, o SAFER é um
algoritmo que se baseia em repetição de um bloco de passos, cada um
representando uma função inversível. Aqui está o esqueleto do algoritmo:

@<Alg...@>=

if (codifica) {
  fprintf(saida, "              \n"); /* espaco para o cabeçalho na saída */
  
  for (i = 0; i < BLKSIZE; i++) CBC[i] = 0; /* início do modo CBC */
  
  lidos = 0;
  
  while((i = leitura(entrada, v))) {
    
    lidos += i; /* |lidos| contém o número de caracteres lidos até o momento */

    for (j = 0; j < BLKSIZE; j++) v[j] ^= CBC[j]; /* Modo CBC */
    
    for (j = 1; j <= rounds; j++) {
      @<Primeiro Passo@>@;
      @<Segundo Passo@>@;
      @<Terceiro Passo@>@;
      @<Quarto Passo@>@;
      @<Quinto Passo@>@;
      @<Sexto Passo@>@;
      for (l = 0; l < BLKSIZE; l++) v[l] = aux[l]; /* preparando para próxima iteração */
    }
    
    @<Operação T@>@; /* operação final, realizada a cada bloco */
    
    j = escrita(saida, v, BLKSIZE); /* verificar resultado da operação */

    for (j = 0; j < BLKSIZE; j++) /* prepando para próxima iteração */
      CBC[j] = v[j];
  }
  
  rewind(saida);
  fprintf(saida, "%d", lidos);
} /* codificação */
else {
  char buffer[BUFSIZE];
  unsigned char CBCOLD[BLKSIZE];

  for (i = 0; i < BLKSIZE; i++) CBCOLD[i] = 0; /* para o início do modo CBC */

  fgets(buffer, BUFSIZE, entrada);
  sscanf(buffer, "%d", &lidos);

  while ((lidos > 0) && (leitura(entrada, v))){
  /* |lidos| aqui indica quantos caracteres ainda devem ser lidos */

    for (i = 0; i < BLKSIZE; i++) CBC[i] = v[i]; /* CBC do próximo passo */

    @<Inversa da Operação T@>@;

    for (j = rounds; j >= 1; j--) {
      @<Inversa do Sexto Passo@>@;
      @<Inversa do Quinto Passo@>@;
      @<Inversa do Quarto Passo@>@;
      @<Inversa do Terceiro Passo@>@;
      @<Inversa do Segundo Passo@>@;
      @<Inversa do Primeiro Passo@>@;
    }

    /* des-CBCeza */

    for (i = 0; i < BLKSIZE; i++)
	v[i] ^= CBCOLD[i];

    if (lidos >= BLKSIZE) escrita(saida, v, BLKSIZE);
    else escrita(saida, v, lidos);

    lidos -= BLKSIZE; /* quantos caracteres faltam para serem escritos */

    for (i = 0; i < BLKSIZE; i++) CBCOLD[i] = CBC[i]; /* ``update'' do modo CBC */
  }

} /* decodificação */

@ Variáveis.
@<Var...@>=
int lidos, l;
unsigned char v[BLKSIZE], CBC[BLKSIZE], aux[BLKSIZE];

@ Tamanho da variável |buffer|.
@<Def...@>=
#define BUFSIZE 128

@ O primeiro passo do algoritmo consiste em operações individuais entre os
bytes do vetor lido (após ser aplicada a operação CBC) e os bytes da primeira
chave a ser usada no round atual. A descrição detalhada do algoritmo está
aqui:
@<Prim...@>=
/* o vetor |k[2*j-1]| contém a chave usada no momento */
v[0] = v[0] ^ k[2*j-1][0];
v[1] = v[1] + k[2*j-1][1];
v[2] = v[2] + k[2*j-1][2];
v[3] = v[3] ^ k[2*j-1][3];
v[4] = v[4] ^ k[2*j-1][4];
v[5] = v[5] + k[2*j-1][5];
v[6] = v[6] + k[2*j-1][6];
v[7] = v[7] ^ k[2*j-1][7];

@ O segundo passo do algoritmo é uma aplicação de exponenciais e logaritmos
aos bytes resultantes da primeira fase.
@<Seg...@>=
/* série de exponenciações e logaritmos */
v[0] = exp[v[0]];
v[1] = log[v[1]];
v[2] = log[v[2]];
v[3] = exp[v[3]];
v[4] = exp[v[4]];
v[5] = log[v[5]];
v[6] = log[v[6]];
v[7] = exp[v[7]];

@ O terceiro passo é análogo ao primeiro, com exceção de que as operações de
soma são trocadas por operações |^| e vice-versa e a chave usada neste momento
é a chave seguinte à chave usada no primeiro passo.
@<Ter...@>=
/* o vetor |k[2*j]| contém a chave usada no momento */
v[0] = v[0] + k[2*j][0];
v[1] = v[1] ^ k[2*j][1];
v[2] = v[2] ^ k[2*j][2];
v[3] = v[3] + k[2*j][3];
v[4] = v[4] + k[2*j][4];
v[5] = v[5] ^ k[2*j][5];
v[6] = v[6] ^ k[2*j][6];
v[7] = v[7] + k[2*j][7];

@ A quarta fase é resultado de um sistema linear (inversível) módulo $256$.
@<Qua...@>=
aux[0] = (2*v[0] + v[1]) % 256;
aux[1] = (2*v[2] + v[3]) % 256;
aux[2] = (2*v[4] + v[5]) % 256;
aux[3] = (2*v[6] + v[7]) % 256;
aux[4] = (v[0] + v[1]) % 256;
aux[5] = (v[2] + v[3]) % 256;
aux[6] = (v[4] + v[5]) % 256;
aux[7] = (v[6] + v[7]) % 256;

@ A quinta fase é exatamente igual ao quarto passo.
@<Qui...@>=
v[0] = (2*aux[0] + aux[1]) % 256;
v[1] = (2*aux[2] + aux[3]) % 256;
v[2] = (2*aux[4] + aux[5]) % 256;
v[3] = (2*aux[6] + aux[7]) % 256;
v[4] = (aux[0] + aux[1]) % 256;
v[5] = (aux[2] + aux[3]) % 256;
v[6] = (aux[4] + aux[5]) % 256;
v[7] = (aux[6] + aux[7]) % 256;

@ O sistema aplicado aqui é o mesmo que o anterior, mas com a diferença de
que os resultados não são ``trançados''.
@<Sex...@>=
aux[0] = (2*v[0] + v[1]) % 256;
aux[1] = (v[0] + v[1]) % 256;
aux[2] = (2*v[2] + v[3]) % 256;
aux[3] = (v[2] + v[3]) % 256;
aux[4] = (2*v[4] + v[5]) % 256;
aux[5] = (v[4] + v[5]) % 256;
aux[6] = (2*v[6] + v[7]) % 256;
aux[7] = (v[6] + v[7]) % 256;

@ A operação final $T$ é exatamente como a primeira operação, exceto que a
chave utilizada aqui deve ser a última chave gerada (isto é, se temos |rounds|
rounds, a última chave utilizada até o momento foi a chave de número
|2*rounds| e, portanto, para a última operação, usaremos a chave de número
|2*rounds + 1|).
@<Ope...@>=
v[0] = v[0] ^ k[2*rounds+1][0];
v[1] = v[1] + k[2*rounds+1][1];
v[2] = v[2] + k[2*rounds+1][2];
v[3] = v[3] ^ k[2*rounds+1][3];
v[4] = v[4] ^ k[2*rounds+1][4];
v[5] = v[5] + k[2*rounds+1][5];
v[6] = v[6] + k[2*rounds+1][6];
v[7] = v[7] ^ k[2*rounds+1][7];

@
@<Inversa da Operação T@>=
v[0] = v[0] ^ k[2*rounds+1][0];
v[1] = v[1] - k[2*rounds+1][1];
v[2] = v[2] - k[2*rounds+1][2];
v[3] = v[3] ^ k[2*rounds+1][3];
v[4] = v[4] ^ k[2*rounds+1][4];
v[5] = v[5] - k[2*rounds+1][5];
v[6] = v[6] - k[2*rounds+1][6];
v[7] = v[7] ^ k[2*rounds+1][7];

@
@<Inversa do Sex...@>=
aux[0] = v[0] - v[1];
aux[1] = 2*v[1] - v[0];
aux[2] = v[2] - v[3];
aux[3] = 2*v[3] - v[2];
aux[4] = v[4] - v[5]; 
aux[5] = 2*v[5] - v[4];
aux[6] = v[6] - v[7];
aux[7] = 2*v[7] - v[6];

@
@<Inversa do Qui...@>=
v[6] = aux[3] - aux[7];
v[7] = 2*aux[7] - aux[3];
v[4] = aux[2] - aux[6];
v[5] = 2*aux[6] - aux[2];
v[2] = aux[1] - aux[5];
v[3] = 2*aux[5] - aux[1];
v[0] = aux[0] - aux[4];
v[1] = 2*aux[4] - aux[0];

@
@<Inversa do Qua...@>=
aux[6] = v[3] - v[7];
aux[7] = 2*v[7] - v[3];
aux[4] = v[2] - v[6];
aux[5] = 2*v[6] - v[2];
aux[2] = v[1] - v[5];
aux[3] = 2*v[5] - v[1];
aux[0] = v[0] - v[4];
aux[1] = 2*v[4] - v[0];

@
@<Inversa do Ter...@>=
v[0] = aux[0] - k[2*j][0];
v[1] = aux[1] ^ k[2*j][1];
v[2] = aux[2] ^ k[2*j][2];
v[3] = aux[3] - k[2*j][3];
v[4] = aux[4] - k[2*j][4];
v[5] = aux[5] ^ k[2*j][5];
v[6] = aux[6] ^ k[2*j][6];
v[7] = aux[7] - k[2*j][7];

@
@<Inversa do Seg...@>=
v[0] = log[v[0]];
v[1] = exp[v[1]];
v[2] = exp[v[2]];
v[3] = log[v[3]];
v[4] = log[v[4]];
v[5] = exp[v[5]];
v[6] = exp[v[6]];
v[7] = log[v[7]];

@
@<Inversa do Pri...@>=
v[0] = v[0] ^ k[2*j-1][0];
v[1] = v[1] - k[2*j-1][1];
v[2] = v[2] - k[2*j-1][2];
v[3] = v[3] ^ k[2*j-1][3];
v[4] = v[4] ^ k[2*j-1][4];
v[5] = v[5] - k[2*j-1][5];
v[6] = v[6] - k[2*j-1][6];
v[7] = v[7] ^ k[2*j-1][7];

@
@<Fin...@>=
fclose(entrada);
fclose(saida);

@* Indice.



