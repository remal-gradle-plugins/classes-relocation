package name.remal.gradle_plugins.classes_relocation.relocator.api;

import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class MinimizationConfig {

    @Default
    ResourcesFilter keepResourcesFilter = new ResourcesFilter();

    @Default
    ClassFilter keepAnnotationsFilter = new ClassFilter();

    @Singular
    List<ClassReachabilityConfig> classReachabilityConfigs;

}
