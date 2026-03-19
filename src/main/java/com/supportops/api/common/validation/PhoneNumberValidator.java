package com.supportops.api.common.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String normalized = value.trim();
        if (!normalized.startsWith("+")) {
            return false;
        }

        try {
            var parsedNumber = PHONE_NUMBER_UTIL.parse(normalized, null);
            return PHONE_NUMBER_UTIL.isValidNumber(parsedNumber);
        } catch (NumberParseException ex) {
            return false;
        }
    }
}
