package name.remal.gradle_plugins.classes_relocation.intern.state;

import static java.util.Comparator.comparing;
import static name.remal.gradle_plugins.classes_relocation.intern.state.ClassMethodInfo.newMethodInfo;
import static name.remal.gradle_plugins.classes_relocation.intern.state.ClassProcessingMode.REGISTER;
import static name.remal.gradle_plugins.classes_relocation.intern.state.ClassProcessingMode.RELOCATE;
import static name.remal.gradle_plugins.classes_relocation.intern.state.ClassProcessingMode.UNSPECIFIED;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import javax.annotation.Nullable;
import lombok.val;

public class RelocationState {

    @GuardedBy("this")
    private final Map<String, RelocationClassState> classStates = new LinkedHashMap<>();

    @GuardedBy("this")
    private final Queue<RelocationTask> tasks = new PriorityQueue<>(comparing(RelocationTask::getInternalClassName));


    @GuardedBy("this")
    private synchronized RelocationClassState getOrCreateClassState(
        String internalClassName,
        ClassProcessingMode mode
    ) {
        RelocationClassState state = classStates.get(internalClassName);
        if (state != null) {
            state.setMode(mode);
            return state;
        }

        state = new RelocationClassState(tasks, internalClassName, mode);
        classStates.put(internalClassName, state);
        return state;
    }


    public synchronized void relocateClass(String internalClassName) {
        getOrCreateClassState(internalClassName, RELOCATE);
    }

    public synchronized void registerClass(String internalClassName) {
        getOrCreateClassState(internalClassName, REGISTER);
    }

    public synchronized void registerParentClass(String internalClassName, @Nullable String parentInternalClassName) {
        if (parentInternalClassName != null) {
            val state = getOrCreateClassState(internalClassName, UNSPECIFIED);
            val parentState = getOrCreateClassState(parentInternalClassName, UNSPECIFIED);
            state.processParent(parentState);
        }
    }

    public void registerParentClasses(String internalClassName, @Nullable String... parentInternalClassNames) {
        if (parentInternalClassNames != null) {
            for (val parentInternalClassName : parentInternalClassNames) {
                registerParentClass(internalClassName, parentInternalClassName);
            }
        }
    }

    public synchronized void registerField(String internalClassName, String fieldName) {
        val state = getOrCreateClassState(internalClassName, UNSPECIFIED);
        state.processField(fieldName);
    }

    public synchronized void registerMethod(
        String internalClassName,
        boolean instanceMethod,
        String methodName,
        String methodDescriptor
    ) {
        val state = getOrCreateClassState(internalClassName, UNSPECIFIED);
        state.processMethod(newMethodInfo(instanceMethod, methodName, methodDescriptor));
    }


    @Nullable
    private RelocationTask currentTask;

    @Nullable
    public synchronized RelocationTask pollRelocationTask() {
        if (currentTask != null) {
            val state = classStates.get(currentTask.getInternalClassName());
            if (state != null) {
                state.clearCurrentTask();
            }
            currentTask = null;
        }

        val task = tasks.poll();
        currentTask = task;
        return task;
    }

}
