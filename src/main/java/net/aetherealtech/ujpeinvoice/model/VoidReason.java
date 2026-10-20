package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;

/**
 * Why a document is stornoed: a code and its name from the gateway's code list ("Причини за
 * сторнирање", sifrarnici of 24.04.2026). The list is served by the gateway, so any pair is
 * accepted; the known ones are constants.
 */
public record VoidReason(String code, String name) {

    public static final VoidReason S_1 = new VoidReason("S-1", "Погрешен купувач");
    public static final VoidReason S_2 = new VoidReason("S-2", "Поништување на договор");
    public static final VoidReason S_3 = new VoidReason("S-3", "Стоката е вратена во целост");
    public static final VoidReason S_4 = new VoidReason("S-4", "Двојно фактурирање на ист промет");
    /** "Друго": the code list asks for free text, so the storno's comment is required. */
    public static final VoidReason S_5 = new VoidReason("S-5", "Друго");

    public VoidReason {
        code = Check.required(code, "voidReason", "code", 10);
        name = Check.required(name, "voidReason", "name", 200);
    }

    @ProvisionalSpec("The code list says 'Друго (внес на слободен текст)'; that the comment is therefore mandatory "
            + "is the library's reading")
    boolean needsComment() {
        return this.equals(S_5);
    }
}
