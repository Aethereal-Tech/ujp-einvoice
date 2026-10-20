package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;

/**
 * Why a document corrects another: a code and its name from the gateway's code list ("Причини за
 * исправка", sifrarnici of 24.04.2026). The list is served by the gateway, so any pair is
 * accepted; the known ones are constants.
 */
public record CorrectionReason(String code, String name) {

    public static final CorrectionReason C01 = new CorrectionReason("C01", "Грешка во цена");
    public static final CorrectionReason C02 = new CorrectionReason("C02", "Грешка во количина");
    public static final CorrectionReason C03 = new CorrectionReason("C03", "Делумно враќање на стока");
    public static final CorrectionReason C04 = new CorrectionReason("C04", "Погрешен даночен индикатор");
    public static final CorrectionReason C05 = new CorrectionReason("C05", "Погрешна ДДВ стапка");
    public static final CorrectionReason C06 = new CorrectionReason("C06", "Погрешна даночна основица");
    public static final CorrectionReason C07 = new CorrectionReason("C07", "Исправка на опис");
    /** "Друго": the code list asks for free text, so the correction's comment is required. */
    public static final CorrectionReason C08 = new CorrectionReason("C08", "Друго");

    public CorrectionReason {
        code = Check.required(code, "correctionReason", "code", 10);
        name = Check.required(name, "correctionReason", "name", 200);
    }

    @ProvisionalSpec("The code list says 'Друго (внес на слободен текст)'; that the comment is therefore mandatory "
            + "is the library's reading")
    boolean needsComment() {
        return this.equals(C08);
    }
}
