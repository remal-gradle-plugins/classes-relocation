package name.remal.gradle_plugins.classes_relocation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.classes_relocation.SourceSetClasspathsCheckMode.DISABLE;
import static name.remal.gradle_plugins.classes_relocation.SourceSetClasspathsCheckMode.FAIL;
import static name.remal.gradle_plugins.toolkit.AttributeContainerUtils.javaRuntimeLibrary;
import static name.remal.gradle_plugins.toolkit.GradleManagedObjectsUtils.copyManagedProperties;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.PluginUtils.findPluginIdFor;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;
import static org.gradle.api.artifacts.Configuration.State.UNRESOLVED;
import static org.gradle.api.attributes.LibraryElements.JAR;
import static org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE;
import static org.gradle.api.plugins.BasePlugin.BUILD_GROUP;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.inject.Inject;
import lombok.CustomLog;
import lombok.Value;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocationException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.FileCollection;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;

@CustomLog
public abstract class ClassesRelocationPlugin implements Plugin<Project> {

    public static final String CLASSES_RELOCATION_EXTENSION_NAME = doNotInline("classesRelocation");
    public static final String CLASSES_RELOCATION_CONFIGURATION_NAME = doNotInline("classesRelocation");
    public static final String CLASSES_RELOCATION_CLASSPATH_CONFIGURATION_NAME =
        doNotInline("classesRelocationClasspath");
    public static final String RELOCATED_JAR_TASK_NAME = doNotInline("relocatedJar");

    @Override
    public void apply(Project project) {
        val extension = project.getExtensions().create(
            CLASSES_RELOCATION_EXTENSION_NAME,
            ClassesRelocationExtension.class
        );


        val depsConfProvider = getConfigurations().register(
            CLASSES_RELOCATION_CONFIGURATION_NAME,
            conf -> {
                conf.setCanBeConsumed(false);
                conf.setCanBeResolved(false);
                conf.setDescription("Dependencies for classes state");
            }
        );

        val confProvider = getConfigurations().register(
            CLASSES_RELOCATION_CLASSPATH_CONFIGURATION_NAME,
            conf -> {
                conf.setCanBeConsumed(true);
                conf.setCanBeResolved(true);
                conf.extendsFrom(depsConfProvider.get());
                conf.attributes(javaRuntimeLibrary(getObjects()));
                conf.setDescription(
                    "Classpath for classes state: dependencies took from " + depsConfProvider.getName()
                );
            }
        );


        configureTaskDefaults(extension, confProvider);


        project.getPluginManager().withPlugin("java", __ -> {
            configureJavaProject(project, extension, depsConfProvider, confProvider);
        });
    }

    private void configureTaskDefaults(
        ClassesRelocationExtension extension,
        NamedDomainObjectProvider<Configuration> relocationConfProvider
    ) {
        getTasks().withType(RelocateJar.class).configureEach(task -> {
            copyManagedProperties(ClassesRelocationSettings.class, extension, task);

            task.getRelocationClasspath().from(relocationConfProvider);
        });
    }

    private void configureJavaProject(
        Project project,
        ClassesRelocationExtension extension,
        NamedDomainObjectProvider<Configuration> relocationConfProvider,
        NamedDomainObjectProvider<Configuration> relocationClasspathConfProvider
    ) {
        setLibraryElementToJar(project);
        extendCompileClasspathConfiguration(relocationConfProvider);
        resolveConsistentlyWithCompileClasspath(relocationClasspathConfProvider);

        val jarProvider = getTasks().named(JAR_TASK_NAME, Jar.class);
        jarProvider.configure(jar -> {
            jar.getArchiveClassifier().set("original");
        });

        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val mainSourceSetProvider = sourceSets.named(MAIN_SOURCE_SET_NAME);

        val relocateJarProvider = getTasks().register(RELOCATED_JAR_TASK_NAME, RelocateJar.class, task -> {
            task.dependsOn(jarProvider);

            task.getJarFile().set(jarProvider.flatMap(Jar::getArchiveFile));
            task.getRuntimeClasspath().from(mainSourceSetProvider.map(SourceSet::getRuntimeClasspath));
            task.getCompileClasspath().from(mainSourceSetProvider.map(SourceSet::getCompileClasspath));
            task.getPreserveFileTimestamps().set(jarProvider.map(Jar::isPreserveFileTimestamps));
            task.getMetadataCharset().set(jarProvider.map(Jar::getMetadataCharset));

            task.getArchiveDestinationDirectory().set(jarProvider.flatMap(Jar::getDestinationDirectory));
            task.getArchiveBaseName().set(jarProvider.flatMap(Jar::getArchiveBaseName));
            task.getArchiveAppendix().set(jarProvider.flatMap(Jar::getArchiveAppendix));
            task.getArchiveVersion().set(jarProvider.flatMap(Jar::getArchiveVersion));
            task.getArchiveExtension().set(jarProvider.flatMap(Jar::getArchiveExtension));

            task.setDescription(format(
                "Relocate classes from dependencies of `%s` configuration in a JAR file created by %s task.",
                relocationClasspathConfProvider.getName(),
                jarProvider.getName()
            ));
            task.setGroup(BUILD_GROUP);
        });

        useRelocatedJarInsteadOfSourceSetOutput(project, extension, relocateJarProvider);
        publishRelocatedJar(jarProvider, relocateJarProvider);

        getTasks().withType(Test.class).configureEach(test ->
            test.shouldRunAfter(relocateJarProvider)
        );
    }

    private void setLibraryElementToJar(Project project) {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.configureEach(sourceSet -> {
            val configurationNames = asList(
                sourceSet.getCompileClasspathConfigurationName(),
                sourceSet.getRuntimeClasspathConfigurationName()
            );
            getConfigurations()
                .matching(conf -> configurationNames.contains(conf.getName()))
                .matching(conf -> conf.getState() == UNRESOLVED)
                .configureEach(conf -> conf.attributes(attrs ->
                    attrs.attribute(
                        LIBRARY_ELEMENTS_ATTRIBUTE,
                        getObjects().named(LibraryElements.class, JAR)
                    )
                ));
        });
    }

    private void extendCompileClasspathConfiguration(
        NamedDomainObjectProvider<Configuration> relocationConfProvider
    ) {
        getConfigurations().named(
            COMPILE_CLASSPATH_CONFIGURATION_NAME,
            conf -> conf.extendsFrom(relocationConfProvider.get())
        );
    }

    @SuppressWarnings("UnstableApiUsage")
    private void resolveConsistentlyWithCompileClasspath(
        NamedDomainObjectProvider<Configuration> relocationClasspathConfProvider
    ) {
        relocationClasspathConfProvider.configure(conf -> {
            val compileClasspathConf = getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME);
            conf.shouldResolveConsistentlyWith(compileClasspathConf);
            conf.setDescription(
                conf.getDescription() + ", resolved consistently with " + compileClasspathConf.getName()
            );
        });
    }

    private void publishRelocatedJar(
        Provider<Jar> jarProvider,
        Provider<RelocateJar> relocateJarProvider
    ) {
        getConfigurations()
            .matching(Configuration::isCanBeConsumed)
            .configureEach(conf -> publishRelocatedJar(conf, jarProvider, relocateJarProvider));
    }

    private void publishRelocatedJar(
        Configuration conf,
        Provider<Jar> jarProvider,
        Provider<RelocateJar> relocateJarProvider
    ) {
        val hasJar = conf.getArtifacts().removeIf(artifact ->
            Objects.equals(
                artifact.getFile(),
                jarProvider.get().getArchiveFile().get().getAsFile()
            )
        );
        if (!hasJar) {
            return;
        }

        getArtifacts().add(
            conf.getName(),
            relocateJarProvider.flatMap(RelocateJar::getTargetJarFile),
            it -> it.builtBy(relocateJarProvider)
        );
    }

    private void useRelocatedJarInsteadOfSourceSetOutput(
        Project project,
        ClassesRelocationExtension extension,
        Provider<RelocateJar> relocateJarProvider
    ) {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets
            .matching(sourceSet -> !sourceSet.getName().equals(MAIN_SOURCE_SET_NAME))
            .configureEach(sourceSet -> {
                val mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
                useRelocatedJarInsteadOfSourceSetOutput(
                    project,
                    extension.getSourceSetClasspaths(),
                    relocateJarProvider,
                    mainSourceSet,
                    sourceSet,
                    false
                );
            });
    }

    private void useRelocatedJarInsteadOfSourceSetOutput(
        Project project,
        ClassesRelocationSourceSetClasspaths sourceSetClasspaths,
        Provider<RelocateJar> relocateJarProvider,
        SourceSet mainSourceSet,
        SourceSet sourceSet,
        boolean isAfterEvaluate
    ) {
        val projectId = getProjectId(project);

        val mainOutput = mainSourceSet.getOutput();
        for (val classpathInfo : SOURCE_SET_CLASSPATH_INFOS) {
            val classpathName = classpathInfo.getName();
            val classpath = classpathInfo.getGetter().apply(sourceSet);
            classpathInfo.getSetter().accept(sourceSet, getObjects().fileCollection()
                .builtBy(classpath)
                .from(getProviders().provider(() ->
                    useRelocatedJarInsteadOfSourceSetOutput(
                        projectId,
                        sourceSetClasspaths,
                        relocateJarProvider,
                        classpathName,
                        sourceSet,
                        classpath,
                        mainSourceSet.getName(),
                        mainOutput,
                        isAfterEvaluate
                    )
                ))
            );
        }

        if (!isAfterEvaluate && !project.getState().getExecuted()) {
            project.afterEvaluate(__ ->
                useRelocatedJarInsteadOfSourceSetOutput(
                    project,
                    sourceSetClasspaths,
                    relocateJarProvider,
                    mainSourceSet,
                    sourceSet,
                    true
                )
            );
        }
    }

    @SuppressWarnings({"java:S3776", "java:S6541", "java:S107", "Slf4jFormatShouldBeConst"})
    private FileCollection useRelocatedJarInsteadOfSourceSetOutput(
        String projectId,
        ClassesRelocationSourceSetClasspaths sourceSetClasspaths,
        Provider<RelocateJar> relocateJarProvider,
        String classpathName,
        SourceSet sourceSet,
        FileCollection classpath,
        String mainSourceSetName,
        FileCollection mainOutput,
        boolean isAfterEvaluate
    ) {
        if (sourceSetClasspaths.getSkipSourceSets().contains(sourceSet)) {
            return classpath;
        }

        if (classpath.isEmpty()) {
            return classpath;
        }

        val mainOutputFiles = mainOutput.getFiles();
        if (mainOutputFiles.isEmpty()) {
            return classpath;
        }

        if (sourceSetClasspaths.getPartialMatchCheck().getOrNull() != DISABLE) {
            val notMatchedFiles = mainOutputFiles.stream()
                .filter(not(classpath::contains))
                .collect(toList());
            if (!notMatchedFiles.isEmpty() && notMatchedFiles.size() < mainOutputFiles.size()) {
                val message = (
                    "For project `{projectId}`, source set `{sourceSetName}` contain partial output"
                        + " of `{mainSourceSetName}` source set in `{classpathName}`"
                        + ", but missing the following files:\n{notMatchedFiles}"
                        + "\n\nIf you don't want `{pluginId}` plugin to process classpaths"
                        + " of `{sourceSetName}` source set"
                        + ", add this source set to `{extensionName}.sourceSetClasspaths.skipSourceSets`."
                        + "\n\nThis check can be configured by setting"
                        + " `{extensionName}.sourceSetClasspaths.partialMatchCheck`."
                        + "\n"
                ).replace("{projectId}", projectId)
                    .replace("{sourceSetName}", sourceSet.getName())
                    .replace("{mainSourceSetName}", mainSourceSetName)
                    .replace("{classpathName}", classpathName)
                    .replace("{notMatchedFiles}", notMatchedFiles.stream()
                        .map(file -> "  * " + file)
                        .collect(joining("\n"))
                    )
                    .replace("{pluginId}", String.valueOf(findPluginIdFor(ClassesRelocationPlugin.class)))
                    .replace("{extensionName}", CLASSES_RELOCATION_EXTENSION_NAME);
                if (sourceSetClasspaths.getPartialMatchCheck().getOrNull() == FAIL) {
                    throw new ClassesRelocationException(message);
                } else {
                    logger.warn(message);
                }
            }
        }

        boolean matched = false;
        val updatedClasspath = getObjects().fileCollection()
            .builtBy(classpath)
            .builtBy(relocateJarProvider);
        for (val file : classpath.getFiles()) {
            if (mainOutput.contains(file)) {
                if (!matched) {
                    updatedClasspath.from(relocateJarProvider.flatMap(RelocateJar::getTargetJarFile));
                    matched = true;
                }
            } else {
                updatedClasspath.from(file);
            }
        }
        if (!matched) {
            return classpath;
        }

        if (isAfterEvaluate && sourceSetClasspaths.getAfterEvaluateChangesCheck().getOrNull() != DISABLE) {
            val message = (
                "For project `{projectId}`, classpath `{classpathName}` of source set `{sourceSetName}`"
                    + " was updated after `{pluginId}` plugin was applied."
                    + "\nThe classpath was fixed on the `afterEvaluate` phase."
                    + "\nThe most common reason for this is when another plugin that configures source sets"
                    + " is applied after `{pluginId}` plugin was applied. For example, `name.remal.test-source-sets`."
                    + "\nThe easiest was to get rid of this message and make your build more stable is"
                    + " to apply `{pluginId}` plugin AFTER other plugins that configure source sets."
                    + "\n\nIf you don't want `{pluginId}` plugin to process classpaths"
                    + " of `{sourceSetName}` source set"
                    + ", add this source set to `{extensionName}.sourceSetClasspaths.skipSourceSets`."
                    + "\n\nThis check can be configured by setting"
                    + " `{extensionName}.sourceSetClasspaths.afterEvaluateChangesCheck`."
                    + "\n"
            ).replace("{projectId}", projectId)
                .replace("{classpathName}", classpathName)
                .replace("{sourceSetName}", sourceSet.getName())
                .replace("{pluginId}", String.valueOf(findPluginIdFor(ClassesRelocationPlugin.class)))
                .replace("{extensionName}", CLASSES_RELOCATION_EXTENSION_NAME);
            if (sourceSetClasspaths.getAfterEvaluateChangesCheck().getOrNull() == FAIL) {
                throw new ClassesRelocationException(message);
            } else {
                logger.warn(message);
            }
        }

        return updatedClasspath;
    }

    private static final List<SourceSetClasspathInfo> SOURCE_SET_CLASSPATH_INFOS = ImmutableList.of(
        new SourceSetClasspathInfo("compileClasspath", SourceSet::getCompileClasspath, SourceSet::setCompileClasspath),
        new SourceSetClasspathInfo("runtimeClasspath", SourceSet::getRuntimeClasspath, SourceSet::setRuntimeClasspath)
    );

    @Value
    private static class SourceSetClasspathInfo {
        String name;
        Function<SourceSet, FileCollection> getter;
        BiConsumer<SourceSet, FileCollection> setter;
    }

    private static String getProjectId(Project project) {
        Gradle gradle = project.getGradle();
        while (true) {
            val parentGradle = gradle.getParent();
            if (parentGradle != null) {
                gradle = parentGradle;
            } else {
                break;
            }
        }

        val rootProject = gradle.getRootProject();
        val rootProjectPath = rootProject.getProjectDir().toPath();
        val projectPath = project.getProjectDir().toPath();
        val relativePath = rootProjectPath.relativize(projectPath).toString();
        return relativePath.isEmpty() ? "." : relativePath;
    }


    @Inject
    protected abstract ConfigurationContainer getConfigurations();

    @Inject
    protected abstract TaskContainer getTasks();

    @Inject
    protected abstract ArtifactHandler getArtifacts();

    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProviderFactory getProviders();

}
