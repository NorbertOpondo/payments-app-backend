package com.app.payments.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingUtilsTest {

    // ── maskPhone ──────────────────────────────────────────────────────────────

    @Test
    void maskPhone_null_returnsThreeStars() {
        assertThat(MaskingUtils.maskPhone(null)).isEqualTo("***");
    }

    @Test
    void maskPhone_lessThanOrEqualThreeChars_returnsThreeStars() {
        assertThat(MaskingUtils.maskPhone("12")).isEqualTo("***");
        assertThat(MaskingUtils.maskPhone("123")).isEqualTo("***");
    }

    @Test
    void maskPhone_preservesFirstThreeCharsAndMasksRest() {
        String phone = "+254712345678";
        String masked = MaskingUtils.maskPhone(phone);

        assertThat(masked).startsWith("+25");
        assertThat(masked).hasSize(phone.length());
        assertThat(masked.substring(3)).matches("\\*+");
    }

    @Test
    void maskPhone_doesNotLeakSensitiveDigits() {
        String masked = MaskingUtils.maskPhone("+254712345678");
        assertThat(masked).doesNotContain("712345678");
    }

    // ── generateReceiptNumber ─────────────────────────────────────────────────

    @Test
    void generateReceiptNumber_hasCorrectPrefix() {
        assertThat(MaskingUtils.generateReceiptNumber("CRD")).startsWith("CRD");
        assertThat(MaskingUtils.generateReceiptNumber("LGR")).startsWith("LGR");
    }

    @Test
    void generateReceiptNumber_hasCorrectTotalLength() {
        // prefix (3) + 7 random chars = 10
        assertThat(MaskingUtils.generateReceiptNumber("CRD")).hasSize(10);
        assertThat(MaskingUtils.generateReceiptNumber("LGR")).hasSize(10);
    }

    @Test
    void generateReceiptNumber_containsOnlyUppercaseAlphanumeric() {
        String receipt = MaskingUtils.generateReceiptNumber("LGR");
        assertThat(receipt).matches("[A-Z0-9]+");
    }

    @Test
    void generateReceiptNumber_isNotNullOrBlank() {
        assertThat(MaskingUtils.generateReceiptNumber("CRD")).isNotBlank();
    }
}
