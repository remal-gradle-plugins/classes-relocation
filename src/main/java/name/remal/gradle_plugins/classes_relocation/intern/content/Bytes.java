package name.remal.gradle_plugins.classes_relocation.intern.content;

public class Bytes {

    private volatile byte[] data;

    Bytes(byte[] bytes) {
        this.data = bytes;
    }

    public void set(byte[] bytes) {
        this.data = bytes;
    }

    public byte[] get() {
        return this.data;
    }

}
