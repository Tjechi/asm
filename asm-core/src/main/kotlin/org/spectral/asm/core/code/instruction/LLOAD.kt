package org.spectral.asm.core.code.instruction

import kotlin.String
import org.objectweb.asm.MethodVisitor
import org.spectral.asm.core.code.Instruction
import org.spectral.asm.core.code.type.VarInstruction
import org.spectral.asm.core.code.type.InstructionType
import org.spectral.asm.core.common.Opcode

@Opcode(value=22)
class LLOAD(override val index: Int) : Instruction(22), VarInstruction {

  override val type = InstructionType.LONG

  override fun accept(visitor: MethodVisitor) {
    visitor.visitVarInsn(opcode, index)
  }

  override fun toString(): String = "LLOAD"
}