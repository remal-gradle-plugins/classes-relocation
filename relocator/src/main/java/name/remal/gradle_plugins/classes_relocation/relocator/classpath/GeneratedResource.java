package name.remal.gradle_plugins.classes_relocation.relocator.classpath;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.val;
import name.remal.gradle_plugins.toolkit.LazyValue;
import org.jetbrains.annotations.Unmodifiable;

public final class GeneratedResource extends ResourceBase {

    public static GeneratedResourceBuilder builder() {
        return new GeneratedResourceBuilder();
    }

    public static GeneratedResource newGeneratedResource(Consumer<GeneratedResourceBuilder> configurer) {
        val builder = builder();
        configurer.accept(builder);
        return builder.build();
    }


    @Getter
    @Unmodifiable
    private final List<Resource> sourceResources;

    @Getter
    private final long lastModifiedMillis;

    private final LazyValue<byte[]> content;

    GeneratedResource(
        ImmutableList<Resource> sourceResources,
        String name,
        @Nullable Integer multiReleaseVersion,
        long lastModifiedMillis,
        LazyValue<byte[]> content
    ) {
        super(name, multiReleaseVersion);
        this.sourceResources = sourceResources;
        this.lastModifiedMillis = lastModifiedMillis;
        this.content = content;
    }

    @Override
    public InputStream open() {
        return new ByteArrayInputStream(content.get());
    }

    @Override
    public byte[] readAllBytes() {
        return content.get().clone();
    }

    @Nullable
    @Override
    public ClasspathElement getClasspathElement() {
        return null;
    }

}
