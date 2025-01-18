package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import java.util.List;
import java.util.Map;
import name.remal.gradle_plugins.classes_relocation.relocator.api.DelegateRelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;

class RelocateClassCombinedHandlerRelocationContext extends DelegateRelocationContext {

    private final RelocateClassCombined currentTask;
    private final Map<String, List<RelocatedClassData>> relocatedClassDataMap;

    public RelocateClassCombinedHandlerRelocationContext(
        RelocateClassCombined currentTask,
        Map<String, List<RelocatedClassData>> relocatedClassDataMap,
        RelocationContext delegate
    ) {
        super(delegate);
        this.relocatedClassDataMap = relocatedClassDataMap;
        this.currentTask = currentTask;
    }

    @Override
    @SuppressWarnings("java:S3776")
    public void queue(QueuedTask task) {
        if (task instanceof RelocateClass) {
            var typedTask = (RelocateClass) task;
            if (typedTask.getClassInternalName().equals(currentTask.getClassInternalName())) {
                // do nothing
                this.markTaskAsProcessed(typedTask);
                return;
            }
        }

        if (task instanceof RelocateField) {
            var typedTask = (RelocateField) task;
            if (typedTask.getClassInternalName().equals(currentTask.getClassInternalName())) {
                currentTask.getFields().add(typedTask.getFieldName());
                this.markTaskAsProcessed(typedTask);
                return;

            } else {
                var relocatedClassDataList = relocatedClassDataMap.get(typedTask.getClassInternalName());
                if (relocatedClassDataList != null) {
                    var hasProcessedField = relocatedClassDataList.stream()
                        .allMatch(data -> data.hasProcessedField(typedTask.getFieldName()));
                    if (hasProcessedField) {
                        // do nothing
                        this.markTaskAsProcessed(typedTask);
                        return;
                    }
                }
            }
        }

        if (task instanceof RelocateMethod) {
            var typedTask = (RelocateMethod) task;
            if (typedTask.getClassInternalName().equals(currentTask.getClassInternalName())) {
                currentTask.getMethods().add(typedTask.getMethodKey());
                this.markTaskAsProcessed(typedTask);
                return;

            } else {
                var relocatedClassDataList = relocatedClassDataMap.get(typedTask.getClassInternalName());
                if (relocatedClassDataList != null) {
                    var hasProcessedMethod = relocatedClassDataList.stream()
                        .allMatch(data -> data.hasProcessedMethod(typedTask.getMethodKey()));
                    if (hasProcessedMethod) {
                        // do nothing
                        this.markTaskAsProcessed(typedTask);
                        return;
                    }
                }
            }
        }

        super.queue(task);
    }

}
