package net.aetherealtech.ujpeinvoice.model;

/**
 * The type of a referenced document (UJP02-01-01/-02): a code and its name from the gateway's code
 * list, as of its 16.10.2026 revision. The list is served by the gateway and grows, so any pair is
 * accepted; the known ones are constants, with the eleven added on 15.10 last. {@link #ST} is the
 * reference a storno or a correction makes to the document it voids or replaces; the 2026-04-24 code
 * list predates it and the 16.10 type pages carry it.
 */
public record RefDocumentType(String code, String name) {

    public static final RefDocumentType DOG = new RefDocumentType("DOG", "Договор");
    public static final RefDocumentType JN = new RefDocumentType("JN", "Документ за јавна набавка");
    public static final RefDocumentType C_D = new RefDocumentType("C_D", "Царинска декларација");
    public static final RefDocumentType C_R = new RefDocumentType("C_R", "Решение од Царинска управа");
    public static final RefDocumentType GRD = new RefDocumentType("GRD", "Градежни документи");
    public static final RefDocumentType TRN = new RefDocumentType("TRN", "Транспортни документи");
    public static final RefDocumentType PRO = new RefDocumentType("PRO", "Профактура");
    public static final RefDocumentType KUP = new RefDocumentType("KUP", "Референца на купувач");
    public static final RefDocumentType NAR = new RefDocumentType("NAR", "Нарачка");
    public static final RefDocumentType PON = new RefDocumentType("PON", "Понуда");
    public static final RefDocumentType FIS = new RefDocumentType("FIS", "Фискална сметка");
    public static final RefDocumentType DOC = new RefDocumentType("DOC", "Други документи");
    public static final RefDocumentType ST = new RefDocumentType("ST", "Сторно/Корекција");
    public static final RefDocumentType PRJ = new RefDocumentType("PRJ", "Проект");
    public static final RefDocumentType REZ = new RefDocumentType("REZ", "Документ за проценка за цели на инвестициско вложување во објект на закуподавач");
    public static final RefDocumentType DOC_SEP = new RefDocumentType("DOC_SEP", "Документ за СЕП проект");
    public static final RefDocumentType IDU = new RefDocumentType("IDU", "Испорака на доброто/услугата");
    public static final RefDocumentType SYS = new RefDocumentType("SYS", "Од систем");
    public static final RefDocumentType PRE = new RefDocumentType("PRE", "Пресметка од странско лице");
    public static final RefDocumentType RMP = new RefDocumentType("RMP", "Решение од Министерство за Правда");
    public static final RefDocumentType ZPPUJN = new RefDocumentType("ZPPUJN", "Записник за продажба на предмети со усно јавно надавање");
    public static final RefDocumentType PPPNS = new RefDocumentType("PPPNS", "Договор за продажба на подвижни предмети со непосредна спогодба");
    public static final RefDocumentType ZNED = new RefDocumentType("ZNED", "Заклучок за намирување на еден доверител");
    public static final RefDocumentType ZNPD = new RefDocumentType("ZNPD", "Заклучок за намирување на повеќе доверители");
    public static final RefDocumentType PPPV = new RefDocumentType("PPPV", "Потврда за предавање на подвижни предмети во владение");
    public static final RefDocumentType ZPNUJN = new RefDocumentType("ZPNUJN", "Засписник за продажба на недвижност со усно јавно наддавање");
    public static final RefDocumentType PNNS = new RefDocumentType("PNNS", "Договор за продажба на недвижност со непосредна спогодба");
    public static final RefDocumentType DPPN = new RefDocumentType("DPPN", "Записник за делба на износот простигнат со продажба на недвижност");
    public static final RefDocumentType RPNKV = new RefDocumentType("RPNKV", "Решение за предавање на надвижноста на купувачот во владение");
    public static final RefDocumentType PIP = new RefDocumentType("PIP", "Потврда за извршена продажба");

    public RefDocumentType {
        code = Check.required(code, "UJP02-01-01", "referenceType.code", 20);
        name = Check.required(name, "UJP02-01-02", "referenceType.name", 80);
    }
}
