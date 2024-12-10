package name.remal.gradle_plugins.classes_relocation.intern.task.manifest;

import static java.util.Locale.ENGLISH;
import static java.util.jar.Attributes.Name.SIGNATURE_VERSION;

import java.util.jar.Attributes;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.content.ManifestContent;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.RelocationTask;

@RequiredArgsConstructor
class UpdateManifestTask implements RelocationTask {

    private final Resource sourceManifestResource;

    @Override
    public int getPriority() {
        return FINALIZE_PRIORITY;
    }

    @Override
    public void execute(RelocationContext context) {
        sourceManifestResource.getContent()
            .with(ManifestContent.class, manifest -> {
                Stream.concat(
                    Stream.of(manifest.getMainAttributes()),
                    manifest.getEntries().values().stream()
                ).forEach(attrs -> {
                    attrs.keySet().removeIf(keyObject -> {
                        if (SIGNATURE_VERSION.equals(keyObject)) {
                            return true;
                        }

                        val key = keyObject != null ? keyObject.toString().toUpperCase(ENGLISH) : "";
                        return key.isEmpty()
                            || key.endsWith("-DIGEST")
                            || key.endsWith("-DIGEST-MANIFEST");
                    });
                });

                manifest.getEntries().values().removeIf(Attributes::isEmpty);
            })
            .writeTo(context.targetEntryFor(sourceManifestResource));
    }

}
