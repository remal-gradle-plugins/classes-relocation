package name.remal.gradle_plugins.classes_relocation.relocator.relocators.manifest;

import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static java.util.jar.Attributes.Name.SIGNATURE_VERSION;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource.newGeneratedResource;
import static name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.classes_relocation.relocator.utils.MultiReleaseUtils.MULTI_RELEASE;

import java.io.ByteArrayOutputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.classes_relocation.relocator.api.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.GeneratedResource;
import name.remal.gradle_plugins.classes_relocation.relocator.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.relocator.task.QueuedTaskHandlerResult;

public class CreateManifestHandler implements QueuedTaskHandler<CreateManifest> {

    @Override
    @SneakyThrows
    public QueuedTaskHandlerResult handle(CreateManifest task, RelocationContext context) {
        var manifest = new Manifest();
        var mainAttrs = manifest.getMainAttributes();

        Resource manifestResource = context.getSourceClasspath().getResources(MANIFEST_NAME)
            .stream().findFirst().orElse(null);
        if (manifestResource != null) {
            try (var in = manifestResource.open()) {
                manifest.read(in);
            }

            var manifestVersion = mainAttrs.getValue(MANIFEST_VERSION);
            if (!"1.0".equals(manifestVersion)) {
                throw new IllegalStateException(format(
                    "%s: manifest version version `%s`, that's not supported by the plugin",
                    manifestResource,
                    manifestVersion
                ));
            }
        } else {
            manifestResource = newGeneratedResource(builder -> builder
                .withName(MANIFEST_NAME)
                .withoutMultiReleaseVersion()
                .withEmptyContent()
            );
        }

        var allEntryAttrs = manifest.getEntries().values();

        Stream.concat(
            Stream.of(mainAttrs),
            allEntryAttrs.stream()
        ).forEach(attrs -> {
            attrs.keySet().removeIf(keyObject -> {
                if (SIGNATURE_VERSION.equals(keyObject)) {
                    return true;
                }

                var key = keyObject != null ? keyObject.toString().toUpperCase(ENGLISH) : "";
                return key.isEmpty()
                    || key.endsWith("-DIGEST")
                    || key.endsWith("-DIGEST-MANIFEST");
            });
        });

        allEntryAttrs.removeIf(Attributes::isEmpty);

        mainAttrs.putIfAbsent(MANIFEST_VERSION, "1.0");

        if (!mainAttrs.containsKey(MULTI_RELEASE)) {
            var isMultiRelease = context.getProcessedResources().stream()
                .anyMatch(resource -> resource.getMultiReleaseVersion() != null);
            if (isMultiRelease) {
                mainAttrs.put(MULTI_RELEASE, "true");
            }
        }

        var out = new ByteArrayOutputStream();
        manifest.write(out);
        var modifiedContent = out.toByteArray();

        manifestResource = GeneratedResource.builder()
            .withSourceResource(manifestResource)
            .withContent(modifiedContent)
            .build();

        context.writeToOutput(manifestResource);

        return TASK_HANDLED;
    }

}
