package name.remal.gradle_plugins.classes_relocation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;

import lombok.CustomLog;
import lombok.val;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;

@CustomLog
public abstract class ClassesRelocationPlugin implements Plugin<Project> {

    public static final String CLASSES_RELOCATION_EXTENSION_NAME = doNotInline("classesRelocation");
    public static final String CLASSES_RELOCATION_CONFIGURATION_NAME = doNotInline("classesRelocation");
    public static final String RELOCATED_JAR_TASK_NAME = doNotInline("relocatedJar");

    @Override
    public void apply(Project project) {
        val extension = project.getExtensions().create(
            CLASSES_RELOCATION_EXTENSION_NAME,
            ClassesRelocationExtension.class
        );

        val configurationProvider = project.getConfigurations().register(
            CLASSES_RELOCATION_CONFIGURATION_NAME,
            conf -> {
                conf.setCanBeResolved(true);
                conf.setDescription("Dependencies for classes relocation");
            }
        );

        project.getPluginManager().withPlugin(
            "java-base",
            __ -> configureJavaProject(project, extension, configurationProvider)
        );
    }

    private void configureJavaProject(
        Project project,
        ClassesRelocationExtension extension,
        NamedDomainObjectProvider<Configuration> relocationConfProvider
    ) {
        logger.warn("{}", extension);

        setLibraryElementToJar(project);
        extendCompileOnlyConfiguration(project, relocationConfProvider);

        val relocatedJarProvider = project.getTasks().register(
            RELOCATED_JAR_TASK_NAME,
            RelocatedJar.class,
            task -> {
                val jar = project.getTasks().withType(Jar.class).getByName(JAR_TASK_NAME);
                task.dependsOn(jar);

                task.getJarFile().set(jar.getArchiveFile());
                task.getRelocationClasspath().from(relocationConfProvider);

                task.setDescription(format(
                    "Relocate classes from dependencies of `%s` configuration in a JAR file created by %s task.",
                    relocationConfProvider.getName(),
                    jar.getPath()
                ));
                task.setGroup(jar.getGroup());
            }
        );
        project.getTasks().withType(Test.class).configureEach(test -> test.shouldRunAfter(relocatedJarProvider));
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
        sourceSets
            .configureEach(sourceSet -> {
                val configurationNames = asList(
                    sourceSet.getCompileClasspathConfigurationName(),
                    sourceSet.getRuntimeClasspathConfigurationName()
                );
                project.getConfigurations()
                    .matching(conf -> configurationNames.contains(conf.getName()))
                    .configureEach(conf -> conf.attributes(attrs ->
                        attrs.attribute(
                            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                            project.getObjects().named(LibraryElements.class, LibraryElements.JAR)
                        )
                    ));
            });
    }

}
