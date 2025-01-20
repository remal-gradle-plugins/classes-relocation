package name.remal.gradle_plugins.classes_relocation.relocator.api;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ClassesRelocatorConfig {

    boolean logDynamicReflectionUsage;

    @Default
    MinimizationConfig minimization = MinimizationConfig.builder().build();

}
