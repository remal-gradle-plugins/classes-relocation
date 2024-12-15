package name.remal.gradle_plugins.classes_relocation.intern.task.queued.manifest;

import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static java.util.jar.Attributes.Name.SIGNATURE_VERSION;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static name.remal.gradle_plugins.classes_relocation.intern.classpath.GeneratedResource.newGeneratedResource;
import static name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult.TASK_HANDLED;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils.MULTI_RELEASE;

import java.io.ByteArrayOutputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandler;
import name.remal.gradle_plugins.classes_relocation.intern.task.queued.QueuedTaskHandlerResult;

public class ProcessManifestHandler implements QueuedTaskHandler<ProcessManifest> {

    @Override
    @SneakyThrows
    public QueuedTaskHandlerResult handle(ProcessManifest task, RelocationContext context) {
        val manifest = new Manifest();
        val mainAttrs = manifest.getMainAttributes();

        Resource manifestResource = context.getSourceClasspath().getResources(MANIFEST_NAME)
            .stream().findFirst().orElse(null);
        if (manifestResource != null) {
            try (val in = manifestResource.open()) {
                manifest.read(in);
            }

            val manifestVersion = mainAttrs.getValue(MANIFEST_VERSION);
            if (!"1.0".equals(manifestVersion)) {
                throw new IllegalStateException(format(
                    "%s: manifest version version `%s`, that's not supported by the plugin",
                    manifestResource,
                    manifestVersion
                ));
            }
        } else {
            manifestResource = newGeneratedResource(MANIFEST_NAME, new byte[0]);
        }

        val allEntryAttrs = manifest.getEntries().values();

        Stream.concat(
            Stream.of(mainAttrs),
            allEntryAttrs.stream()
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

        allEntryAttrs.removeIf(Attributes::isEmpty);

        if (!mainAttrs.containsKey(MANIFEST_VERSION)) {
            mainAttrs.put(MANIFEST_VERSION, "1.0");
        }

        if (!mainAttrs.containsKey(MULTI_RELEASE)) {
            val isMultiRelease = context.getProcessedResources().stream()
                .anyMatch(resource -> resource.getMultiReleaseVersion() != null);
            if (isMultiRelease) {
                mainAttrs.put(MULTI_RELEASE, "true");
            }
        }

        val out = new ByteArrayOutputStream();
        manifest.write(out);
        val modifiedContent = out.toByteArray();

        context.writeToOutput(manifestResource, modifiedContent);

        return TASK_HANDLED;
    }

}
