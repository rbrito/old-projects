# Instructions: 
L1:
	li t33 0
	add t32 $fp t33
	li t35 5
	li t36 2
	add t34 t35 t36
	sw t34 0(t32)
	li t38 0
	add t37 $fp t38
	li t43 0
	add t42 $fp t43
	lw t41 0(t42)
	li t44 3
	add t40 t41 t44
	li t45 4
	add t39 t40 t45
	sw t39 0(t37)
	b L0
L0:
