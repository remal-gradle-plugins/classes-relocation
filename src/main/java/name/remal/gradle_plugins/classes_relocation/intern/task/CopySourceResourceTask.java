package name.remal.gradle_plugins.classes_relocation.intern.task;

import static lombok.AccessLevel.PRIVATE;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;

@RequiredArgsConstructor(access = PRIVATE)
@Builder
public class CopySourceResourceTask implements RelocationTask {

    private final Resource sourceResource;

    @Override
    public int getPriority() {
        return PROCESS_SOURCE_PRIORITY;
    }

    @Override
    public void execute(RelocationContext context) {
        sourceResource.writeTo(context.targetEntryFor(sourceResource));
    }

}
