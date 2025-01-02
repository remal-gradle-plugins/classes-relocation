package name.remal.gradle_plugins.classes_relocation.relocator;

@FunctionalInterface
public interface ClassesRelocatorObjectFactory {

    <T> T create(Class<T> clazz) throws Throwable;

}
