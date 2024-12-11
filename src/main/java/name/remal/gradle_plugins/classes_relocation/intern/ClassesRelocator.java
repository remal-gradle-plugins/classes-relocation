package name.remal.gradle_plugins.classes_relocation.intern;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static java.util.jar.Attributes.Name.SIGNATURE_VERSION;
import static java.util.jar.JarFile.MANIFEST_NAME;
import static name.remal.gradle_plugins.classes_relocation.intern.classpath.Classpath.newClasspathForPaths;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassName;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils.MULTI_RELEASE;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyProxy;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazySetProxy;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Type.getDescriptor;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.api.RelocateClasses;
import name.remal.gradle_plugins.classes_relocation.intern.asm.NameClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.asm.RelocationAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.asm.UnsupportedAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Classpath;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.ClasspathElement;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Resource;
import name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils;
import name.remal.gradle_plugins.classes_relocation.intern.utils.MultiReleaseUtils;
import name.remal.gradle_plugins.toolkit.ClosablesContainer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

@SuperBuilder
@CustomLog
public class ClassesRelocator extends ClassesRelocatorParams implements Closeable {

    private static final boolean IN_TEST = isInTest();


    private final ClosablesContainer closables = new ClosablesContainer();

    @Override
    @OverridingMethodsMustInvokeSuper
    public void close() {
        closables.close();
    }


    private final Classpath pureSourceClasspath = asLazyProxy(Classpath.class, () ->
        closables.registerCloseable(newClasspathForPaths(singletonList(sourceJarPath)))
    );

    private final Classpath pureRelocationClasspath = asLazyProxy(Classpath.class, () ->
        closables.registerCloseable(newClasspathForPaths(relocationClasspathPaths))
    );

    private final Classpath sourceClasspath = asLazyProxy(Classpath.class, () ->
        pureSourceClasspath
            .matching(filter ->
                filter.excludePackages(pureRelocationClasspath.getPackageNames())
            )
    );

    private final Classpath relocationClasspath = asLazyProxy(Classpath.class, () ->
        pureSourceClasspath
            .matching(filter ->
                filter.includePackages(pureRelocationClasspath.getPackageNames())
            )
            .plus(pureRelocationClasspath)
    );

    private final Classpath sourceAndRelocationClasspath = asLazyProxy(Classpath.class, () ->
        sourceClasspath.plus(relocationClasspath)
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

    private final RelocationOutput output = asLazyProxy(RelocationOutput.class, () ->
        closables.registerCloseable(new RelocationOutputImpl(targetJarPath, metadataCharset, preserveFileTimestamps))
    );

    private final Classpath classpath = asLazyProxy(Classpath.class, () ->
        closables.registerCloseable(newClasspathForPaths(
            runtimeClasspathPaths,
            compileClasspathPaths
        ))
    );


    private final Deque<String> internalClassNamesToProcess = new ArrayDeque<>();
    private final Set<String> processedInternalClassNames = new LinkedHashSet<>();
    private final Set<Resource> processedResources = new LinkedHashSet<>();

    @SneakyThrows
    public void relocate() {
        sourceClasspath.getClassNames().stream()
            .map(AsmUtils::toClassInternalName)
            .forEach(internalClassNamesToProcess::addLast);
        processedInternalClassNames.addAll(internalClassNamesToProcess);

        while (true) {
            val internalClassNameToProcess = internalClassNamesToProcess.pollFirst();
            if (internalClassNameToProcess == null) {
                break;
            }

            processClass(internalClassNameToProcess);
        }

        processManifest();
    }

    private void processClass(String internalClassNameToProcess) {
        val processedResourcesPaths = new LinkedHashSet<String>();
        val resources = sourceAndRelocationClasspath.getResources(internalClassNameToProcess + ".class").stream()
            .filter(resource -> processedResourcesPaths.add(resource.getName()))
            .collect(toImmutableSet());
        for (val resource : resources) {
            processClassAndResource(internalClassNameToProcess, resource);
        }
    }

    private void handleInternalClassName(String internalClassName) {
        if (processedInternalClassNames.add(internalClassName)) {
            internalClassNamesToProcess.addLast(internalClassName);
        }
    }

    private static final Pattern DESCRIPTOR_PATTERN = Pattern.compile("^(\\[*L)([^;]+)(;)$");

    @SneakyThrows
    @SuppressWarnings({"java:S3776", "java:S1121", "VariableDeclarationUsageDistance"})
    private void processClassAndResource(String internalClassNameToProcess, Resource resource) {
        processedResources.add(resource);

        ClassVisitor classVisitor;
        val classWriter = (ClassWriter) (classVisitor = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES));

        if (IN_TEST) {
            classVisitor = wrapWithTestClassVisitors(classVisitor);
        }

        val relocatedNameVisitor = (NameClassVisitor) (classVisitor = new NameClassVisitor(classVisitor));

        val isSourceClass = sourceInternalClassNames.contains(internalClassNameToProcess);
        if (!isSourceClass) {
            val relocationSource = Optional.ofNullable(resource.getClasspathElement())
                .map(ClasspathElement::getPath)
                .map(Path::toUri)
                .map(moduleIdentifiers::get)
                .filter(not(String::isEmpty))
                .orElseGet(() ->
                    Optional.ofNullable(resource.getClasspathElement())
                        .map(ClasspathElement::getModuleName)
                        .filter(not(String::isEmpty))
                        .orElse(null)
                );
            classVisitor = new RelocationAnnotationsClassVisitor(classVisitor, relocationSource);
        }


        val relocatedClassInternalNamePrefix = toClassInternalName(basePackageForRelocatedClasses + '.');
        val remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                if (relocationInternalClassNames.contains(internalName)) {
                    handleInternalClassName(internalName);
                    return relocatedClassInternalNamePrefix + internalName;
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

    @SneakyThrows
    private void processManifest() {
        val manifest = new Manifest();
        val mainAttrs = manifest.getMainAttributes();

        val manifestResource = pureSourceClasspath.getResources().stream()
            .filter(resource -> resource.getName().equals(MANIFEST_NAME))
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
    }

}
