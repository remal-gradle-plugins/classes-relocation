package name.remal.gradle_plugins.classes_relocation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.RequiredArgsConstructor;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class ClassesRelocationPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(ClassesRelocationPlugin.class);
    }

    @Test
    void test() {
        assertTrue(project.getPlugins().hasPlugin(ClassesRelocationPlugin.class));
    }

}
