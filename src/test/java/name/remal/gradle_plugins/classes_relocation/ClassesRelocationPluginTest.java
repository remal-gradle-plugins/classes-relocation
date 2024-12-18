package name.remal.gradle_plugins.classes_relocation;

import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectValidations.executeAfterEvaluateActions;
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME;

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
    void testDependencies() {
        val testSourceSet = project.getExtensions().getByType(SourceSetContainer.class).getByName(TEST_SOURCE_SET_NAME);
        testSourceSet.getCompileClasspath().getFiles().forEach(file -> logger.warn("compileClasspath={}", file));
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
