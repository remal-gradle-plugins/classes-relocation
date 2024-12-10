package name.remal.gradle_plugins.classes_relocation.intern;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.classes_relocation.intern.classpath.Classpath.newClasspathForPaths;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmTestUtils.wrapWithTestClassVisitors;
import static name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils.toClassInternalName;
import static name.remal.gradle_plugins.toolkit.InTestFlags.isInTest;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazyProxy;
import static name.remal.gradle_plugins.toolkit.LazyProxy.asLazySetProxy;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

import java.io.Closeable;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.asm.NameClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.asm.RelocationAnnotationsClassVisitor;
import name.remal.gradle_plugins.classes_relocation.intern.asm.RelocationClassRemapper;
import name.remal.gradle_plugins.classes_relocation.intern.asm.RelocationRemapper;
import name.remal.gradle_plugins.classes_relocation.intern.classpath.Classpath;
import name.remal.gradle_plugins.classes_relocation.intern.utils.AsmUtils;
import name.remal.gradle_plugins.toolkit.AbstractClosablesContainer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

@Builder
@RequiredArgsConstructor(access = PRIVATE)
@CustomLog
public class ClassesRelocator extends AbstractClosablesContainer implements Closeable {

    public static final Charset DEFAULT_METADATA_CHARSET = UTF_8;

    private static final boolean IN_TEST = isInTest();


    private final Path sourceJarPath;

    @Singular
    private final List<Path> relocationClasspathPaths;

    @Singular
    private final List<Path> runtimeClasspathPaths;

    @Singular
    private final List<Path> compileClasspathPaths;

    @Singular
    private final Map<String, String> moduleIdentifiers;


    private final Path targetJarPath;

    private final String basePackageForRelocatedClasses;

    @Default
    private final Charset metadataCharset = DEFAULT_METADATA_CHARSET;

    private final boolean preserveFileTimestamps;


    private final Classpath pureSourceClasspath = asLazyProxy(Classpath.class, () ->
        registerCloseable(newClasspathForPaths(singletonList(sourceJarPath)))
    );

    private final Classpath pureRelocationClasspath = asLazyProxy(Classpath.class, () ->
        registerCloseable(newClasspathForPaths(relocationClasspathPaths))
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
        registerCloseable(new RelocationOutputImpl(targetJarPath, metadataCharset, preserveFileTimestamps))
    );

    private final Classpath classpath = asLazyProxy(Classpath.class, () ->
        registerCloseable(newClasspathForPaths(
            runtimeClasspathPaths,
            compileClasspathPaths
        ))
    );


    private final Deque<String> internalClassNamesToRelocate = new ArrayDeque<>();
    private final Set<String> processedInternalClassNames = new LinkedHashSet<>();

    @SneakyThrows
    public void relocate() {
        sourceClasspath.getClassNames().stream()
            .map(AsmUtils::toClassInternalName)
            .forEach(internalClassNamesToRelocate::addLast);
        processedInternalClassNames.addAll(internalClassNamesToRelocate);

        val remapper = new RelocationRemapper(
            toClassInternalName(basePackageForRelocatedClasses + '.'),
            relocationInternalClassNames::contains,
            internalName -> {
                if (processedInternalClassNames.add(internalName)) {
                    internalClassNamesToRelocate.addLast(internalName);
                }
            }
        );

        while (true) {
            val internalClassNameToRelocate = internalClassNamesToRelocate.pollFirst();
            if (internalClassNameToRelocate == null) {
                break;
            }

            val isSourceClass = sourceInternalClassNames.contains(internalClassNameToRelocate);
            val processedResourcesPaths = new LinkedHashSet<String>();
            val resources = sourceAndRelocationClasspath.getResources(internalClassNameToRelocate + ".class").stream()
                .filter(resource -> processedResourcesPaths.add(resource.getName()))
                .collect(toImmutableSet());
            for (val resource : resources) {
                val classWriter = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
                val testClassVisitor = IN_TEST ? wrapWithTestClassVisitors(classWriter) : classWriter;
                val relocatedNameVisitor = new NameClassVisitor(testClassVisitor);
                val annotationVisitor = isSourceClass
                    ? relocatedNameVisitor
                    : new RelocationAnnotationsClassVisitor(relocatedNameVisitor);
                val classRemapper = new RelocationClassRemapper(annotationVisitor, remapper);
                try (val in = resource.open()) {
                    new ClassReader(in).accept(classRemapper, 0);
                }

                val originalResourceName = resource.getName();
                val resourceNamePrefix = originalResourceName.substring(
                    0,
                    originalResourceName.length() - internalClassNameToRelocate.length() - ".class".length()
                );
                val relocatedResourceName = resourceNamePrefix + relocatedNameVisitor.getClassName() + ".class";
                output.write(relocatedResourceName, resource.getLastModifiedMillis(), classWriter.toByteArray());
            }
        }
    }

}
