package name.remal.gradle_plugins.classes_relocation.intern.postprocessing;

import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;

public interface PostRelocationStep {

    void execute(RelocationContext context);


    default int getPriority() {
        return 0;
    }

}
