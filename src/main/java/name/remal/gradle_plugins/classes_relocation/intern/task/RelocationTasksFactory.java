package name.remal.gradle_plugins.classes_relocation.intern.task;

import java.util.Collection;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;

public interface RelocationTasksFactory {

    Collection<RelocationTask> createRelocationTasks(RelocationContext context);

}
