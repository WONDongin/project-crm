package com.project.crm.common.util;

public final class PhoneNumberMasker {

    private PhoneNumberMasker() {
    }

    public static String maskMiddle(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return phoneNumber;
        }

        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.length() <= 7) {
            return "***";
        }

        int prefixLength = digits.startsWith("02") ? 2 : 3;
        String areaCode = digits.substring(0, Math.min(prefixLength, digits.length() - 4));
        String lastFourDigits = digits.substring(digits.length() - 4);

        return areaCode + "-***-" + lastFourDigits;
    }
}
