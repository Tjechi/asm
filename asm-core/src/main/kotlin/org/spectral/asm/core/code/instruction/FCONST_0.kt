package org.spectral.asm.core.code.instruction

import kotlin.String
import org.objectweb.asm.MethodVisitor
import org.spectral.asm.core.code.Instruction
import org.spectral.asm.core.common.Opcode
import org.spectral.asm.core.execution.Frame
import org.spectral.asm.core.execution.value.FloatValue

@Opcode(value=11)
class FCONST_0 : Instruction(11) {

  override fun execute(frame: Frame) {
    frame.push(FloatValue(0F))
  }

  override fun accept(visitor: MethodVisitor) {
    visitor.visitInsn(opcode)
  }

  override fun toString(): String = "FCONST_0"
}
