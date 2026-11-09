package io.github.foundry4j.forge.crypto;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/// Serializes a string with the masking strategy declared by [Sensitive].
public class SensitiveSerializer extends ValueSerializer<String> {

    private final SensitiveType type;

    /// Creates a serializer without masking, used when the annotation is inherited.
    public SensitiveSerializer() {
        this(SensitiveType.NONE);
    }

    /// Creates a serializer with an explicit strategy.
    public SensitiveSerializer(SensitiveType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext context) {
        gen.writeString(Masking.mask(value, type));
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext context, BeanProperty property) {
        if (property == null) {
            return this;
        }
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        return annotation == null ? this : new SensitiveSerializer(annotation.value());
    }
}
