package name.remal.gradle_plugins.classes_relocation.intern.context;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.WillNotClose;

public interface RelocationDestination {

    void write(byte[] bytes) throws IOException;

    void copy(@WillNotClose InputStream inputStream) throws IOException;

}
