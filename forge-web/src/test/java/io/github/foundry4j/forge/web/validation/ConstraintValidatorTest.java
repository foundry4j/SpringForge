package io.github.foundry4j.forge.web.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/// Tests for the custom constraint validators.
class ConstraintValidatorTest {

    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @Phone
    private String optionalPhone;

    @Phone(allowBlank = false)
    private String requiredPhone;

    @EnumValue(Status.class)
    private String status;

    @EnumValue(value = Status.class, ignoreCase = true, allowNull = false)
    private String requiredStatus;

    @Test
    void validatesChineseMobileNumbers() throws NoSuchFieldException {
        var validator = new PhoneValidator();
        validator.initialize(annotation("optionalPhone", Phone.class));

        assertThat(validator.isValid("13800138000", context)).isTrue();
        assertThat(validator.isValid("12800138000", context)).isFalse();
        assertThat(validator.isValid(null, context)).isTrue();
        assertThat(validator.isValid(" ", context)).isTrue();

        validator.initialize(annotation("requiredPhone", Phone.class));
        assertThat(validator.isValid(null, context)).isFalse();
    }

    @Test
    void validatesEnumNames() throws NoSuchFieldException {
        var validator = new EnumValueValidator();
        validator.initialize(annotation("status", EnumValue.class));

        assertThat(validator.isValid("CREATED", context)).isTrue();
        assertThat(validator.isValid("created", context)).isTrue();
        assertThat(validator.isValid("UNKNOWN", context)).isFalse();
        assertThat(validator.isValid(null, context)).isTrue();

        validator.initialize(annotation("requiredStatus", EnumValue.class));
        assertThat(validator.isValid(null, context)).isFalse();
    }

    private <A extends Annotation> A annotation(String field, Class<A> type)
            throws NoSuchFieldException {
        var declared = ConstraintValidatorTest.class.getDeclaredField(field).getAnnotation(type);
        assertThat(declared).isNotNull();
        return declared;
    }

    /// Status used by the enum value validator tests.
    enum Status {
        CREATED,
        PAID
    }
}
