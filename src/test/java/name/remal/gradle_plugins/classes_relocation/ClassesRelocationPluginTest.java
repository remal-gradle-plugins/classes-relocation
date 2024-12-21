package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.classes_relocation.ClassesRelocationPlugin.RELOCATED_JAR_TASK_NAME;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectValidations.executeAfterEvaluateActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.val;
import name.remal.gradle_plugins.toolkit.testkit.TaskValidations;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
@CustomLog
class ClassesRelocationPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(ClassesRelocationPlugin.class);
        project.getPluginManager().apply("java");
    }

    @Test
    void sourceSetsClasspaths() {
        val sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        val mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME);
        val relocateJar = project.getTasks().withType(RelocateJar.class).getByName(RELOCATED_JAR_TASK_NAME);
        for (val sourceSet : sourceSets) {
            val compileClasspathAssertion = assertThat(sourceSet.getCompileClasspath().getFiles())
                .as("%s: %s", sourceSet, "compileClasspath")
                .doesNotContainAnyElementsOf(mainSourceSet.getOutput().getFiles());

            val runtimeClasspathAssertion = assertThat(sourceSet.getRuntimeClasspath().getFiles())
                .as("%s: %s", sourceSet, "runtimeClasspath")
                .doesNotContainAnyElementsOf(mainSourceSet.getOutput().getFiles());

            if (sourceSet.getName().equals(MAIN_SOURCE_SET_NAME)) {
                // relocated JAR can't be a part of classpaths for the `main` source set
                // do nothing
            } else {
                compileClasspathAssertion.contains(relocateJar.getTargetJarFile().get().getAsFile());
                runtimeClasspathAssertion.contains(relocateJar.getTargetJarFile().get().getAsFile());
            }
        }
    }

    @Test
    void pluginTasksDoNotHavePropertyProblems() {
        executeAfterEvaluateActions(project);

        val taskClassNamePrefix = packageNameOf(ClassesRelocationPlugin.class) + '.';
        project.getTasks().stream()
            .filter(task -> {
                val taskClass = unwrapGeneratedSubclass(task.getClass());
                return taskClass.getName().startsWith(taskClassNamePrefix);
            })
            .map(TaskValidations::markTaskDependenciesAsSkipped)
            .forEach(TaskValidations::assertNoTaskPropertiesProblems);
    }

}
