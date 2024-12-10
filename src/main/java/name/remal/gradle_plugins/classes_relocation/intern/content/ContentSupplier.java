package name.remal.gradle_plugins.classes_relocation.intern.content;

@FunctionalInterface
public interface ContentSupplier<T> {

    T get() throws Exception;

}
