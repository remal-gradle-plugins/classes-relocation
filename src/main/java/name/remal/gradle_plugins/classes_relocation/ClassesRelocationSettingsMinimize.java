package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getStringProperty;

import java.util.List;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassReachabilityConfig;
import name.remal.gradle_plugins.classes_relocation.relocator.api.ClassReachabilityConfig.ClassReachabilityConfigBuilder;
import org.gradle.api.Action;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

public abstract class ClassesRelocationSettingsMinimize {

    @Input
    @org.gradle.api.tasks.Optional
    public abstract SetProperty<String> getKeepClasses();

    public void keepClasses(Iterable<String> classNamePatterns) {
        getKeepClasses().addAll(classNamePatterns);
    }

    public void keepClasses(String... classNamePatterns) {
        keepClasses(List.of(classNamePatterns));
    }


    @Input
    @org.gradle.api.tasks.Optional
    public abstract SetProperty<String> getKeepMembersAnnotatedWith();

    public void keepMembersAnnotatedWith(Iterable<String> annotationClassNamePatterns) {
        getKeepMembersAnnotatedWith().addAll(annotationClassNamePatterns);
    }

    public void keepMembersAnnotatedWith(String... annotationClassNamePatterns) {
        keepMembersAnnotatedWith(List.of(annotationClassNamePatterns));
    }


    @Internal
    public abstract Property<String> getGraalvmReachabilityMetadataVersion();

    {
        getGraalvmReachabilityMetadataVersion().convention(
            getStringProperty("graalvm-reachability-metadata.version")
        );
    }


    @Input
    @org.gradle.api.tasks.Optional
    public abstract ListProperty<ClassReachabilityConfig> getClassReachabilityConfigs();

    public void addClassReachabilityConfig(Action<? super ClassReachabilityConfigBuilder> action) {
        var builder = ClassReachabilityConfig.builder();
        action.execute(builder);
        getClassReachabilityConfigs().add(builder.build());
    }

}
