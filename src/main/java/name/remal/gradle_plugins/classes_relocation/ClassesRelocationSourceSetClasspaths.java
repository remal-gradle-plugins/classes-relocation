package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.classes_relocation.SourceSetClasspathsCheckMode.WARN;

import javax.inject.Inject;
import lombok.Getter;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;

@Getter
public abstract class ClassesRelocationSourceSetClasspaths {

    @Internal
    private final NamedDomainObjectSet<SourceSet> skipSourceSets = getObject().namedDomainObjectSet(SourceSet.class);


    @Console
    public abstract Property<SourceSetClasspathsCheckMode> getPartialMatchCheck();

    {
        getPartialMatchCheck().convention(WARN);
    }


    @Console
    public abstract Property<SourceSetClasspathsCheckMode> getAfterEvaluateChangesCheck();

    {
        getAfterEvaluateChangesCheck().convention(WARN);
    }


    @Inject
    protected abstract ObjectFactory getObject();

}
