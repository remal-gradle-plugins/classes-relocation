package name.remal.gradle_plugins.classes_relocation.intern.task;

import static java.lang.String.format;

import name.remal.gradle_plugins.classes_relocation.ClassesRelocationException;
import name.remal.gradle_plugins.classes_relocation.intern.task.immediate.ImmediateTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTask;

public class NotHandledTaskException extends ClassesRelocationException {

    NotHandledTaskException(ImmediateTask<?> task) {
        super(format(
            "Task of type %s can't be handled by any handler: %s",
            task.getClass().getName(),
            task
        ));
    }

    NotHandledTaskException(QueuedTask task) {
        super(format(
            "Task of type %s can't be handled by any handler: %s",
            task.getClass().getName(),
            task
        ));
    }

}