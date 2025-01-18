package name.remal.gradle_plugins.classes_relocation.relocator.guava_immutable_list;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import name.remal.gradle_plugins.classes_relocation.relocator.ClassesRelocatorTestLogic;

@SuppressWarnings("java:S4738")
public class GuavaImmutableListTestLogic implements ClassesRelocatorTestLogic {

    @Override
    public void assertTestLogic() {
        assertThat(ImmutableList.of("a", "b", "c"))
            .containsExactly("a", "b", "c");
    }

}
