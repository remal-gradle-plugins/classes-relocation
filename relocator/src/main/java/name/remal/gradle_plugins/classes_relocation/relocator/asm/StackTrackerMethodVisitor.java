package name.remal.gradle_plugins.classes_relocation.relocator.asm;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.relocator.asm.AsmUtils.getLatestAsmApi;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;
import lombok.CustomLog;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@CustomLog
public class StackTrackerMethodVisitor extends MethodVisitor implements Opcodes {

    public static final Object DYNAMIC_STACK_VALUE = new Object() {
        @Override
        public String toString() {
            return "<dynamic stack value>";
        }
    };

    private static final Object NULL_STACK_VALUE = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    private static final Object TWO_SLOTS_VALUE_FILLER = new Object() {
        @Override
        public String toString() {
            return "<two slots value filler>";
        }
    };


    private boolean isBroken = false;

    private final Deque<Object> stack = new ArrayDeque<>();

    public StackTrackerMethodVisitor(@Nullable MethodVisitor methodVisitor) {
        super(getLatestAsmApi(), methodVisitor);
    }

    @Nullable
    @Unmodifiable
    @SuppressWarnings("FuseStreamOperations")
    public List<Object> getStack() {
        if (isBroken) {
            return null;
        }

        return unmodifiableList(
            stack.stream()
                .filter(value -> {
                    if (value == TWO_SLOTS_VALUE_FILLER) {
                        return false;
                    }
                    return true;
                })
                .map(value -> {
                    if (value == NULL_STACK_VALUE) {
                        return null;
                    }
                    return value;
                })
                .collect(toList())
        );
    }

    private void push(@Nullable Object value) {
        if (value == null) {
            value = NULL_STACK_VALUE;
        }

        stack.addLast(value);
    }

    @Nullable
    private Object popOneSlot() {
        if (stack.isEmpty()) {
            isBroken = true;
            logger.info("{}: pop from an empty stack", StackTrackerMethodVisitor.class);
        }

        return stack.pollLast();
    }

    @Nullable
    private Object pop() {
        var removedValue = popOneSlot();

        if (removedValue == TWO_SLOTS_VALUE_FILLER) {
            removedValue = popOneSlot();
        }

        return removedValue;
    }


    @Override
    @SuppressWarnings("java:S1871")
    public void visitInsn(int opcode) {
        Object value1;
        Object value2;
        Object value3;
        Object value4;
        switch (opcode) {
            case ACONST_NULL:
                push(null);
                break;
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
                push(opcode - ICONST_0);
                break;
            case LCONST_0:
            case LCONST_1:
                push((long) (opcode - LCONST_0));
                push(TWO_SLOTS_VALUE_FILLER);
                break;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                push((float) (opcode - FCONST_0));
                break;
            case DCONST_0:
            case DCONST_1:
                push((double) (opcode - DCONST_0));
                push(TWO_SLOTS_VALUE_FILLER);
                break;

            case IALOAD:
            case FALOAD:
            case AALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
                // load from array: arrayref, index -> value
                pop();
                pop();
                push(DYNAMIC_STACK_VALUE);
                break;
            case LALOAD:
            case DALOAD:
                // load from array: arrayref, index -> value
                pop();
                pop();
                push(DYNAMIC_STACK_VALUE);
                push(TWO_SLOTS_VALUE_FILLER);
                break;

            case IASTORE:
            case LASTORE:
            case FASTORE:
            case DASTORE:
            case AASTORE:
            case BASTORE:
            case CASTORE:
            case SASTORE:
                // store to array: arrayref, index, value ->
                pop();
                pop();
                pop();
                break;

            case POP:
                pop();
                break;
            case POP2:
                popOneSlot();
                popOneSlot();
                break;
            case DUP:
                value1 = pop();
                push(value1);
                push(value1);
                break;
            case DUP_X1:
                value1 = pop();
                value2 = pop();
                push(value1);
                push(value2);
                push(value1);
                break;
            case DUP_X2:
                value1 = pop();
                value2 = pop();
                value3 = pop();
                push(value1);
                push(value3);
                push(value2);
                push(value1);
                break;
            case DUP2:
                value1 = popOneSlot();
                value2 = popOneSlot();
                push(value2);
                push(value1);
                push(value2);
                push(value1);
                break;
            case DUP2_X1:
                value1 = popOneSlot();
                value2 = popOneSlot();
                value3 = popOneSlot();
                push(value2);
                push(value1);
                push(value3);
                push(value2);
                push(value1);
                break;
            case DUP2_X2:
                value1 = popOneSlot();
                value2 = popOneSlot();
                value3 = popOneSlot();
                value4 = popOneSlot();
                push(value2);
                push(value1);
                push(value4);
                push(value3);
                push(value2);
                push(value1);
                break;
            case SWAP:
                value1 = pop();
                value2 = pop();
                push(value1);
                push(value2);
                break;

            case IADD:
            case FADD:
            case ISUB:
            case FSUB:
            case IMUL:
            case FMUL:
            case IDIV:
            case FDIV:
            case IREM:
            case FREM:
            case INEG:
            case FNEG:
                pop();
                pop();
                push(DYNAMIC_STACK_VALUE);
                break;
            case LADD:
            case DADD:
            case LSUB:
            case DSUB:
            case LMUL:
            case DMUL:
            case LDIV:
            case DDIV:
                pop();
                pop();
                push(DYNAMIC_STACK_VALUE);
                push(TWO_SLOTS_VALUE_FILLER);
                break;

            case NOP:
                // do nothing
                break;
            default:
                logger.info("{}: Unsupported instruction: {}", StackTrackerMethodVisitor.class, opcode);
                isBroken = true;
        }
        super.visitInsn(opcode);
    }

}
