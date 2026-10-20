package net.aetherealtech.ujpeinvoice.model;

/**
 * How a document is paid (UJP10-01/-02): a code and its description from the gateway's code list
 * ("Шифрарник за начин на плаќање", 24.04.2026). The list is served by the gateway, so any pair is
 * accepted; the known ones are constants.
 */
public record PaymentType(String code, String description) {

    public static final PaymentType P_10 = new PaymentType("P10", "Плаќање во готово");
    public static final PaymentType P_11 = new PaymentType("P11", "Плаќање со картичка");
    public static final PaymentType P_12 = new PaymentType("P12", "Плаќање преку банка");
    public static final PaymentType P_13 = new PaymentType("P13", "Плаќање на рати");
    public static final PaymentType P_14 = new PaymentType("P14", "Онлајн банкарско плаќање");
    public static final PaymentType P_15 = new PaymentType("P15", "Плаќање преку мобилна апликација");
    public static final PaymentType P_16 = new PaymentType("P16", "Давање без надомест");
    public static final PaymentType P_17 = new PaymentType("P17", "Компензација");
    public static final PaymentType P_18 = new PaymentType("P18", "Плаќање со вредносен ваучер");
    public static final PaymentType P_19 = new PaymentType("P19", "Други платежни инструменти");

    public PaymentType {
        code = Check.required(code, "UJP10-01", "paymentType.code", 3);
        description = Check.required(description, "UJP10-02", "paymentType.description", 40);
    }
}
