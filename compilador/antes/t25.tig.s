# Instructions: 
L4:
	addi t34 $fp 0
	move t33 t34
	li t35 10
	li t36 0
	jal initArray
	move t32 $v0
	sw t32 0(t33)
	li t37 1
	sw t37 -4($fp)
L1:
	lw t38 -4($fp)
	li t39 10
	ble t38 t39 L2
L0:
	b L3
L2:
	lw t42 0($fp)
	lw t41 0(t42)
	lw t44 -4($fp)
	li t45 4
	mul t43 t44 t45
	add t40 t41 t43
	lw t50 0($fp)
	lw t49 0(t50)
	lw t52 -4($fp)
	li t53 4
	mul t51 t52 t53
	add t48 t49 t51
	lw t47 0(t48)
	addi t46 t47 1
	sw t46 0(t40)
	lw t55 -4($fp)
	addi t54 t55 1
	sw t54 -4($fp)
	b L1
L3:
