package name.remal.gradle_plugins.classes_relocation.relocator.reachability;

import static lombok.AccessLevel.PRIVATE;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.NoArgsConstructor;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassReachabilityConfig;

@NoArgsConstructor(access = PRIVATE)
public abstract class ReachabilityConfigDefaults {

    public static final List<ClassReachabilityConfig> DEFAULT_CLASS_REACHABILITY_CONFIGS = ImmutableList.of(
        ClassReachabilityConfig.builder()
            .classInternalName("org/xmlresolver/loaders/XmlLoader")
            .onReachedClassInternalName("org/xmlresolver/ResolverFeature")
            .allPublicConstructors(true)
            .build()
    );

}
