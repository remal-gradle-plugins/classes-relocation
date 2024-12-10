package name.remal.gradle_plugins.classes_relocation.intern.task.license;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.RelocationTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.RelocationTasksFactory;

@RequiredArgsConstructor
public class CopyLicensesTaskFactory implements RelocationTasksFactory {

    @Override
    public Collection<RelocationTask> createRelocationTasks(RelocationContext context) {
        val licenseResources = context.getResources()
            .forConsumption()
            .include("**/LICENSE", "**/LICENSE.*")
            .getRelocationResources()
            .stream()
            .collect(toImmutableList());

        if (licenseResources.isEmpty()) {
            return emptyList();
        }

        return singletonList(new CopyLicensesTask(licenseResources));
    }

}
