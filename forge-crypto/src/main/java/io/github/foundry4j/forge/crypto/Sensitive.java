package io.github.foundry4j.forge.crypto;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import tools.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Masks a field when it is serialized to JSON.
///
/// ```java
/// public record UserView(String name, @Sensitive(SensitiveType.PHONE) String phone) { }
/// ```
///
/// The annotation only changes serialization; the value in the entity and in the
/// database stays untouched. Combine it with
/// [EncryptedStringConverter] when the column itself must be encrypted.
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface Sensitive {

    /// Masking strategy.
    SensitiveType value() default SensitiveType.PASSWORD;
}
