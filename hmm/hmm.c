#include <stdlib.h>
#include <stdio.h>
#include "dado.h"
#include <assert.h>

/* Gera $l$ observações de acordo com o modelo $(a, e)$.
   A matriz de transição de estados $a$ possui dimensões $N \times N$.
   A matriz de emissão de símbolos $e$ possui dimensões $N \times M$.

   A função imprime uma seqüência de $l$ observações de acordo com o
   modelo da entrada.

   Supõe-se que o conjunto de estados é $Q = \{0, \ldots, N-1\}$ e
   que o estado inicial é o estado $0$ (que não gera símbolos).
   Supõe-se que o alfabeto seja $\Sigma = \{0, \ldots, M-1 \}$. */
void observa(double **a, double **e, int N, int M, int l)
{
  int t = 0; /* número de caracteres gerados */
  int q = 0; /* estado atual do modelo */

  for (t = 0; t < l; t++) {
    q = dado(a[q], N);
    printf("(%d, %d)\n", q, dado(e[q], M));
  }

}

/* TODO: implementar HMMs como objetos? */

/* Recebe um arquivo já aberto e lê desse arquivo os parâmetros de um modelo
   de Markov de estados ocultos.

   O arquivo deve estar no formato:
   N M
   matriz de transições
   matriz de emissões

   */
void lemodelo(FILE *f, double ***a, double ***e, int *N, int *M)
{
  int i, j;

  /* erros nao sao verificados */
  fscanf(f, "%d %d", N, M);

  *a = malloc(*N * sizeof(double *));
  for (i = 0; i < *N; i++) {
    (*a)[i] = malloc(*N * sizeof(double));
  }
  *e = malloc(*N * sizeof(double *));
  for (i = 0; i < *N; i++) {
    (*e)[i] = malloc(*M * sizeof(double));
  }

  fscanf(f, "\n"); /* desnecessário? */

  /* matrizes alocadas, agora leitura */
  for (i = 0; i < *N; i++) {
    for (j = 0; j < *N; j++) {
      fscanf(f, "%lf", &((*a)[i][j]));
    }
  }

  fscanf(f, "\n"); /* idem */

  for (i = 0; i < *N; i++) {
    for (j = 0; j < *M; j++) {
      fscanf(f, "%lf", &((*e)[i][j]));
    }
  }
}

void imprimemodelo(double **a, double **e, int N, int M)
{
  int i, j;

  for (i = 0; i < N; i++) {
    for (j = 0; j < N; j++) printf("%f ", a[i][j]);
    printf("\n");
  }
  printf("\n");
  for (i = 0; i < N; i++) {
    for (j = 0; j < M; j++) printf("%f ", e[i][j]);
    printf("\n");
  }
  printf("\n");
}

int main(int argc, char *argv[])
{
  int N, M, l;
  FILE *f;
  double **a, **e;

  if (argc != 2) {
    printf("%s: Uso:\n"
	   "\t%s <num>,\n"
	   "onde <num> e o comprimento da observacao a ser gerada.\n",
	   argv[0], argv[0]);
    exit(1);
  }

  f = fopen("hmm.txt", "r");
  lemodelo(f, &a, &e, &N, &M);
  fclose(f);
  imprimemodelo(a, e, N, M);

  l = atoi(argv[1]);

  observa(a, e, N, M, l);

  return 0;
}
