package name.remal.gradle_plugins.classes_relocation.intern;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.lang.String.format;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.sort;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparingInt;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static java.util.jar.Attributes.Name.SIGNATURE_VERSION;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.intern.MergedResource.newMergedResource;
import static name.remal.gradle_plugins.classes_relocation.intern.classpath.Classpath.newClasspathForPaths;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassName;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils.MULTI_RELEASE;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyListProxy;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyProxy;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazySetProxy;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isNotEmpty;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;
import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrowsFunction;
import static org.objectweb.asm.Type.getDescriptor;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.CustomLog;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.api.RelocateClasses;
import name.remal.gradle_plugins.classes_relocation.intern.asm.NameClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.asm.RelocationAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.asm.UnsupportedAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Classpath;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.resource_handler.ResourceProcessingContext;
import name.remal.gradle_plugins.classes_relocation.intern.resource_handler.ResourceProcessor;
import name.remal.gradle_plugins.classes_relocation.intern.resource_handler.ResourcesMerger;
import name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils;
import name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils;
import name.remal.gradle_plugins.toolkit.ClosablesContainer;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

@SuperBuilder
@CustomLog
public class ClassesRelocator extends ClassesRelocatorParams implements ResourceProcessingContext, Closeable {

    private static final boolean IN_TEST = isInTest();


    private final ClosablesContainer closables = new ClosablesContainer();

    @Override
    @OverridingMethodsMustInvokeSuper
    public void close() {
        closables.close();
    }


    private final Classpath sourceClasspath = asLazyProxy(Classpath.class, () ->
        closables.registerCloseable(newClasspathForPaths(singletonList(sourceJarPath)))
    );

    private final Classpath relocationClasspath = asLazyProxy(Classpath.class, () ->
        closables.registerCloseable(newClasspathForPaths(relocationClasspathPaths))
    );

    private final Classpath sourceAndRelocationClasspath = asLazyProxy(Classpath.class, () ->
        sourceClasspath.plus(relocationClasspath)
    );

    private final Set<String> sourceAndRelocationClasspathResourceNames = asLazySetProxy(() ->
        sourceAndRelocationClasspath.getResources().stream()
            .map(Resource::getName)
            .collect(toImmutableSet())
    );

    private final Set<String> sourceInternalClassNames = asLazySetProxy(() ->
        sourceClasspath.getClassNames().stream()
            .map(AsmUtils::toClassInternalName)
            .collect(toImmutableSet())
    );

    private final Set<String> relocationInternalClassNames = asLazySetProxy(() ->
        relocationClasspath.getClassNames().stream()
            .map(AsmUtils::toClassInternalName)
            .collect(toImmutableSet())
    );

    private final List<ResourcesMergerWithCompiledPatterns> compiledResourcesMergers = asLazyListProxy(() ->
        Stream.of(
                resourcesMergers,
                ImmutableList.copyOf(ServiceLoader.load(
                    ResourcesMerger.class,
                    ClassesRelocator.class.getClassLoader()
                ))
            )
            .flatMap(Collection::stream)
            .sorted(comparingInt(ResourcesMerger::getPriority))
            .map(ResourcesMergerWithCompiledPatterns::new)
            .collect(toImmutableList())
    );

    private final List<ResourceProcessorWithCompiledPatterns> compiledResourceProcessors = asLazyListProxy(() ->
        Stream.of(
                resourceProcessors,
                ImmutableList.copyOf(ServiceLoader.load(
                    ResourceProcessor.class,
                    ClassesRelocator.class.getClassLoader()
                ))
            )
            .flatMap(Collection::stream)
            .sorted(comparingInt(ResourceProcessor::getPriority))
            .map(ResourceProcessorWithCompiledPatterns::new)
            .collect(toImmutableList())
    );

    private final Classpath classpath = asLazyProxy(Classpath.class, () ->
        closables.registerCloseable(newClasspathForPaths(
            runtimeClasspathPaths,
            compileClasspathPaths
        ))
    );

    @Getter
    private final String relocatedClassNamePrefix = basePackageForRelocatedClasses + '.';

    @Getter
    private final String relocatedClassInternalNamePrefix = toClassInternalName(relocatedClassNamePrefix);

    private final RelocationOutput output = asLazyProxy(RelocationOutput.class, () ->
        closables.registerCloseable(new RelocationOutputImpl(targetJarPath, metadataCharset, preserveFileTimestamps))
    );


    private final Set<Resource> processedResources = new LinkedHashSet<>();

    private final Deque<ResourceRelocation> resourceRelocations = new ArrayDeque<>();
    private final Set<String> processedResourceNames = new LinkedHashSet<>();

    private final Deque<String> internalClassNamesToProcess = new ArrayDeque<>();
    private final Set<String> processedInternalClassNames = new LinkedHashSet<>();

    @SneakyThrows
    public void relocate() {
        val relocationPackageNames = relocationClasspath.getPackageNames();
        val sourceResourcesInRelocationPackages = sourceClasspath
            .matching(filter ->
                filter.includePackages(relocationPackageNames)
            )
            .getResources();
        if (!sourceResourcesInRelocationPackages.isEmpty()) {
            throw new SourceResourcesInRelocationPackagesException(sourceResourcesInRelocationPackages);
        }


        sourceClasspath.getClassNames().stream()
            .map(AsmUtils::toClassInternalName)
            .forEach(internalClassNamesToProcess::addLast);
        processedInternalClassNames.addAll(internalClassNamesToProcess);

        while (true) {
            val resourceRelocation = resourceRelocations.pollFirst();
            if (resourceRelocation != null) {
                processResource(resourceRelocation);
                continue;
            }

            val internalClassNameToProcess = internalClassNamesToProcess.pollFirst();
            if (internalClassNameToProcess != null) {
                processClass(internalClassNameToProcess);
                continue;
            }

            break;
        }


        copyLicensesOfRelocatedResources();
        processManifest();
        copyNotProcessedSourceResources();
    }


    @Override
    public void handleResourceName(String resourceName, String relocatedResourceName) {
        if (!sourceAndRelocationClasspathResourceNames.contains(resourceName)) {
            return;
        }

        if (processedResourceNames.add(resourceName)) {
            resourceRelocations.addLast(new ResourceRelocation(resourceName, relocatedResourceName));
        }
    }

    @SneakyThrows
    private void processResource(ResourceRelocation resourceRelocation) {
        val resourceName = resourceRelocation.getResourceName();
        val resourcesGroupedByName = sourceAndRelocationClasspath.getResources(resourceName).stream()
            .filter(not(processedResources::contains))
            .collect(groupingBy(Resource::getName));

        val resourcesGroupedByNameAndMerged = resourcesGroupedByName.entrySet().stream()
            .map(sneakyThrowsFunction(entry -> mergeResources(entry.getKey(), entry.getValue())))
            .collect(toList());

        for (val resource : resourcesGroupedByNameAndMerged) {
            val resourceProcessor = compiledResourceProcessors.stream()
                .filter(it -> it.matches(resourceName))
                .findFirst()
                .orElse(null);
            if (resourceProcessor != null) {
                val bytes = resourceProcessor.processResource(resource, this);
                output.write(resourceRelocation.getRelocatedResourceName(), resource.getLastModifiedMillis(), bytes);

            } else {
                try (val in = resource.open()) {
                    output.copy(resourceRelocation.getRelocatedResourceName(), resource.getLastModifiedMillis(), in);
                }
            }
        }
    }

    @SneakyThrows
    private Resource mergeResources(String resourceName, List<Resource> resources) {
        if (resources.size() == 1) {
            val resource = resources.get(0);
            processedResources.add(resource);
            return resource;
        }

        val merger = compiledResourcesMergers.stream()
            .filter(it -> it.matches(resourceName))
            .findFirst()
            .orElse(null);
        if (merger != null) {
            processedResources.addAll(resources);
            val lastModifiedMillis = resources.stream()
                .map(Resource::getLastModifiedMillis)
                .max(Long::compareTo)
                .orElse(null);
            val mergedContent = merger.merge(resourceName, resources);
            val resource = newMergedResource(resourceName, lastModifiedMillis, mergedContent);
            processedResources.add(resource);
            return resource;
        }

        val resource = resources.get(0);
        processedResources.add(resource);
        return resource;
    }


    private void processClass(String internalClassNameToProcess) {
        val resources = sourceAndRelocationClasspath
            .streamResourcesWithUniqueNames(internalClassNameToProcess + ".class")
            .filter(processedResources::add)
            .collect(toList());
        for (val resource : resources) {
            processClassAndResource(internalClassNameToProcess, resource);
        }
    }


    @Override
    public void handleInternalClassName(String internalClassName) {
        if (processedInternalClassNames.add(internalClassName)) {
            internalClassNamesToProcess.addLast(internalClassName);
        }
    }

    @SneakyThrows
    @SuppressWarnings({"java:S3776", "java:S1121", "VariableDeclarationUsageDistance"})
    private void processClassAndResource(String internalClassNameToProcess, Resource resource) {
        ClassVisitor classVisitor;
        val classWriter = (ClassWriter) (classVisitor = new ClassWriter(0));

        if (IN_TEST) {
            classVisitor = wrapWithTestClassVisitors(classVisitor);
        }

        val relocatedNameVisitor = (NameClassVisitor) (classVisitor = new NameClassVisitor(classVisitor));

        val isSourceClass = sourceInternalClassNames.contains(internalClassNameToProcess);
        if (!isSourceClass) {
            val relocationSource = getRelocationSource(resource);
            classVisitor = new RelocationAnnotationsClassVisitor(classVisitor, relocationSource);
        }


        val remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                if (relocationInternalClassNames.contains(internalName)) {
                    handleInternalClassName(internalName);
                    val relocatedInternalName = relocatedClassInternalNamePrefix + internalName;
                    handleResourceName(
                        "META-INF/services/" + internalName.replace('/', '.'),
                        "META-INF/services/" + relocatedInternalName.replace('/', '.')
                    );
                    return relocatedInternalName;
                }

                return internalName;
            }

            @Override
            public Object mapValue(Object value) {
                if (value instanceof String) {
                    val string = (String) value;
                    if (string.contains(".")) {
                        val internalName = toClassInternalName(string);
                        if (relocationInternalClassNames.contains(internalName)) {
                            val name = toClassName(internalName);
                            if (name.equals(string)) {
                                handleInternalClassName(internalName);
                                val relocatedInternalName = relocatedClassInternalNamePrefix + internalName;
                                return toClassName(relocatedInternalName);
                            }
                        }

                    } else if (string.contains("/")) {
                        if (relocationInternalClassNames.contains(string)) {
                            handleInternalClassName(string);
                            return relocatedClassInternalNamePrefix + string;
                        }

                        val descriptorMatcher = DESCRIPTOR_PATTERN.matcher(string);
                        if (descriptorMatcher.matches()) {
                            val internalName = descriptorMatcher.group(2);
                            if (relocationInternalClassNames.contains(internalName)) {
                                handleInternalClassName(internalName);
                                val relocatedInternalName = relocatedClassInternalNamePrefix + internalName;
                                return descriptorMatcher.group(1) + relocatedInternalName + descriptorMatcher.group(3);
                            }
                        }
                    }

                    // TODO: handle resources
                }

                return super.mapValue(value);
            }
        };
        classVisitor = new ClassRemapper(classVisitor, remapper);


        if (isSourceClass) {
            classVisitor = new UnsupportedAnnotationsClassVisitor(classVisitor,
                getDescriptor(RelocateClasses.class), // TODO: implement it instead of throwing an exception
                "Lname/remal/gradle_plugins/api/RelocateClasses;", // TODO: log it instead of throwing an exception
                "Lname/remal/gradle_plugins/api/RelocatePackages;" // TODO: log it instead of throwing an exception
            );
        }

        try (val in = resource.open()) {
            new ClassReader(in).accept(classVisitor, 0);
        }

        val originalResourceName = resource.getName();
        val resourceNamePrefix = originalResourceName.substring(
            0,
            originalResourceName.length() - internalClassNameToProcess.length() - ".class".length()
        );
        val relocatedResourceName = resourceNamePrefix + relocatedNameVisitor.getClassName() + ".class";
        output.write(relocatedResourceName, resource.getLastModifiedMillis(), classWriter.toByteArray());
    }

    private static final Pattern DESCRIPTOR_PATTERN = compile("^(\\[*L)([^;]+)(;)$");

    @Nullable
    private String getRelocationSource(Resource resource) {
        val classpathElement = resource.getClasspathElement();
        if (classpathElement == null) {
            return null;
        }

        val moduleIdentifier = moduleIdentifiers.get(classpathElement.getPath().toUri());
        if (isNotEmpty(moduleIdentifier)) {
            return moduleIdentifier;
        }

        return classpathElement.getModuleName();
    }


    private static final Pattern LICENSE_RESOURCE_NAME_PATTERN = Pattern.compile(
        "^(.*/)?[^/]*\\blicense\\b[^/]*$",
        CASE_INSENSITIVE
    );

    @SneakyThrows
    private void copyLicensesOfRelocatedResources() {
        val relocationResources = relocationClasspath.getResources();
        val licenseResources = ImmutableList.copyOf(processedResources).stream()
            .filter(relocationResources::contains)
            .map(Resource::getClasspathElement)
            .filter(Objects::nonNull)
            .flatMap(classpathElement ->
                classpathElement.streamResourcesWithUniqueNames()
                    .filter(resource -> LICENSE_RESOURCE_NAME_PATTERN.matcher(resource.getName()).matches())
            )
            .filter(processedResources::add)
            .collect(toList());
        if (licenseResources.isEmpty()) {
            return;
        }

        val forbiddenNameChars = "\\/:<>\"'|?*${}()&[]^".toCharArray();
        sort(forbiddenNameChars);

        @SuppressWarnings("java:S4276")
        Function<String, String> escapeName = name -> {
            val result = new StringBuilder(name.length());
            for (int index = 0; index < name.length(); index++) {
                val ch = name.charAt(index);
                if (binarySearch(forbiddenNameChars, ch) >= 0) {
                    result.append('-');
                } else if (ch < 32 || ch > 126) {
                    result.append('-');
                } else {
                    result.append(ch);
                }
            }
            return result.toString();
        };

        for (val resource : licenseResources) {
            val fullName = resource.getName();

            val prefixDelimPos = fullName.lastIndexOf('/');
            val namePrefix = prefixDelimPos >= 0
                ? fullName.substring(0, prefixDelimPos + 1)
                : "";

            val name = fullName.substring(namePrefix.length());

            val relocationSource = getRelocationSource(resource);
            val relocatedName = escapeName.apply(requireNonNull(relocationSource)) + '-' + name;
            val relocatedFullName = namePrefix + relocatedName;

            try (val in = resource.open()) {
                output.copy(relocatedFullName, resource.getLastModifiedMillis(), in);
            }
        }
    }


    @SneakyThrows
    private void processManifest() {
        val manifest = new Manifest();
        val mainAttrs = manifest.getMainAttributes();

        val manifestResource = sourceClasspath.getResources().stream()
            .filter(resource -> resource.getName().equals(MANIFEST_NAME))
            .filter(processedResources::add)
            .findFirst()
            .orElse(null);
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
            val isMultiRelease = processedResources.stream()
                .anyMatch(MultiReleaseUtils::isMultiRelease);
            if (isMultiRelease) {
                mainAttrs.put(MULTI_RELEASE, "true");
            }
        }

        val out = new ByteArrayOutputStream();
        manifest.write(out);
        val bytes = out.toByteArray();
        output.write(
            MANIFEST_NAME,
            manifestResource != null ? manifestResource.getLastModifiedMillis() : null,
            bytes
        );
    }


    @SneakyThrows
    private void copyNotProcessedSourceResources() {
        val resourceToCopy = sourceClasspath.streamResourcesWithUniqueNames()
            .filter(processedResources::add)
            .collect(toList());
        for (val resource : resourceToCopy) {
            try (val in = resource.open()) {
                output.copy(resource.getName(), resource.getLastModifiedMillis(), in);
            }
        }
    }


    @Value
    private static class ResourceRelocation {
        String resourceName;
        String relocatedResourceName;
    }

}
