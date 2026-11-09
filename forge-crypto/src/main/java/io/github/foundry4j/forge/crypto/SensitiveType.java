package io.github.foundry4j.forge.crypto;

/// Masking strategy of [Sensitive] serialization.
public enum SensitiveType {

    /// No masking, handy for switching a field temporarily.
    NONE,

    /// Personal name, keeps the first character.
    NAME,

    /// Mobile number, keeps the first three and the last four digits.
    PHONE,

    /// Identity card number, keeps the first six and the last four characters.
    ID_CARD,

    /// Bank card number, keeps the last four digits.
    BANK_CARD,

    /// E-mail address, keeps the first character of the local part and the domain.
    EMAIL,

    /// Postal address, keeps the first six characters.
    ADDRESS,

    /// Secret, replaces every character with an asterisk.
    PASSWORD
}
