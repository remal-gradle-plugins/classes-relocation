package name.remal.gradle_plugins.classes_relocation.intern.state;

import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static name.remal.gradle_plugins.classes_relocation.intern.state.ClassProcessingMode.RELOCATE;
import static name.remal.gradle_plugins.classes_relocation.intern.state.ClassProcessingMode.UNSPECIFIED;

import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.Getter;

class RelocationClassState {

    private final Queue<RelocationTask> tasks;

    @Getter
    private final String internalClassName;

    @Getter
    private ClassProcessingMode mode;

    public RelocationClassState(Queue<RelocationTask> tasks, String internalClassName, ClassProcessingMode mode) {
        this.tasks = tasks;
        this.internalClassName = internalClassName;
        this.mode = mode;
        forRelocationTask(__ -> { });
    }


    public void setMode(ClassProcessingMode mode) {
        if (this.mode == mode) {
            return;
        }

        if (this.mode != UNSPECIFIED) {
            throw new IllegalStateException(format(
                "%s: %s can't be changed from %s to %s",
                internalClassName,
                ClassProcessingMode.class.getSimpleName(),
                this.mode,
                mode
            ));
        }

        this.mode = mode;

        forRelocationTask(task -> {
            fieldNames.forEach(task::addFieldName);
            methods.forEach(task::addMethod);
        });
    }


    private final Set<RelocationClassState> allParents = new LinkedHashSet<>();
    private final Set<RelocationClassState> allChildren = new LinkedHashSet<>();

    public void processParent(RelocationClassState parent) {
        if (!allParents.add(parent)) {
            return;
        }
        allParents.addAll(parent.allParents);

        parent.allChildren.add(this);
        parent.allParents.forEach(curParent -> curParent.allChildren.add(this));

        methods.forEach(this::processParentAndChildMethods);
    }

    private void processParentAndChildMethods(ClassMethodInfo method) {
        if (method.isInstanceMethod()) {
            allParents.forEach(parent -> parent.processMethodImpl(method));
            allChildren.forEach(parent -> parent.processMethodImpl(method));
        }
    }


    private final Set<String> fieldNames = new LinkedHashSet<>();

    public void processField(String fieldName) {
        if (fieldNames.add(fieldName)) {
            forRelocationTask(task -> task.addFieldName(fieldName));
        }
    }


    private final Set<ClassMethodInfo> methods = new LinkedHashSet<>();

    private void processMethodImpl(ClassMethodInfo method) {
        if (methods.add(method)) {
            forRelocationTask(task -> task.addMethod(method));
        }
    }

    public void processMethod(ClassMethodInfo method) {
        processMethodImpl(method);
        processParentAndChildMethods(method);
    }


    @Nullable
    private RelocationTask currentTask;

    private void forRelocationTask(Consumer<RelocationTask> action) {
        if (mode != RELOCATE) {
            return;
        }

        if (currentTask == null) {
            currentTask = new RelocationTask(internalClassName);
            tasks.add(currentTask);
        }

        action.accept(currentTask);
    }

    public void clearCurrentTask() {
        currentTask = null;
    }


    @Override
    public final boolean equals(@Nullable Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return identityHashCode(this);
    }

}
