package org.spectral.asm.core.code.instruction

import kotlin.String
import org.objectweb.asm.MethodVisitor
import org.spectral.asm.core.code.Instruction
import org.spectral.asm.core.code.type.ReturnInstruction
import org.spectral.asm.core.common.Opcode

@Opcode(value=172)
class IRETURN : Instruction(172), ReturnInstruction {

  override fun accept(visitor: MethodVisitor) {
    visitor.visitInsn(opcode)
  }

  override fun toString(): String = "IRETURN"
}
