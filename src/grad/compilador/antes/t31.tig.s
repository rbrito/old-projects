# Instructions: 
L1:
	addi t33 $fp 0
	li t34 8
	jal malloc
	move t32 $v0

	# Memoria ja' alocada -- endereco do objeto em t32

	li t35 13
	sw t35 0(t32) # guardando 13 no 1o. campo do record
	
	li t36 17
	sw t36 4(t32) # guardando 17 no 2o. campo do record
	
	sw t32 0(t33) # guardando o end. do record em M[t33]
	
	lw t39 0($fp)
	lw t38 0(t39) # carregando o end. do record para t38

	li t40 4      
	add t37 t38 t40 # adiciona 4 ao end do record (offset de b)

	li t41 19       
	sw t41 0(t37)   # guarda 19 na posicao b
	
	lw t44 0($fp)
	lw t43 0(t44)
	li t45 0        
	add t42 t43 t45 # encontra o offset de a
	
	li t46 23
	sw t46 0(t42) # guarda 23 na posicao a
	b L0
L0:
