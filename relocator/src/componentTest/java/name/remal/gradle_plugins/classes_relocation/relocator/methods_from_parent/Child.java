package name.remal.gradle_plugins.classes_relocation.relocator.methods_from_parent;

import name.remal.gradle_plugins.classes_relocation.relocator.methods_from_parent.to_relocate.Parent;

public class Child extends Parent {

    public void childMethod() {
        parentMethod();
    }

}
