package name.remal.gradle_plugins.classes_relocation.intern.guava;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import name.remal.gradle_plugins.classes_relocation.intern.ClassesRelocatorTestLogic;

public class GuavaTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() {
        assertThat(ImmutableList.of("a", "b", "c"))
            .containsExactly("a", "b", "c");
    }

}
