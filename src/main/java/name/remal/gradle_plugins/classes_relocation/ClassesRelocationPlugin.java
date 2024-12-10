package name.remal.gradle_plugins.classes_relocation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static name.remal.gradle_plugins.toolkit.AttributeContainerUtils.javaRuntimeLibrary;
import static name.remal.gradle_plugins.toolkit.GradleManagedObjectsUtils.copyManagedProperties;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static org.gradle.api.attributes.LibraryElements.JAR;
import static org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import lombok.CustomLog;
import lombok.val;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
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

        val depsConfProvider = project.getConfigurations().register(
            CLASSES_RELOCATION_CONFIGURATION_NAME,
            conf -> {
                conf.setCanBeConsumed(false);
                conf.setCanBeResolved(false);
                conf.setDescription("Dependencies for classes state");
            }
        );

        val confProvider = project.getConfigurations().register(
            CLASSES_RELOCATION_CONFIGURATION_NAME,
            conf -> {
                conf.setCanBeConsumed(true);
                conf.setCanBeResolved(true);
                conf.extendsFrom(depsConfProvider.get());
                conf.attributes(javaRuntimeLibrary(project.getObjects()));
                conf.setDescription(
                    "Classpath for classes state: dependencies took from " + depsConfProvider.getName()
                );
            }
        );

        configureTaskDefaults(project, extension, confProvider);

        project.getPluginManager().withPlugin("java-base", __ -> {
            confProvider.configure(conf -> {
                val compileClasspathConf = project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME);
                conf.shouldResolveConsistentlyWith(compileClasspathConf);
                conf.setDescription(
                    conf.getDescription() + ", resolved consistently with " + compileClasspathConf.getName()
                );
            });

            configureJavaProject(project, extension, confProvider);
        });
    }

    private void configureTaskDefaults(
        Project project,
        ClassesRelocationExtension extension,
        NamedDomainObjectProvider<Configuration> relocationConfProvider
    ) {
        project.getTasks().withType(RelocateJar.class).configureEach(task -> {
            copyManagedProperties(ClassesRelocationSettings.class, extension, task);

            task.getRelocationClasspath().from(relocationConfProvider);
        });
    }

    private void configureJavaProject(
        Project project,
        ClassesRelocationExtension extension,
        NamedDomainObjectProvider<Configuration> relocationConfProvider
    ) {
        logger.warn("extension={}", extension);

        setLibraryElementToJar(project);
        extendCompileOnlyConfiguration(project, relocationConfProvider);

        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

        val relocatedJarProvider = project.getTasks().register(RELOCATED_JAR_TASK_NAME, RelocateJar.class, task -> {
            val jar = project.getTasks().withType(Jar.class).getByName(JAR_TASK_NAME);
            task.dependsOn(jar);

            task.getJarFile().set(jar.getArchiveFile());
            task.getRuntimeClasspath().from(sourceSets.named(MAIN_SOURCE_SET_NAME)
                .map(SourceSet::getRuntimeClasspath)
            );
            task.getCompileClasspath().from(sourceSets.named(MAIN_SOURCE_SET_NAME)
                .map(SourceSet::getCompileClasspath)
            );
            task.getPreserveFileTimestamps().set(project.provider(jar::isPreserveFileTimestamps));
            task.getMetadataCharset().set(project.provider(jar::getMetadataCharset));

            task.setDescription(format(
                "Relocate classes from dependencies of `%s` configuration in a JAR file created by %s task.",
                relocationConfProvider.getName(),
                jar.getPath()
            ));
            task.setGroup(jar.getGroup());
        });
        project.getTasks().withType(Test.class).configureEach(test ->
            test.shouldRunAfter(relocatedJarProvider)
        );
    }

    private void extendCompileOnlyConfiguration(
        Project project,
        NamedDomainObjectProvider<Configuration> relocationConfProvider
    ) {
        project.getConfigurations().named(
            COMPILE_ONLY_CONFIGURATION_NAME,
            conf -> conf.extendsFrom(relocationConfProvider.get())
        );
    }

    private void setLibraryElementToJar(Project project) {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.configureEach(sourceSet -> {
            val configurationNames = asList(
                sourceSet.getCompileClasspathConfigurationName(),
                sourceSet.getRuntimeClasspathConfigurationName()
            );
            project.getConfigurations()
                .matching(conf -> configurationNames.contains(conf.getName()))
                .configureEach(conf -> conf.attributes(attrs ->
                    attrs.attribute(
                        LIBRARY_ELEMENTS_ATTRIBUTE,
                        project.getObjects().named(LibraryElements.class, JAR)
                    )
                ));
        });
    }

}
