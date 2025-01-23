package name.remal.gradle_plugins.classes_relocation.relocator.minimization;

import static name.remal.gradle_plugins.classes_relocation.relocator.minimization.MinimizationConfigDefaults.KEEP_ANNOTATION_INCLUSIONS_DEFAULT;

import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassesRelocatorConfigurer;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;

public class MinimizationConfigConfigurer
    implements ClassesRelocatorConfigurer {

    @Override
    public void configure(RelocationContext context) {
        context.getConfig().getMinimization().getKeepAnnotationsFilter().include(KEEP_ANNOTATION_INCLUSIONS_DEFAULT);
    }

}
