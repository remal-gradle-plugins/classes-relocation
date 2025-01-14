package name.remal.gradle_plugins.classes_relocation.relocator.api;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public class DelegateRelocationContext implements RelocationContext {

    @Delegate
    private final RelocationContext delegate;

}
