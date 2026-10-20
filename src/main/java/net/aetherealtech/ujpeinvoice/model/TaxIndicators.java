package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.ProvisionalSpec;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The tax-indicator catalogue as a dated snapshot, because the list changed three times between
 * July and October 2026 and a closed enum would go stale with the next changelog entry. A caller
 * may still name an indicator the snapshot does not know, by constructing a {@link TaxIndicator}
 * and stating its impact.
 *
 * <p>The snapshot is the sales table of efakturawiki.ujp.gov.mk/danocni-indikatori, retrieved
 * 2026-10-19 (the page's last edit is 2026-08-27; the 02.10 changelog lists the same codes as
 * new). The page carries a code, a name, a note, a description and the rate group. It carries no
 * {@code vatImpact}, so the impact of each entry is this library's reading of its name, against the
 * four descriptions in the API description's "Валидација на даночни индикатори" and the gateway's
 * own examples, which settle only {@code DDV-G} (NULA) and {@code DDV-A} (STANDARD). Which impact a
 * code has matters only for STANDARD against the rest (VAT calculated or not) and for PRENESEN
 * (VAT shown in the per-indicator totals only); NULA against OSLOBODEN is a label.
 *
 * <p>Two rows are not byte for byte as the page prints them. The page prints
 * {@code DDV-10-14-4-6} twice, the second for article 14 paragraph 6; the changelog's list of new
 * indicators names {@code DDV-10-14-6}, so the second row is stored under that code. Four codes
 * contain a Cyrillic letter on the page ({@code DDV-11-A-О}, {@code DDV-PNPI-DS-А},
 * {@code DDV-PNPI-А}, {@code DDV-M-ТО}) and are stored exactly so, with the letter escaped in this
 * source: a lookup does not normalise it. The nine purchase-side indicators (DDV-22 to DDV-36) are
 * not part of the snapshot.
 */
@ProvisionalSpec("vatImpact of every entry except DDV-A, DDV-V, DDV-B and DDV-G is the library's reading "
        + "of the indicator's name: the /danocni-indikatori page has no vatImpact column")
public final class TaxIndicators {

    /** The rows of the page: code, impact, rate group letter (or - when several), note. */
    private static final String ROWS = """
            DDV-G|NULA|G|ДДВ не е пресметан согласно член 51 став 3 од ЗДДВ
            DDV-A|STANDARD|A|
            DDV-V|STANDARD|V|
            DDV-B|STANDARD|B|
            DDV-7-I|NULA|G|ДДВ не е пресметан согласно член 24 став 1 точка 1 од ЗДДВ
            DDV-8-2|NULA|G|ДДВ не е пресметан согласно член 24 став 1 точка 2 од ЗДДВ
            DDV-8-3|NULA|G|ДДВ не е пресметан согласно член 24 став 1 точка 3 од ЗДДВ
            DDV-8-4|NULA|G|ДДВ не е пресметан согласно член 24 став 1 точка 4 од ЗДДВ
            DDV-8-5|NULA|G|ДДВ не е пресметан согласно член 24 став 1 точка 5 од ЗДДВ
            DDV-8-6|NULA|G|ДДВ не е пресметан согласно член 24 став 1 точка 6 од ЗДДВ
            DDV-8-7|NULA|G|ДДВ не е пресметан согласно член 24 став 1 точка 7 од ЗДДВ
            DDV-8-8|NULA|G|ДДВ не е пресметан согласно член 24 став 1 точка 8 од ЗДДВ
            DDV-8-9|NULA|G|ДДВ не е пресметан согласно член 24 став 1 точка 9 од ЗДДВ
            DDV-8-10|NULA|G|ДДВ не е пресметан согласно член 24 став 1 точка 10 од ЗДДВ
            DDV-8-P|NULA|G|ДДВ не е пресметан согласно член 24-а од ЗДДВ
            DDV-8-ZDSJD|NULA|G|ДДВ не е пресметан согласно член 15-б од ЗДСЈД
            DDV-8-DR|NULA|G|ДДВ не е пресметан согласно друг пропис
            DDV-8-Z|NULA|G|ДДВ не е пресметан согласно член 38-б од ЗДДВ
            DDV-9-1|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 1 од ЗДДВ
            DDV-9-2|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 2 од ЗДДВ
            DDV-9-3|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 3 од ЗДДВ
            DDV-9-4|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 4 од ЗДДВ
            DDV-9-5|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 5 од ЗДДВ
            DDV-9-6|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 6 од ЗДДВ
            DDV-9-7|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 7 од ЗДДВ
            DDV-9-8|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 8 од ЗДДВ
            DDV-9-8-a|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 8-a од ЗДДВ
            DDV-9-9|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 9 од ЗДДВ
            DDV-9-10|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 10 од ЗДДВ
            DDV-9-11|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 11 од ЗДДВ
            DDV-9-12|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 12 од ЗДДВ
            DDV-9-13|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 13 од ЗДДВ
            DDV-9-14|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 14 од ЗДДВ
            DDV-9-15|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 15 од ЗДДВ
            DDV-9-16|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 16 од ЗДДВ
            DDV-9-17|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 17 од ЗДДВ
            DDV-9-17-a|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 17-a од ЗДДВ
            DDV-9-17-b|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 17-б од ЗДДВ
            DDV-9-18|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 18 од ЗДДВ
            DDV-9-19|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 19 од ЗДДВ
            DDV-9-19-a|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 19-а од ЗДДВ
            DDV-9-19-b|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 19-б од ЗДДВ
            DDV-9-20|OSLOBODEN|G|ДДВ не е пресметан согласно член 23 точка 20 од ЗДДВ
            DDV-9-DR|OSLOBODEN|G|ДДВ не е пресметан согласно друг пропис
            DDV-10-13-1-1|OSLOBODEN|G|Прометот на добра не е предмет на оданочување во земјата согласно член 2 и 13 став 1 точка 1 од ЗДДВ
            DDV-10-13-1-2|OSLOBODEN|G|Прометот на добра не е предмет на оданочување во земјата согласно член 2 и 13 став 1 точка 2 од ЗДДВ
            DDV-10-13-1-3|OSLOBODEN|G|Прометот на добра не е предмет на оданочување во земјата согласно член 2 и 13 став 1 точка 3 од ЗДДВ
            DDV-10-13-1-4|OSLOBODEN|G|Прометот на добра не е предмет на оданочување во земјата согласно член 2 и 13 став 1 точка 4 од ЗДДВ
            DDV-10-13-1-5|OSLOBODEN|G|Прометот на добра не е предмет на оданочување во земјата согласно член 2 и 13 став 1 точка 5 од ЗДДВ
            DDV-10-13-3|OSLOBODEN|G|Прометот на добра не е предмет на оданочување во земјата согласно член 2 и 13 став 3 од ЗДДВ
            DDV-10-14-2|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 2 од ЗДДВ
            DDV-10-14-4-1|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 1 од ЗДДВ
            DDV-10-14-4-2|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 2 од ЗДДВ
            DDV-10-14-4-3|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 3 од ЗДДВ
            DDV-10-14-4-4|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 4 од ЗДДВ
            DDV-10-14-4-5|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 5 од ЗДДВ
            DDV-10-14-4-6|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 6 од ЗДДВ
            DDV-10-14-4-7|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 7 од ЗДДВ
            DDV-10-14-4-8|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 8 од ЗДДВ
            DDV-10-14-4-9|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 9 од ЗДДВ
            DDV-10-14-4-10|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 10 од ЗДДВ
            DDV-10-14-4-11|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 4 точка 11 од ЗДДВ
            DDV-10-14-6|OSLOBODEN|G|Прометот не е предмет на оданочување во земјата согласно член 2 и 14 став 6 од ЗДДВ
            DDV-11-A-G|PRENESEN|A|Пренесување на даночна обврска согласно член 32-а став 1 точка 1 од ЗДДВ
            DDV-11-A-\u041e|PRENESEN|A|Пренесување на даночна обврска согласно член 32-а став 1 точка 2 од ЗДДВ
            DDV-PNPI-DS-\u0410|PRENESEN|A|Пренесување на даночна обврска согласно член 32-а став 1 точка 3 од ЗДДВ
            DDV-PNPI-DS-V|PRENESEN|V|Пренесување на даночна обврска согласно член 32-а став 1 точка 3 од ЗДДВ
            DDV-PNPI-DS-B|PRENESEN|B|Пренесување на даночна обврска согласно член 32-а став 1 точка 3 од ЗДДВ
            DDV-PNPI-\u0410|STANDARD|A|Промет во постапка на присилна наплата или присилно извршување согласно член 43-а од ЗДДВ
            DDV-PNPI-V|STANDARD|V|Промет во постапка на присилна наплата или присилно извршување согласно член 43-а од ЗДДВ
            DDV-PNPI-B|STANDARD|B|Промет во постапка на присилна наплата или присилно извршување согласно член 43-а од ЗДДВ
            DDV-12-4|PRENESEN|A|Данок кој се должи член 32 точка 4 од ЗДДВ
            DDV-14-4|PRENESEN|-|Данок кој се должи член 32 точка 4 од ЗДДВ
            DDV-12-5|PRENESEN|A|Данок кој се должи член 32 точка 5 од ЗДДВ
            DDV-14-5|PRENESEN|-|Данок кој се должи член 32 точка 5 од ЗДДВ
            DDV-uvoz|PRENESEN|-|Данок кој се должи согласно член 32 точка 2 од ЗДДВ
            DDV-M-\u0422\u041e|MARGINALNA|A|Маргинална шема за оданочување на тур-оператори
            DDV-M-18|MARGINALNA|A|Маргинална шема за оданочување на употребувани добра
            DDV-M-5|MARGINALNA|B|Маргинална шема за оданочување на употребувани добра
            DDV-PS|OSLOBODEN|G|Проодна ставка согласно член 18 точка 3 од ЗДДВ
            DDV-18-BN|STANDARD|A|
            DDV-10-BN|STANDARD|V|
            DDV-5-BN|STANDARD|B|
            DDV-vaucer-E|STANDARD|-|Еднонаменски вредносен ваучер
            DDV-vaucer-P|OSLOBODEN|G|Повеќе наменски вредносен ваучер - Не се пресметува ДДВ
            ZDSJD-18|STANDARD|A|Промет кој подлежи на даночни потикнувања согласно Законот за донации и за спонзорства во јавните дејности.
            ZDSJD-10|STANDARD|V|Промет кој подлежи на даночни потикнувања согласно Законот за донации и за спонзорства во јавните дејности.
            ZDSJD-5|STANDARD|B|Промет кој подлежи на даночни потикнувања согласно Законот за донации и за спонзорства во јавните дејности.
            DDV-14-3-18|STANDARD|A|Промет кој подлежи на оданочување со ДДВ согласно член 14 став 3 од ЗДДВ
            DDV-14-3-5|STANDARD|B|Промет кој подлежи на оданочување со ДДВ согласно член 14 став 3 од ЗДДВ
            """;

    // Named for the page's last edit, which is the version of the catalogue it holds; retrievedOn is when it was read.
    public static final Snapshot SNAPSHOT_2026_08_27 = new Snapshot(LocalDate.of(2026, 10, 19), parse());

    private TaxIndicators() {
    }

    /** An immutable set of indicators looked up by code, with the date it was taken. */
    public record Snapshot(LocalDate retrievedOn, Map<String, TaxIndicator> byCode) {

        public Snapshot {
            byCode = Map.copyOf(byCode);
        }

        public Optional<TaxIndicator> find(String code) {
            return Optional.ofNullable(byCode.get(code));
        }

        /** The indicator with this code; refuses by name when the snapshot does not know it. */
        public TaxIndicator get(String code) {
            return find(code).orElseThrow(() -> new DocumentValidationException("UJP07-19 taxIndicator " + code
                    + " is not in the snapshot of " + retrievedOn + "; state its vatImpact explicitly"));
        }

        public Collection<TaxIndicator> all() {
            return byCode.values();
        }
    }

    private static Map<String, TaxIndicator> parse() {
        Map<String, TaxIndicator> indicators = new LinkedHashMap<>();
        for (String row : ROWS.strip().split("\n")) {
            String[] columns = row.strip().split("\\|", 4);
            VatGroup group = columns[2].equals("-") ? null : VatGroup.fromCode("DDV-" + columns[2]);
            String note = columns[3].isBlank() ? null : columns[3];
            indicators.put(columns[0], new TaxIndicator(columns[0], VatImpact.valueOf(columns[1]), group, note));
        }
        return indicators;
    }
}
