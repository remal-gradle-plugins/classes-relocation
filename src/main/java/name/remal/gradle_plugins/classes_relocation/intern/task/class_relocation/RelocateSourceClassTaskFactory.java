package name.remal.gradle_plugins.classes_relocation.intern.task.class_relocation;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.RelocationTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.RelocationTasksFactory;

public class RelocateSourceClassTaskFactory implements RelocationTasksFactory {

    @Override
    public Collection<RelocationTask> createRelocationTasks(RelocationContext context) {
        return context.getResources()
            .forConsumption()
            .include("**/*.class")
            .exclude("**/module-info.class")
            .getSourceResources()
            .stream()
            .map(RelocateSourceClassTask::new)
            .collect(toList());
    }

}
