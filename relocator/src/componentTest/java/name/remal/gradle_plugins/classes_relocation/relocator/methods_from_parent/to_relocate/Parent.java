package name.remal.gradle_plugins.classes_relocation.relocator.methods_from_parent.to_relocate;

public class Parent {

    protected void parentMethod() {
        throw new ExpectedException();
    }

}
