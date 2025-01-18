package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import static lombok.AccessLevel.PRIVATE;

import java.util.Queue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import name.remal.gradle_plugins.classes_relocation.relocator.api.MethodKey;
import name.remal.gradle_plugins.classes_relocation.relocator.task.AbstractQueuedIdentityTask;
import name.remal.gradle_plugins.classes_relocation.relocator.task.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.relocator.utils.UniqueArrayQueue;

@RequiredArgsConstructor
@Getter
@ToString(callSuper = false)
@FieldDefaults(makeFinal = true, level = PRIVATE)
public class RelocateClassCombined
    extends AbstractQueuedIdentityTask
    implements RelocateClassTask, ImmediateTask<RelocateClassResult> {

    String classInternalName;

    Queue<String> fields = new UniqueArrayQueue<>();

    Queue<MethodKey> methods = new UniqueArrayQueue<>();

    public boolean isEmpty() {
        return fields.isEmpty() && methods.isEmpty();
    }

}
