package name.remal.gradle_plugins.classes_relocation.intern.task.manifest;

import static java.util.Collections.singletonList;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static name.remal.gradle_plugins.classes_relocation.intern.content.Content.asContent;

import java.util.Collection;
import java.util.jar.Manifest;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.GeneratedResource;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.content.ManifestContent;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.RelocationTask;
import name.remal.gradle_plugins.classes_relocation.intern.task.RelocationTasksFactory;

@RequiredArgsConstructor
public class UpdateManifestTaskFactory implements RelocationTasksFactory {

    @Override
    public Collection<RelocationTask> createRelocationTasks(RelocationContext context) {
        Resource sourceManifestResource = context.getResources()
            .forConsumption()
            .include(MANIFEST_NAME)
            .getSourceResources()
            .stream()
            .findFirst()
            .orElse(null);

        if (sourceManifestResource == null) {
            val manifest = new Manifest();
            manifest.getMainAttributes().putValue(MANIFEST_VERSION.toString(), "1.0");

            sourceManifestResource = new GeneratedResource(
                MANIFEST_NAME,
                asContent(ManifestContent.class, manifest)
            );
        }

        return singletonList(new UpdateManifestTask(sourceManifestResource));
    }

}
