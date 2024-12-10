package name.remal.gradle_plugins.classes_relocation.intern.task.license;

import static java.lang.String.format;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.sort;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.toolkit.StringUtils.substringAfterLast;
import static name.remal.gradle_plugins.toolkit.StringUtils.substringBeforeLast;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.classpath_old.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationContext;
import name.remal.gradle_plugins.classes_relocation.intern.task.RelocationTask;

@RequiredArgsConstructor
class CopyLicensesTask implements RelocationTask {

    private final Collection<Resource> allLicenseResources;

    @Override
    public int getPriority() {
        return FINALIZE_PRIORITY;
    }

    @Override
    public void execute(RelocationContext context) {
        val licenseResourcesToRelocate = allLicenseResources.stream()
            .filter(licenseResource ->
                licenseResource.getClasspathElement().getResources().stream()
                    .anyMatch(context::isResourceRelocated)
            )
            .collect(toList());

        for (val resource : licenseResourcesToRelocate) {
            val moduleIdentifier = getModuleIdentifier(context, resource);
            val escapedModuleIdentifier = escapeModuleIdentifier(moduleIdentifier);

            val targetDir = substringBeforeLast(resource.getPath(), "/", "");
            val targetFile = substringAfterLast(resource.getPath(), "/");

            val targetPath = format(
                "%s/%s-%s",
                targetDir,
                escapedModuleIdentifier,
                targetFile
            );

            resource.writeTo(context.targetEntryFor(resource, targetPath));
        }
    }

    private static String getModuleIdentifier(RelocationContext context, Resource resource) {
        String moduleIdentifier = context.getModuleIdentifier(resource);

        if (moduleIdentifier == null) {
            moduleIdentifier = resource.getClasspathElement().getModuleName();
        }

        return moduleIdentifier;
    }


    private static final char[] FORBIDDEN_MODULE_IDENTIFIER_CHARS = "\\/:<>\"'|?*${}()&[]^".toCharArray();

    static {
        sort(FORBIDDEN_MODULE_IDENTIFIER_CHARS);
    }

    private static String escapeModuleIdentifier(String moduleIdentifier) {
        val result = new StringBuilder(moduleIdentifier.length() + 1);
        for (int index = 0; index < result.length(); index++) {
            val ch = result.charAt(index);
            if (binarySearch(FORBIDDEN_MODULE_IDENTIFIER_CHARS, ch) >= 0) {
                result.append('-');
            } else if (ch < 32 || ch > 126) {
                result.append('-');
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

}
