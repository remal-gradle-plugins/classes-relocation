package name.remal.gradle_plugins.classes_relocation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static name.remal.gradle_plugins.classes_relocation.TaskClasspathConfigurers.TASK_CLASSPATH_CONFIGURERS;
import static name.remal.gradle_plugins.toolkit.AttributeContainerUtils.javaApiLibrary;
import static name.remal.gradle_plugins.toolkit.FileCollectionUtils.getModuleVersionIdentifiersForFilesIn;
import static name.remal.gradle_plugins.toolkit.GradleManagedObjectsUtils.copyManagedProperties;
import static name.remal.gradle_plugins.toolkit.JavaLauncherUtils.getJavaLauncherProviderFor;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.TaskPropertiesUtils.registerTaskProperties;
import static org.gradle.api.artifacts.Configuration.State.UNRESOLVED;
import static org.gradle.api.attributes.LibraryElements.JAR;
import static org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.CustomLog;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocationException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository.MetadataSources;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.jvm.tasks.Jar;

@CustomLog
public abstract class ClassesRelocationPlugin implements Plugin<Project> {

    public static final String CLASSES_RELOCATION_EXTENSION_NAME = doNotInline("classesRelocation");

    public static final String CLASSES_RELOCATION_LEGACY_CONFIGURATION_NAME = doNotInline("relocateClasses");
    public static final String CLASSES_RELOCATION_CONFIGURATION_NAME = doNotInline("classesRelocation");
    public static final String CLASSES_RELOCATION_CLASSPATH_CONFIGURATION_NAME =
        doNotInline("classesRelocationClasspath");

    public static final String GRAALVM_REACHABILITY_METADATA_REPOSITORY_NAME =
        doNotInline("graalvmReachabilityMetadata");
    public static final String GRAALVM_REACHABILITY_METADATA_REPOSITORY_URL =
        doNotInline("https://github.com");
    public static final String GRAALVM_REACHABILITY_METADATA_REPOSITORY_ARTIFACT_PATTERN =
        doNotInline("[organisation]/[module]/releases/download/[revision]/[artifact]-[revision].[ext]");

    public static final String REACHABILITY_METADATA_CONFIGURATION_NAME =
        doNotInline("classesRelocationReachabilityMetadata");

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


        val graalvmReachabilityMetadataRepo = getRepositories().ivy(repo -> {
            repo.setName(GRAALVM_REACHABILITY_METADATA_REPOSITORY_NAME);
            repo.setUrl(GRAALVM_REACHABILITY_METADATA_REPOSITORY_URL);
            repo.patternLayout(layout -> {
                layout.artifact(GRAALVM_REACHABILITY_METADATA_REPOSITORY_ARTIFACT_PATTERN);
                layout.setM2compatible(false);
            });
            repo.metadataSources(MetadataSources::artifact);
        });
        getRepositories().exclusiveContent(exclusive -> exclusive
            .forRepositories(graalvmReachabilityMetadataRepo)
            .filter(content -> content.includeModule("oracle", "graalvm-reachability-metadata"))
        );

        val reachabilityMetadataConfProvider = getConfigurations().register(
            REACHABILITY_METADATA_CONFIGURATION_NAME,
            conf -> {
                conf.setVisible(false);
                conf.setCanBeConsumed(false);
                conf.setCanBeResolved(true);
                conf.setDescription("GraalVM reachability metadata");
                conf.withDependencies(deps -> {
                    val dependencyVersion = extension.getMinimize().getGraalvmReachabilityMetadataVersion().get();
                    deps.add(getDependencies().create(format(
                        "oracle:graalvm-reachability-metadata:%s@zip",
                        dependencyVersion
                    )));
                });
            }
        );


        project.getPluginManager().withPlugin("java", __ -> {
            configureJavaProject(project, extension, depsConfProvider, confProvider, reachabilityMetadataConfProvider);
        });
    }


    private void configureJavaProject(
        Project project,
        ClassesRelocationExtension extension,
        NamedDomainObjectProvider<Configuration> relocationDepsConfProvider,
        NamedDomainObjectProvider<Configuration> relocationConfProvider,
        NamedDomainObjectProvider<Configuration> reachabilityMetadataConfProvider
    ) {
        setLibraryElementToJar(project);
        extendCompileClasspathConfiguration(relocationDepsConfProvider);
        resolveConsistentlyWithCompileClasspath(relocationConfProvider);

        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val mainSourceSetProvider = sourceSets.named(MAIN_SOURCE_SET_NAME);

        val jarProvider = getTasks().named(JAR_TASK_NAME, Jar.class);
        jarProvider.configure(jar -> {
            val action = getObjects().newInstance(RelocateJarAction.class);
            jar.getOutputs().cacheIf(RelocateJarAction.class.getName(), __ -> true);
            registerTaskProperties(jar, action, RelocateJarAction.class.getSimpleName());

            copyManagedProperties(ClassesRelocationSettings.class, extension, action);

            action.getRelocationClasspath().from(relocationConfProvider);
            action.getCompileAndRuntimeClasspath().from(getObjects().fileCollection()
                .from(mainSourceSetProvider
                    .map(SourceSet::getCompileClasspath)
                )
                .from(mainSourceSetProvider
                    .map(SourceSet::getRuntimeClasspathConfigurationName)
                    .flatMap(getConfigurations()::named)
                )
            );
            action.getReachabilityMetadataClasspath().from(reachabilityMetadataConfProvider);
            action.getModuleIdentifiers().putAll(retrieveModuleIdentifiers(action.getCompileAndRuntimeClasspath()));

            action.getJavaLauncher().convention(getJavaLauncherProviderFor(project));

            jar.doLast(RelocateJarAction.class.getName(), action);
        });


        TASK_CLASSPATH_CONFIGURERS.forEach(configurer ->
            configurer.configureTasks(project, mainSourceSetProvider, jarProvider)
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

    private Provider<Map<String, String>> retrieveModuleIdentifiers(FileCollection fileCollection) {
        return getProviders().provider(() -> {
            val moduleIdentifiers = new LinkedHashMap<String, String>();
            getModuleVersionIdentifiersForFilesIn(fileCollection).forEach((file, id) -> {
                moduleIdentifiers.putIfAbsent(
                    file.toPath().toUri().toString(),
                    format(
                        "%s:%s:%s",
                        id.getGroup(),
                        id.getName(),
                        id.getVersion()
                    )
                );
            });
            return moduleIdentifiers;
        });
    }


    @Inject
    protected abstract ConfigurationContainer getConfigurations();

    @Inject
    protected abstract RepositoryHandler getRepositories();

    @Inject
    protected abstract DependencyHandler getDependencies();

    @Inject
    protected abstract TaskContainer getTasks();

    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProviderFactory getProviders();

}
