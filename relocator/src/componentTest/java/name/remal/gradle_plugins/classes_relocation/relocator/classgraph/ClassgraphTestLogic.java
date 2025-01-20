package name.remal.gradle_plugins.classes_relocation.relocator.classgraph;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import java.io.ByteArrayOutputStream;
import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;

public class ClassgraphTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() throws Throwable {
        try (
            var scanResult = new ClassGraph()
                .overrideClassLoaders(ClassgraphTestLogic.class.getClassLoader())
                .acceptPathsNonRecursive("META-INF")
                .scan()
        ) {
            var metaInfResources = scanResult.getAllResources();
            var metaInfResourcePaths = metaInfResources.stream()
                .map(Resource::getPath)
                .collect(toUnmodifiableList());
            assertThat(metaInfResourcePaths)
                .isNotEmpty()
                .contains("META-INF/MANIFEST.MF");

            try (
                var firstResource = metaInfResources.stream()
                    .filter(resource -> resource.getPath().equals("META-INF/MANIFEST.MF"))
                    .findFirst()
                    .orElseThrow()
            ) {
                var out = new ByteArrayOutputStream();
                try (var in = firstResource.open()) {
                    in.transferTo(out);
                }
                var bytes = out.toByteArray();
                assertThat(bytes).isNotEmpty();
            }
        }
    }

}
