package name.remal.gradle_plugins.classes_relocation.intern.content;

public class BytesContent implements MutableContentType<Bytes> {

    @Override
    public Class<Bytes> getType() {
        return Bytes.class;
    }

    @Override
    public Bytes fromBytes(byte[] bytes) {
        return new Bytes(bytes);
    }

    @Override
    public byte[] toBytes(Bytes bytes) {
        return bytes.get();
    }

}
