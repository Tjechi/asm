package org.spectral.asm.core.code.instruction

import kotlin.String
import org.objectweb.asm.MethodVisitor
import org.spectral.asm.core.code.Instruction
import org.spectral.asm.core.common.Opcode

@Opcode(value=134)
class I2F : Instruction(134) {
  override fun accept(visitor: MethodVisitor) {

  }

  override fun toString(): String = "I2F"
}
