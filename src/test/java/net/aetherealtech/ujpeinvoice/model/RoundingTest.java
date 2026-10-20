package net.aetherealtech.ujpeinvoice.model;

import net.aetherealtech.ujpeinvoice.internal.Money;
import net.aetherealtech.ujpeinvoice.testsupport.DocumentFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UJP's rule (/json-struktura): line values are not rounded, they are summed unrounded and the sums
 * are rounded once. Rounding each line first, as 0.2.0 did, gives other totals; the gateway
 * recomputes them and refuses a mismatch beyond 0.1.
 */
class RoundingTest {

    @Test
    void totalsSummedFromUnroundedLinesDifferFromTotalsOfRoundedLines() {
        // Three lines of 0.335 at 18%: each line's VAT is 0.0603, its gross 0.3953. Summed unrounded and
        // rounded once the gross is 1.19; rounded per line (0.40 each) it is 1.20.
        Document.Builder builder = DocumentFixtures.invoice();
        for (int i = 0; i < 3; i++) {
            builder.line(Line.builder().description("x").quantity("1").unitPrice("0.335").standardRate(VatGroup.DDV_A).build());
        }
        Document document = builder.build();

        BigDecimal roundedPerLine = document.effectiveLines().stream()
                .map(line -> Money.total(line.totalWithVat()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(document.totals().gross()).isEqualByComparingTo("1.19");
        assertThat(roundedPerLine).isEqualByComparingTo("1.20");
        assertThat(document.totals().gross()).isNotEqualByComparingTo(roundedPerLine);
    }

    @Test
    void theLibraryAlwaysSumsBeforeItRounds() {
        Random random = new Random(20261020L);
        boolean sawADifference = false;
        for (int round = 0; round < 500; round++) {
            Document.Builder builder = DocumentFixtures.invoice();
            List<Line> lines = new ArrayList<>();
            for (int i = 0; i < 1 + random.nextInt(6); i++) {
                VatGroup group = VatGroup.values()[random.nextInt(3)];
                Line line = Line.builder().description("x")
                        .quantity(BigDecimal.valueOf(1 + random.nextInt(50_000), 4))
                        .unitPrice(BigDecimal.valueOf(random.nextInt(2_000_000), 4))
                        .standardRate(group).build();
                lines.add(line);
                builder.line(line);
            }
            Document document = builder.build();

            BigDecimal unroundedGross = lines.stream().map(Line::totalWithVat).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unroundedVat = lines.stream().map(Line::totalVat).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal roundedLinesGross = lines.stream().map(line -> Money.total(line.totalWithVat()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(document.totals().gross()).isEqualByComparingTo(Money.total(unroundedGross));
            assertThat(document.totals().vat()).isEqualByComparingTo(Money.total(unroundedVat));
            assertThat(document.totals().grossRounded()).isEqualByComparingTo(Money.whole(unroundedGross));
            sawADifference |= Money.total(unroundedGross).compareTo(roundedLinesGross) != 0;
        }
        assertThat(sawADifference).as("the generated cases include ones where per-line rounding would differ").isTrue();
    }
}
