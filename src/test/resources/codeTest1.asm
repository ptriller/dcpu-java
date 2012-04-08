		set A, 0x199
		add [data+A], 0x1
		JSR subroutine
:data 	dat 0x0

		SET PC, 0x0
:subroutine
		SET PC, POP
		