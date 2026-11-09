package io.github.foundry4j.forge.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/// JPA converter that stores a string encrypted with AES-256-GCM.
///
/// ```java
/// @Convert(converter = EncryptedStringConverter.class)
/// @Column(name = "id_card")
/// private String idCard;
/// ```
///
/// The column must be wide enough for Base64: the stored length is roughly
/// `4 * (plainUtf8Length + 28) / 3`.
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : CryptoServiceHolder.service().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CryptoServiceHolder.service().decrypt(dbData);
    }
}
