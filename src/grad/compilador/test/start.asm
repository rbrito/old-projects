/* Start.s 
 *	Assembly language assist for user programs running on top of spim.
 *
 *	Since we don't want to pull in the entire C library, we define
 *	what we need for a user program here, namely Start and the system
 *	calls.
 */

#define IN_ASM
#include "spim-syscall.h"

	.text   
	.align  2

/* -------------------------------------------------------------
 * __start
 *	Initialize running a C program, by calling "main". 
 * -------------------------------------------------------------
 */

	.globl __start
	.ent	__start
__start:
	li      $gp,0x10008000
	jal	main
	jal	Exit	 /* if we return from main, exit(0) */
	.end __start

/* -------------------------------------------------------------
 * System call stubs:
 *	Assembly language assist to make system calls to the Spim kernel.
 *	There is one stub per system call, that places the code for the
 *	system call into register v0, and leaves the arguments to the
 *	system call alone (in other words, arg1 is in $a0, arg2 is 
 *	in $a1, arg3 is in $a2, arg4 is in $a3)
 *
 * 	The return value is in $v0. This follows the standard C calling
 * 	convention on the MIPS.
 * -------------------------------------------------------------
 */

	.globl  PrintInt
	.ent    PrintInt
PrintInt:
	li      $2,PRINT_INT_SYSCALL
	syscall
	j	$31
	.end    PrintInt
	


	.globl  PrintFloat
	.ent    PrintFloat
PrintFloat:
	li      $2,PRINT_FLOAT_SYSCALL
	syscall
	j	$31
	.end    PrintFloat
	


	.globl  PrintDouble
	.ent    PrintDouble
PrintDouble:
	li      $2,PRINT_DOUBLE_SYSCALL
	syscall
	j	$31
	.end    PrintDouble
	
	


	.globl  PrintString
	.ent    PrintString
PrintString:
	li      $2,PRINT_STRING_SYSCALL
	syscall
	j	$31
	.end    PrintString
	


	.globl  ReadInt
	.ent    ReadInt
ReadInt:
	li      $2,READ_INT_SYSCALL
	syscall
	j	$31
	.end    ReadInt
	


	.globl  ReadFloat
	.ent    ReadFloat
ReadFloat:
	li      $2,READ_FLOAT_SYSCALL
	syscall
	j	$31
	.end    ReadFloat
	


	.globl  ReadDouble
	.ent    ReadDouble
ReadDouble:
	li      $2,READ_DOUBLE_SYSCALL
	syscall
	j	$31
	.end    ReadDouble
	


	.globl  ReadString
	.ent    ReadString
ReadString:
	li      $2,READ_STRING_SYSCALL
	syscall
	j	$31
	.end    ReadString



	.globl  Sbrk
	.ent    Sbrk
Sbrk:
	li      $2,SBRK_SYSCALL
	syscall
	j	$31
	.end    Sbrk
	


	.globl  Exit
	.ent    Exit
Exit:
	li      $2,EXIT_SYSCALL
	syscall
	j	$31
	.end    Exit


	
/* dummy function to keep gcc happy */
        .globl  __main
        .ent    __main
__main:
        j       $31
        .end    __main

