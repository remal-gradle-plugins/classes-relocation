package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static java.util.Collections.singletonList;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskTransformer;

public class RelocateClassTaskTransformer implements QueuedTaskTransformer {

    private final Map<String, RelocateClassCombined> currentRelocateClassCombinedTasks = new LinkedHashMap<>();

    @Override
    public Optional<Collection<? extends QueuedTask>> transform(QueuedTask task, RelocationContext context) {
        if (!(task instanceof RelocateClassTask)) {
            return Optional.empty();
        }

        if (task instanceof RelocateClassCombined) {
            return Optional.empty();
        }

        var relocateClassTask = (RelocateClassTask) task;
        var currentRelocateClassCombinedTask = currentRelocateClassCombinedTasks.computeIfAbsent(
            relocateClassTask.getClassInternalName(),
            internalClassName -> new RelocateClassCombined(internalClassName) {
                @Override
                public void onHandled() {
                    currentRelocateClassCombinedTasks.remove(internalClassName);
                }
            }
        );

        if (relocateClassTask instanceof RelocateField) {
            var relocateField = (RelocateField) relocateClassTask;
            currentRelocateClassCombinedTask.getFields().add(relocateField.getFieldName());
        }

        if (relocateClassTask instanceof RelocateMethod) {
            var relocateMethod = (RelocateMethod) relocateClassTask;
            currentRelocateClassCombinedTask.getMethods().add(relocateMethod.getMethodKey());
        }

        return Optional.of(singletonList(currentRelocateClassCombinedTask));
    }

}
