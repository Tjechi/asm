package org.spectral.asm.core.execution

import org.objectweb.asm.Type
import org.spectral.asm.core.Method
import org.spectral.asm.core.code.Instruction
import org.spectral.asm.core.execution.exception.ExecutionException
import org.spectral.asm.core.execution.exception.StackOverflowException
import org.spectral.asm.core.execution.value.AbstractValue
import org.spectral.asm.core.execution.value.ObjectValue
import org.spectral.asm.core.execution.value.TopValue
import java.util.*

/**
 * Represents a method execution frame per JVM specifications.
 *
 * @property execution The execution instance of this frame.
 * @property method The method this frame is of.
 * @constructor
 */
class Frame(val execution: Execution, val method: Method) {

    /**
     * Whether this frame is currently executing.
     */
    var executing = false

    /**
     * The current instruction which is going to be executed.
     */
    var currentInsn: Instruction? = null

    /**
     * The operand stack of this frame.
     */
    val stack = Stack<AbstractValue>()

    /**
     * The local variable table of this frame.
     */
    val lvt = mutableListOf<AbstractValue>()

    /**
     * The max number of slots for the operand stack
     */
    var maxStack: Int = -1
        private set

    /**
     * The max number of local variables in the LVT.
     */
    var maxLocals: Int = -1
        private set

    /**
     * The state recorder instance for this frame.
     */
    private val recorder = StateRecorder(this)

    /**
     * Initializes the frame with given argument values.
     *
     * @param args List<AbstractValue>
     */
    fun init(args: List<AbstractValue>) {
        if(currentInsn != null) {
            throw ExecutionException("Frame has already been initialized.")
        }

        maxStack = method.code.maxStack
        maxLocals = method.code.maxLocals

        /*
         * Set the initial instruction.
         */
        currentInsn = method.code.instructions.first()

        /*
         * If the method is NOT static. Add the 'this' local variable to
         * the LVT at the zero index.
         */
        if(!method.isStatic) {
            /*
             * Add support for instanced initialization later.
             */
            lvt.add(ObjectValue(null, Type.getObjectType("java/lang/Object")))
        }

        /*
         * Add all the args to the LVT.
         */
        if(args.isNotEmpty()) {
            args.forEach { arg ->
                lvt.add(arg)
            }
        }

        executing = true
    }

    /**
     * Executes a single step or instruction in this frame.
     *
     * @return Boolean
     */
    fun execute() {
        if(executing) {
            /*
             * Execute the current instruction if its not null.
             */
            if(currentInsn == null) {
                executing = false
                return
            }

            /*
             * Start the state recorder.
             */
            recorder.start()

            try {
                currentInsn!!.execute(this)
            } catch (e : ExecutionException) {
                System.err.println("WARN : ${e.message}")
            }

            /*
             * Stop the state recorder.
             */
            recorder.stop()

            /**
             * The the current instruction to the next.
             */
            currentInsn = currentInsn!!.next
        }
    }

    /**
     * Pushes a 32bit [value] to the top of the stack.
     *
     * @param value AbstractValue
     */
    fun push(value: AbstractValue) {
        push(0, value)
    }

    /**
     * Pushes a wide or 64bit [value] to the top of the stack.
     *
     * @param value AbstractValue
     */
    fun pushWide(value: AbstractValue) {
        push(value)
        push(TopValue())
    }

    /**
     * Pushes a value to the stack at index [index].
     *
     * @param index Int
     * @param value AbstractValue
     */
    fun push(index: Int, value: AbstractValue) {
        if(stack.size >= maxStack) {
            throw StackOverflowException("Max Stack Size: $maxStack")
        }

        stack.add(index, value)

        /*
         * Record the push to the state recorder.
         */
        val stackValue = value.stackValue ?: StackValue(value).apply { value.stackValue = this }
        recorder.recordPush(index, stackValue)
    }

    /**
     * Pops a 32bit value from the top of the stack.
     *
     * @return AbstractValue
     */
    fun pop(): AbstractValue {
       return pop(0)
    }

    /**
     * Pops a 64bit value from the top of the stack.
     *
     * @return AbstractValue
     */
    fun popWide(): AbstractValue {
       val top = pop()

        /*
         * Verify this is a top value.
         */
        if(top !is TopValue) {
            throw ExecutionException("Expected 'TopValue' when popping 64bit values. Found: '$top'.")
        }

        return pop()
    }

    /**
     * Pops a value from the stack located at [index]
     *
     * @param index Int
     * @return AbstractValue
     */
    fun pop(index: Int): AbstractValue {
        val value = stack.pop()

        /*
         * Record the state to the state recorder.
         */
        recorder.recordPop(index)

        return value
    }

    override fun toString(): String {
        return "FRAME[$method]"
    }
}