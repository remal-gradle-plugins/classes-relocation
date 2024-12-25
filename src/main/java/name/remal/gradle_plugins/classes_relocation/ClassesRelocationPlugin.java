package name.remal.gradle_plugins.classes_relocation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static name.remal.gradle_plugins.toolkit.AttributeContainerUtils.javaApiLibrary;
import static name.remal.gradle_plugins.toolkit.FileCollectionUtils.getModuleVersionIdentifiersForFilesIn;
import static name.remal.gradle_plugins.toolkit.GradleManagedObjectsUtils.copyManagedProperties;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.TaskUtils.doBeforeTaskExecution;
import static org.gradle.api.artifacts.Configuration.State.UNRESOLVED;
import static org.gradle.api.attributes.LibraryElements.JAR;
import static org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE;
import static org.gradle.api.plugins.ApplicationPlugin.TASK_RUN_NAME;
import static org.gradle.api.plugins.BasePlugin.ASSEMBLE_TASK_NAME;
import static org.gradle.api.plugins.BasePlugin.BUILD_GROUP;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.CustomLog;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocationException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.application.tasks.CreateStartScripts;
import org.gradle.jvm.tasks.Jar;
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata;
import org.gradle.testing.jacoco.tasks.JacocoReportBase;

@CustomLog
public abstract class ClassesRelocationPlugin implements Plugin<Project> {

    public static final String CLASSES_RELOCATION_EXTENSION_NAME = doNotInline("classesRelocation");

    public static final String CLASSES_RELOCATION_LEGACY_CONFIGURATION_NAME = doNotInline("relocateClasses");
    public static final String CLASSES_RELOCATION_CONFIGURATION_NAME = doNotInline("classesRelocation");
    public static final String CLASSES_RELOCATION_CLASSPATH_CONFIGURATION_NAME =
        doNotInline("classesRelocationClasspath");

    public static final String RELOCATE_JAR_TASK_NAME = doNotInline("relocateJar");

    @Override
    @SuppressWarnings("Slf4jFormatShouldBeConst")
    public void apply(Project project) {
        val extension = project.getExtensions().create(
            CLASSES_RELOCATION_EXTENSION_NAME,
            ClassesRelocationExtension.class
        );


        val depsLegacyConfProvider = getConfigurations().register(
            CLASSES_RELOCATION_LEGACY_CONFIGURATION_NAME,
            conf -> {
                conf.setVisible(false);
                conf.setCanBeConsumed(false);
                conf.setCanBeResolved(false);
                conf.setDescription(
                    "Dependencies for classes state (legacy configuration for backwards compatibility)"
                );

                conf.getDependencies().configureEach(__ -> {
                    val message = format(
                        "Use `%s` configuration for classes relocation dependencies instead of the legacy `%s`",
                        CLASSES_RELOCATION_CONFIGURATION_NAME,
                        CLASSES_RELOCATION_LEGACY_CONFIGURATION_NAME
                    );
                    val exception = new ClassesRelocationException(message);
                    logger.warn(exception.toString(), exception);
                });
            }
        );

        val depsConfProvider = getConfigurations().register(
            CLASSES_RELOCATION_CONFIGURATION_NAME,
            conf -> {
                conf.setVisible(false);
                conf.setCanBeConsumed(false);
                conf.setCanBeResolved(false);
                conf.extendsFrom(depsLegacyConfProvider.get());
                conf.setDescription("Dependencies for classes relocation");
            }
        );

        val confProvider = getConfigurations().register(
            CLASSES_RELOCATION_CLASSPATH_CONFIGURATION_NAME,
            conf -> {
                conf.setVisible(false);
                conf.setCanBeConsumed(false);
                conf.setCanBeResolved(true);
                conf.extendsFrom(depsConfProvider.get());
                conf.attributes(javaApiLibrary(getObjects()));
                conf.setDescription(format(
                    "Classpath for classes relocation (dependencies are taken from `%s` configuration)",
                    depsConfProvider.getName()
                ));
            }
        );


        configureTaskDefaults(extension, confProvider);


        project.getPluginManager().withPlugin("java", __ -> {
            configureJavaProject(project, depsConfProvider, confProvider);
        });
    }


    private void configureTaskDefaults(
        ClassesRelocationExtension extension,
        NamedDomainObjectProvider<Configuration> relocationConfProvider
    ) {
        getTasks().withType(RelocateJar.class).configureEach(task -> {
            copyManagedProperties(ClassesRelocationSettings.class, extension, task);

            task.getRelocationClasspath().from(relocationConfProvider);
            task.getModuleIdentifiers().putAll(retrieveModuleIdentifiers(relocationConfProvider));
        });
    }

    private Provider<Map<String, String>> retrieveModuleIdentifiers(
        Provider<? extends FileCollection> fileCollectionProvider
    ) {
        return getProviders().provider(() -> {
            val fileCollection = fileCollectionProvider.getOrNull();
            if (fileCollection == null) {
                return emptyMap();
            }

            val moduleIdentifiers = new LinkedHashMap<String, String>();
            getModuleVersionIdentifiersForFilesIn(fileCollection).forEach((file, id) -> {
                moduleIdentifiers.putIfAbsent(
                    file.toPath().toUri().toString(),
                    format(
                        "%s:%s",
                        id.getGroup(),
                        id.getName()
                    )
                );
            });
            return moduleIdentifiers;
        });
    }


    private void configureJavaProject(
        Project project,
        NamedDomainObjectProvider<Configuration> relocationDepsConfProvider,
        NamedDomainObjectProvider<Configuration> relocationConfProvider
    ) {
        setLibraryElementToJar(project);
        extendCompileClasspathConfiguration(relocationDepsConfProvider);
        resolveConsistentlyWithCompileClasspath(relocationConfProvider);

        val jarProvider = getTasks().named(JAR_TASK_NAME, Jar.class);
        jarProvider.configure(jar -> {
            jar.getArchiveClassifier().set("original");
        });

        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val mainSourceSetProvider = sourceSets.named(MAIN_SOURCE_SET_NAME);

        val relocateJarProvider = getTasks().register(RELOCATE_JAR_TASK_NAME, RelocateJar.class, task -> {
            task.dependsOn(jarProvider);

            task.getJarFile().set(jarProvider.flatMap(Jar::getArchiveFile));
            task.getModuleIdentifiers().putAll(retrieveModuleIdentifiers(mainSourceSetProvider
                .map(SourceSet::getCompileClasspath)
            ));
            task.getPreserveFileTimestamps().set(jarProvider.map(Jar::isPreserveFileTimestamps));
            task.getMetadataCharset().set(jarProvider.map(Jar::getMetadataCharset));

            task.getArchiveDestinationDirectory().set(jarProvider.flatMap(Jar::getDestinationDirectory));
            task.getArchiveBaseName().set(jarProvider.flatMap(Jar::getArchiveBaseName));
            task.getArchiveAppendix().set(jarProvider.flatMap(Jar::getArchiveAppendix));
            task.getArchiveVersion().set(jarProvider.flatMap(Jar::getArchiveVersion));
            task.getArchiveExtension().set(jarProvider.flatMap(Jar::getArchiveExtension));

            task.setDescription(format(
                "Relocate classes from dependencies of `%s` configuration in a JAR file created by `%s` task.",
                relocationConfProvider.getName(),
                jarProvider.getName()
            ));
            task.setGroup(BUILD_GROUP);
        });

        getTasks().named(ASSEMBLE_TASK_NAME, task ->
            task.dependsOn(relocateJarProvider)
        );


        publishRelocatedJar(jarProvider, relocateJarProvider);


        configureTestTasks(mainSourceSetProvider, relocateJarProvider);

        configurePluginUnderTestMetadataTasks(mainSourceSetProvider, relocateJarProvider);

        // TODO: configure ValidatePlugins
        // TODO: configure JavaExec

        configureJacocoReportBaseTasks(mainSourceSetProvider, relocateJarProvider);

        configureCreateStartScriptsTasks(mainSourceSetProvider, relocateJarProvider);

        project.getPluginManager().withPlugin("application", __ -> {
            configureApplicationRunTask(mainSourceSetProvider, relocateJarProvider);
        });
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
        NamedDomainObjectProvider<Configuration> relocationDepsConfProvider
    ) {
        getConfigurations().named(
            COMPILE_CLASSPATH_CONFIGURATION_NAME,
            conf -> conf.extendsFrom(relocationDepsConfProvider.get())
        );
    }

    @SuppressWarnings("UnstableApiUsage")
    private void resolveConsistentlyWithCompileClasspath(
        NamedDomainObjectProvider<Configuration> relocationConfProvider
    ) {
        relocationConfProvider.configure(conf -> {
            val compileClasspathConf = getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME);
            conf.shouldResolveConsistentlyWith(compileClasspathConf);
            conf.setDescription(format(
                "%s, resolved consistently with `%s` configuration",
                conf.getDescription(),
                compileClasspathConf.getName()
            ));
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


    private void configureTestTasks(
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<RelocateJar> relocateJarProvider
    ) {
        getTasks().withType(Test.class).configureEach(it -> {
            it.dependsOn(relocateJarProvider);
            doBeforeTaskExecution(it, task -> {
                val classpath = task.getClasspath();
                val modifiedClasspath = createModifiedClasspath(
                    classpath,
                    sourceSetProvider,
                    relocateJarProvider
                );
                if (modifiedClasspath != null) {
                    modifiedClasspath.builtBy(classpath);
                    task.setClasspath(modifiedClasspath);
                }
            });
        });
    }

    private void configurePluginUnderTestMetadataTasks(
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<RelocateJar> relocateJarProvider
    ) {
        getTasks().withType(PluginUnderTestMetadata.class).configureEach(it -> {
            it.dependsOn(relocateJarProvider);
            doBeforeTaskExecution(it, task -> {
                val classpath = task.getPluginClasspath();
                val modifiedClasspath = createModifiedClasspath(
                    classpath,
                    sourceSetProvider,
                    relocateJarProvider
                );
                if (modifiedClasspath != null) {
                    classpath.setFrom(modifiedClasspath);
                }
            });
        });
    }

    private void configureJacocoReportBaseTasks(
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<RelocateJar> relocateJarProvider
    ) {
        getTasks().withType(JacocoReportBase.class).configureEach(it -> {
            it.dependsOn(relocateJarProvider);
            doBeforeTaskExecution(it, task -> {
                val classpath = task.getClassDirectories();
                val modifiedClasspath = createModifiedClasspath(
                    classpath,
                    sourceSetProvider,
                    relocateJarProvider
                );
                if (modifiedClasspath != null) {
                    classpath.setFrom(modifiedClasspath);
                }
            });
            doBeforeTaskExecution(it, task -> {
                val classpath = task.getAdditionalClassDirs();
                val modifiedClasspath = createModifiedClasspath(
                    classpath,
                    sourceSetProvider,
                    relocateJarProvider
                );
                if (modifiedClasspath != null) {
                    classpath.setFrom(modifiedClasspath);
                }
            });
        });
    }

    private void configureCreateStartScriptsTasks(
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<RelocateJar> relocateJarProvider
    ) {
        getTasks().withType(CreateStartScripts.class).configureEach(it -> {
            it.dependsOn(relocateJarProvider);
            doBeforeTaskExecution(it, task -> {
                val classpath = task.getClasspath();
                if (classpath == null) {
                    return;
                }

                val modifiedClasspath = createModifiedClasspath(
                    classpath,
                    sourceSetProvider,
                    relocateJarProvider
                );
                if (modifiedClasspath != null) {
                    modifiedClasspath.builtBy(classpath);
                    task.setClasspath(modifiedClasspath);
                }
            });
        });
    }

    private void configureApplicationRunTask(
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<RelocateJar> relocateJarProvider
    ) {
        getTasks().named(TASK_RUN_NAME, JavaExec.class, it -> {
            it.dependsOn(relocateJarProvider);
            doBeforeTaskExecution(it, task -> {
                val classpath = task.getClasspath();
                val modifiedClasspath = createModifiedClasspath(
                    classpath,
                    sourceSetProvider,
                    relocateJarProvider
                );
                if (modifiedClasspath != null) {
                    modifiedClasspath.builtBy(classpath);
                    task.setClasspath(modifiedClasspath);
                }
            });
        });
    }

    @Nullable
    private ConfigurableFileCollection createModifiedClasspath(
        FileCollection classpath,
        NamedDomainObjectProvider<SourceSet> sourceSetProvider,
        TaskProvider<RelocateJar> relocateJarProvider
    ) {
        ConfigurableFileCollection modifiedClasspath = null;
        boolean foundSourceSetOutput = false;
        val sourceSetOutputFiles = sourceSetProvider.get().getOutput().getFiles();
        for (val file : classpath.getFiles()) {
            if (sourceSetOutputFiles.contains(file)) {
                if (!foundSourceSetOutput) {
                    if (modifiedClasspath == null) {
                        modifiedClasspath = getObjects().fileCollection();
                    }
                    modifiedClasspath.from(relocateJarProvider
                        .flatMap(RelocateJar::getTargetJarFile)
                    );
                    foundSourceSetOutput = true;
                }
            } else {
                if (modifiedClasspath == null) {
                    modifiedClasspath = getObjects().fileCollection();
                }
                modifiedClasspath.from(file);
            }
        }

        if (modifiedClasspath != null) {
            return modifiedClasspath.builtBy(relocateJarProvider);
        }

        return null;
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
