package name.remal.gradle_plugins.classes_relocation.intern.relocator.manifest;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static java.util.jar.Attributes.Name.SIGNATURE_VERSION;
import static java.util.jar.JarFile.MANIFEST_NAME;

import java.util.Collection;
import java.util.jar.Attributes;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.content.ManifestContent;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.relocator.ResourceRelocator;
import name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils;

public class ManifestRelocator implements ResourceRelocator {

    private static final Pattern MULTI_RELEASE_RESOURCE_PATH = Pattern.compile(
        "^META-INF/versions/\\d+/.+$"
    );

    @Override
    public Collection<String> getInclusions() {
        return singletonList(MANIFEST_NAME);
    }

    @Override
    public void relocateResource(Resource resource, RelocationContext context) {
        if (!context.isSourceResource(resource)) {
            return;
        }

        resource.getContent()
            .with(ManifestContent.class, manifest -> {
                val mainAttrs = manifest.getMainAttributes();
                val allEntryAttrs = manifest.getEntries().values();

                Stream.concat(
                    Stream.of(mainAttrs),
                    allEntryAttrs.stream()
                ).forEach(attrs -> {
                    attrs.keySet().removeIf(keyObject -> {
                        if (SIGNATURE_VERSION.equals(keyObject)
                            || MultiReleaseUtils.MULTI_RELEASE_NAME.equals(keyObject)
                        ) {
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

                val isMultiRelease = context.getTargetEntries().stream()
                    .anyMatch(path -> MULTI_RELEASE_RESOURCE_PATH.matcher(path).matches());
                if (isMultiRelease) {
                    mainAttrs.put(MultiReleaseUtils.MULTI_RELEASE_NAME, "true");
                }
            })
            .writeTo(context.targetEntryFor(resource));
    }

    @Override
    public int getPriority() {
        return MANIFEST_PRIORITY;
    }

}
