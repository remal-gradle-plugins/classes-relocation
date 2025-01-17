package name.remal.gradle_plugins.classes_relocation.relocator.relocators.meta_inf_classpath_element;

import static java.util.jar.JarFile.MANIFEST_NAME;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource.newGeneratedResource;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.ResourceNameUtils.resourceNameWithRelocationSource;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isEmpty;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.resource.BaseResourcesHandler;

public class MetaInfClasspathElementResourcesHandler extends BaseResourcesHandler {

    public MetaInfClasspathElementResourcesHandler() {
        super(
            List.of(
                MANIFEST_NAME,
                "META-INF/maven/**/pom.xml",
                "META-INF/maven/**/pom.properties"
            ),
            List.of(
            )
        );
    }

    @Override
    protected Optional<Resource> selectImpl(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        List<Resource> candidateResources,
        @Nullable ClasspathElement classpathElement,
        RelocationContext context
    ) {
        if (classpathElement == null) {
            return Optional.empty();
        }

        var classpathElementResource = candidateResources.stream()
            .filter(resource -> classpathElement.equals(resource.getClasspathElement()))
            .findFirst()
            .orElse(null);
        if (classpathElementResource == null) {
            return Optional.empty();
        }

        var relocationSource = context.getRelocationSource(classpathElementResource);
        if (isEmpty(relocationSource)) {
            return Optional.empty();
        }

        return Optional.of(newGeneratedResource(builder -> builder
            .withSourceResource(classpathElementResource)
            .withName(resourceNameWithRelocationSource(originalResourceName, relocationSource))
            .withMultiReleaseVersion(multiReleaseVersion)
        ));
    }

    @Override
    protected Optional<Resource> processResourceImpl(
        String resourceName,
        String originalResourceName,
        @Nullable Integer multiReleaseVersion,
        Resource resource,
        RelocationContext context
    ) {
        return Optional.of(resource);
    }

}
