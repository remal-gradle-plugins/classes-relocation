package name.remal.gradle_plugins.classes_relocation.intern.task;

import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;

public interface RelocationTask {

    int getPriority();

    void execute(RelocationContext context) throws Exception;


    int COLLECT_USAGE_PRIORITY = doNotInline(0);
    int PROCESS_SOURCE_PRIORITY = COLLECT_USAGE_PRIORITY + 100;
    int RELOCATE_PRIORITY = PROCESS_SOURCE_PRIORITY + 100;
    int FINALIZE_PRIORITY = RELOCATE_PRIORITY + 100;

}
