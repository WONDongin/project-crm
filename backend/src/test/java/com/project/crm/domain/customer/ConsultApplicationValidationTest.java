package com.project.crm.domain.customer;

import com.project.crm.domain.customer.dto.ConsultApplyRequestDto;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class ConsultApplicationValidationTest {
    @Test void rejectsValuesLongerThanDatabaseColumns() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validateValue(ConsultApplyRequestDto.class,"name","가".repeat(51))).isNotEmpty();
            assertThat(validator.validateValue(ConsultApplyRequestDto.class,"email","a".repeat(95)+"@test.com")).isNotEmpty();
            assertThat(validator.validateValue(ConsultApplyRequestDto.class,"interestCourse","a".repeat(31))).isNotEmpty();
            assertThat(validator.validateValue(ConsultApplyRequestDto.class,"consultType","a".repeat(21))).isNotEmpty();
            assertThat(validator.validateValue(ConsultApplyRequestDto.class,"preferredTime","a".repeat(21))).isNotEmpty();
            assertThat(validator.validateValue(ConsultApplyRequestDto.class,"leadSource","a".repeat(31))).isNotEmpty();
            assertThat(validator.validateValue(ConsultApplyRequestDto.class,"content","가".repeat(10001))).isNotEmpty();
        }
    }
    @Test void normalizesSupportedPhoneFormatsAndRejectsInvalidCharactersAndLength() {
        assertThat(CustomerProfileService.normalizePhone("010-1111-1111")).isEqualTo("01011111111");
        assertThat(CustomerProfileService.normalizePhone("010 1111 1111")).isEqualTo("01011111111");
        for (String value : new String[]{"abc01011111111", "010/1111/1111", "010\\1111\\1111", "12345678", "123456789012"}) {
            assertThatThrownBy(() -> CustomerProfileService.normalizePhone(value)).isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> CustomerProfileService.normalizePhone(null)).isInstanceOf(ResponseStatusException.class);
    }
}
