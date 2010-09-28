#include <stdlib.h>

/* Simula um dado de $n$ faces, numeradas de $0$ a $n-1$.
   Entrada: um vetor de $n$ numeros correspondentes as probabilidades
            das faces do dado.
   Saida: um inteiro entre $0$ e $n-1$, representando uma face do
          dado, com probabilidade de $dado(p, n) = i$ igual a $p[i]$. */
int dado(double *p, int n)
{
  double r;
  double cum;
  int i;

  /* casos triviais ou patologicos */
  if (n <= 1) { return 0; }

  r = rand()/((double) RAND_MAX);

  cum = 0; i = 0;
  while (i <= n-1 && r > cum + p[i]) {
    cum = cum + p[i];
    i++;
  }

  if (i == n) { return n-1; }
  else { return i; }
}
