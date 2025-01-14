package name.remal.gradle_plugins.classes_relocation.relocator.relocators.clazz;

import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTask;

public interface RelocateClassTask extends QueuedTask {

    String getClassInternalName();


    @Override
    default int getPhase() {
        return RELOCATE_PHASE;
    }

}
