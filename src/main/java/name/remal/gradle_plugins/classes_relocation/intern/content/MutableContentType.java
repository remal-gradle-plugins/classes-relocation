package name.remal.gradle_plugins.classes_relocation.intern.content;

public interface MutableContentType<T> {

    Class<T> getType();

    T fromBytes(byte[] bytes) throws Exception;

    byte[] toBytes(T content) throws Exception;

}
