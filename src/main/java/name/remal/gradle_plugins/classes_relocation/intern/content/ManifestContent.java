package name.remal.gradle_plugins.classes_relocation.intern.content;

import java.io.ByteArrayInputStream;
import java.util.jar.Manifest;
import lombok.val;
import org.apache.commons.io.output.ByteArrayOutputStream;

public class ManifestContent implements MutableContentType<Manifest> {

    @Override
    public Class<Manifest> getType() {
        return Manifest.class;
    }

    @Override
    public Manifest fromBytes(byte[] bytes) throws Exception {
        return new Manifest(new ByteArrayInputStream(bytes));
    }

    @Override
    public byte[] toBytes(Manifest manifest) throws Exception {
        val out = new ByteArrayOutputStream();
        manifest.write(out);
        return out.toByteArray();
    }

}
