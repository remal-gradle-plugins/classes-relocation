package name.remal.gradle_plugins.classes_relocation.intern.content;

import static name.remal.gradle_plugins.toolkit.SneakyThrowUtils.sneakyThrow;
import static name.remal.gradle_plugins.toolkit.ThrowableUtils.unwrapReflectionException;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.makeAccessible;

import com.google.common.io.ByteStreams;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import name.remal.gradle_plugins.classes_relocation.intern.context.RelocationDestination;
import name.remal.gradle_plugins.toolkit.LazyValue;

public class Content {

    public static Content contentForInputStreamSupplier(ContentSupplier<InputStream> inputStreamSupplier) {
        return contentForBytesSupplier(() -> {
            val inputStream = inputStreamSupplier.get();
            return ByteStreams.toByteArray(inputStream);
        });
    }

    public static Content contentForBytesSupplier(ContentSupplier<byte[]> bytesSupplier) {
        return new Content(bytesSupplier);
    }

    public static <T> Content asContent(
        Class<? extends MutableContentType<T>> contentTypeClass,
        T initialContent
    ) {
        return new Content(contentTypeClass, initialContent);
    }


    @Value
    private static class ContentAndType {
        Object content;
        MutableContentType<Object> contentType;
    }

    private static final ContentAndType NOT_INITIALIZED = new ContentAndType(
        new Object[0],
        new NotInitializedContentType()
    );

    private static final ContentAndType DISPOSED = new ContentAndType(
        new Object[0],
        new NotInitializedContentType()
    );


    private ContentAndType current;

    private final LazyValue<byte[]> initialContent;

    private Content(ContentSupplier<byte[]> initialContentSupplier) {
        this.current = NOT_INITIALIZED;
        this.initialContent = LazyValue.of(initialContentSupplier::get);
    }

    private <T> Content(
        Class<? extends MutableContentType<T>> contentTypeClass,
        T initialContent
    ) {
        this.current = new ContentAndType(initialContent, contentTypeOf(contentTypeClass));
        this.initialContent = LazyValue.of(() -> {
            throw new UnsupportedOperationException("Content was provided at the initialization");
        });
    }


    @SneakyThrows
    @SuppressWarnings("unchecked")
    public synchronized <T> T as(Class<? extends MutableContentType<T>> contentTypeClass) {
        if (current == DISPOSED) {
            throw new IllegalStateException("Already disposed");
        }

        if (current == NOT_INITIALIZED) {
            val contentType = contentTypeOf(contentTypeClass);
            val bytes = initialContent.get();
            val content = contentType.fromBytes(bytes);
            current = new ContentAndType(content, contentType);
            return (T) content;
        }

        val currentContent = current.content;
        val currentContentType = current.contentType;
        if (currentContentType.getClass() == contentTypeClass) {
            return (T) currentContent;

        } else {
            val contentType = contentTypeOf(contentTypeClass);
            if (contentType.getType().isInstance(currentContent)) {
                return (T) currentContent;
            }

            val bytes = currentContentType.toBytes(currentContent);
            val content = contentType.fromBytes(bytes);
            current = new ContentAndType(content, contentType);
            return (T) content;
        }
    }

    @SneakyThrows
    public synchronized <T> Content with(
        Class<? extends MutableContentType<T>> contentTypeClass,
        ContentConsumer<T> consumer
    ) {
        val content = as(contentTypeClass);
        consumer.accept(content);
        return this;
    }

    public synchronized void dispose() {
        current = DISPOSED;
    }

    @SneakyThrows
    public synchronized byte[] toByteArray() {
        if (current == DISPOSED) {
            throw new IllegalStateException("Already disposed");
        }

        try {
            if (current == NOT_INITIALIZED) {
                return initialContent.get();
            }

            val currentContent = current.content;
            val currentContentType = current.contentType;
            return currentContentType.toBytes(currentContent);

        } finally {
            dispose();
        }
    }

    @SneakyThrows
    public synchronized void writeTo(RelocationDestination destination) {
        val bytes = toByteArray();
        destination.write(bytes);
    }


    private static final ConcurrentMap<Class<?>, Object> CONTENT_TYPE_INSTANCES = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private static <T> MutableContentType<T> contentTypeOf(
        Class<? extends MutableContentType<?>> contentTypeClass
    ) {
        return (MutableContentType<T>) CONTENT_TYPE_INSTANCES.computeIfAbsent(contentTypeClass, clazz -> {
            try {
                val ctor = clazz.getConstructor();
                makeAccessible(ctor);
                return ctor.newInstance();
            } catch (Exception e) {
                throw sneakyThrow(unwrapReflectionException(e));
            }
        });
    }


    private static class NotInitializedContentType implements MutableContentType<Object> {
        @Override
        public Class<Object> getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object fromBytes(byte[] bytes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] toBytes(Object content) {
            throw new UnsupportedOperationException();
        }
    }

}
