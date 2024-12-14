package name.remal.gradle_plugins.classes_relocation.intern.task;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface BaseWithSupportedTaskClass<TASK> {

    Class<TASK> getSupportedTaskClass();

}
