package name.remal.gradle_plugins.classes_relocation.relocator.relocated_constructors.to_relocate;

import java.util.concurrent.atomic.AtomicBoolean;

public class RelocatedConstructorsComponent {

    public final AtomicBoolean initialized = new AtomicBoolean(false);

    public RelocatedConstructorsComponent() {
        initialized.set(true);
    }

}
