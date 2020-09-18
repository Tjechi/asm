package org.spectral.asm.core.code.instruction

import kotlin.String
import org.objectweb.asm.MethodVisitor
import org.spectral.asm.core.code.Instruction
import org.spectral.asm.core.common.Opcode

@Opcode(value=94)
class DUP2_X2 : Instruction(94) {
  override fun accept(visitor: MethodVisitor) {

  }

  override fun toString(): String = "DUP2_X2"
}
