package org.spectral.asm.simulator

import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.AnalyzerException
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Interpreter
import org.spectral.asm.simulator.controlflow.BlockHandler
import org.spectral.asm.simulator.util.combine
import org.spectral.asm.simulator.util.isPrimitive
import org.spectral.asm.simulator.value.*


/**
 * Responsible for interpreting bytecode of a method and calculating the
 * resulting values pushed to the stack.
 */
class ExecInterpreter : Interpreter<AbstractValue>(ASM8) {

    /**
     * The simulator instance.
     */
    internal lateinit var simulator: MethodSimulator

    /**
     * The control flow block handler instance.
     */
    internal lateinit var blockHandler: BlockHandler

    /**
     * If the value pushed is a supported static invocation, simulate / inline its
     * instructions and return its value result.
     *
     * @param insn AbstractInsnNode
     * @param type Type
     * @return AbstractValue?
     */
    private fun newValueOrVirtualized(insn: AbstractInsnNode, type: Type): AbstractValue? {
        if(SimulatedVirtualValue.supported(type)) {
            return SimulatedVirtualValue.initialize(listOf(insn), type)
        }

        return newValue(insn, type)
    }

    /**
     * Invoked when a new type value is pushed to the stack.
     *
     * @param insn AbstractInsnNode
     * @param type Type
     * @return AbstractValue
     */
    private fun newValue(insn: AbstractInsnNode, type: Type?): AbstractValue? {
        return when {
            type == null -> {
                UninitializedValue.UNINITIALIZED_VALUE
            }
            type == Type.VOID_TYPE -> {
                null
            }
            type.sort <= Type.DOUBLE -> {
                PrimitiveValue(insn, type, null)
            }
            else -> VirtualValue.ofVirtual(insn, type)
        }
    }

    /**
     * Invoked when a new type value is pushed to the stack.
     *
     * @param insns List<AbstractInsnNode>
     * @param type Type?
     * @return AbstractValue?
     */
    private fun newValue(insns: List<AbstractInsnNode>, type: Type?): AbstractValue? {
        return when {
            type == null -> UninitializedValue.UNINITIALIZED_VALUE
            type == Type.VOID_TYPE -> null
            type.sort <= Type.DOUBLE -> PrimitiveValue(insns, type, null)
            else -> VirtualValue.ofVirtual(insns, type)
        }
    }

    override fun newValue(type: Type): AbstractValue {
        throw UnsupportedOperationException("Interpreter called default implementation of 'newValue'.")
    }

    override fun newReturnTypeValue(type: Type?): AbstractValue? {
        return newValue(listOf(), type)
    }

    override fun newEmptyValue(local: Int): AbstractValue {
        return UninitializedValue.UNINITIALIZED_VALUE
    }

    override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type?): AbstractValue? {
        return newValue(listOf(), type)
    }

    override fun newExceptionValue(tryCatchBlockNode: TryCatchBlockNode,
                                   handlerFrame: Frame<AbstractValue>?,
                                   exceptionType: Type?
    ): AbstractValue? {
        return newValue(tryCatchBlockNode.handler, exceptionType)
    }

    override fun newOperation(insn: AbstractInsnNode): AbstractValue?  {
        return when(insn.opcode) {
            ACONST_NULL -> NullConstantValue.newNull(insn)
            ICONST_M1 -> PrimitiveValue.ofInt(insn, -1)
            ICONST_0 -> PrimitiveValue.ofInt(insn, 0)
            ICONST_1 -> PrimitiveValue.ofInt(insn, 1)
            ICONST_2 -> PrimitiveValue.ofInt(insn, 2)
            ICONST_3 -> PrimitiveValue.ofInt(insn, 3)
            ICONST_4 -> PrimitiveValue.ofInt(insn, 4)
            ICONST_5 -> PrimitiveValue.ofInt(insn, 5)
            LCONST_0 -> PrimitiveValue.ofLong(insn, 0L)
            LCONST_1 -> PrimitiveValue.ofLong(insn, 1L)
            FCONST_0 -> PrimitiveValue.ofFloat(insn, 0.0F)
            FCONST_1 -> PrimitiveValue.ofFloat(insn, 1.0F)
            FCONST_2 -> PrimitiveValue.ofFloat(insn, 2.0F)
            DCONST_0 -> PrimitiveValue.ofDouble(insn, 0.0)
            DCONST_1 -> PrimitiveValue.ofDouble(insn, 1.0)
            BIPUSH, SIPUSH -> PrimitiveValue.ofInt(insn, (insn as IntInsnNode).operand)
            LDC -> {
                when(val value = (insn as LdcInsnNode).cst) {
                    is Int -> PrimitiveValue.ofInt(insn, value.toInt())
                    is Long -> PrimitiveValue.ofLong(insn, value.toLong())
                    is Float -> PrimitiveValue.ofFloat(insn, value.toFloat())
                    is Double -> PrimitiveValue.ofDouble(insn, value.toDouble())
                    is String -> SimulatedVirtualValue.ofString(insn, value)
                    is Type -> {
                        val type = value
                        if(type.sort == Type.OBJECT || type.sort == Type.ARRAY) {
                            return VirtualValue.ofClass(insn, type)
                        }
                        else if(type.sort == Type.METHOD) {
                            newValue(insn, Type.getObjectType("java/lang/invoke/MethodType"))!!
                        }
                        else {
                            throw AnalyzerException(insn, "Illegal LDC value $value")
                        }
                    }
                    is Handle -> newValue(insn, Type.getObjectType("java/lang/invoke/MethodHandle"))
                    is ConstantDynamic -> newValue(insn, Type.getType(value.descriptor))
                    else -> throw AnalyzerException(insn, "Illegal LDC value $value")
                }
            }
            JSR -> ReturnAddressValue.newRet(insn)
            GETSTATIC -> newValue(insn, Type.getType((insn as FieldInsnNode).desc))
            NEW -> newValue(insn, Type.getObjectType((insn as TypeInsnNode).desc))
            else -> throw IllegalStateException()
        }
    }

    override fun copyOperation(insn: AbstractInsnNode, value: AbstractValue): AbstractValue? {
        var insnType: Type? = null
        var load = false

        when (insn.opcode) {
            ILOAD -> {
                load = true
                insnType = Type.INT_TYPE
            }
            ISTORE -> insnType = Type.INT_TYPE
            LLOAD -> {
                load = true
                insnType = Type.LONG_TYPE
            }
            LSTORE -> insnType = Type.LONG_TYPE
            FLOAD -> {
                load = true
                insnType = Type.FLOAT_TYPE
            }
            FSTORE -> insnType = Type.FLOAT_TYPE
            DLOAD -> {
                load = true
                insnType = Type.DOUBLE_TYPE
            }
            DSTORE -> insnType = Type.DOUBLE_TYPE
            ALOAD -> {
                load = true
                if (value !== UninitializedValue.UNINITIALIZED_VALUE && !value.isReference) throw AnalyzerException(insn, "Expected a reference type.")
                insnType = value.type
            }
            ASTORE -> {
                if (!value.isReference && value !is ReturnAddressValue) throw AnalyzerException(insn, "Expected a reference or return-address type.")
                insnType = value.type
            }
            else -> {}
        }

        /*
         * Perform a simple verification. We dont want to mix primitive
         * data type with non-primitive data types.
         */
        val argType = value.type
        if(insnType != null) {
            if(insnType.sort == Type.OBJECT && argType.isPrimitive) {
                throw AnalyzerException(insn, "Cannot mix primitive value with type-variable instruction")
            }
            else if(argType.sort == Type.OBJECT && insnType.isPrimitive) {
                throw AnalyzerException(insn, "Cannot mix type value with primitive-variable instruction")
            }
        }

        if(load && insnType != value.type) {
            return newValue(combine(value.insns, insn), insnType)
        }

        return value.copy(insn)
    }

    override fun unaryOperation(insn: AbstractInsnNode?, value: AbstractValue?): AbstractValue {
        TODO("Not yet implemented")
    }

    override fun binaryOperation(insn: AbstractInsnNode?, value1: AbstractValue?, value2: AbstractValue?): AbstractValue {
        TODO("Not yet implemented")
    }

    override fun ternaryOperation(insn: AbstractInsnNode?, value1: AbstractValue?, value2: AbstractValue?, value3: AbstractValue?): AbstractValue {
        TODO("Not yet implemented")
    }

    override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out AbstractValue>?): AbstractValue {
        TODO("Not yet implemented")
    }

    override fun returnOperation(insn: AbstractInsnNode?, value: AbstractValue?, expected: AbstractValue?) {
        TODO("Not yet implemented")
    }

    override fun merge(value1: AbstractValue, value2: AbstractValue): AbstractValue {
        if(value2 == UninitializedValue.UNINITIALIZED_VALUE) {
            return value1
        }

        if(value1 == value2) {
            return value1
        }

        return UninitializedValue.UNINITIALIZED_VALUE
    }
}