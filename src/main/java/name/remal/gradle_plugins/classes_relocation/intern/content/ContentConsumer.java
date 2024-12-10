package name.remal.gradle_plugins.classes_relocation.intern.content;

@FunctionalInterface
public interface ContentConsumer<T> {

    void accept(T content) throws Exception;

}
