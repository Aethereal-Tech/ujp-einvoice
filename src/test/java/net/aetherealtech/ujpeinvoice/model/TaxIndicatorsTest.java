package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxIndicatorsTest {

    private static final TaxIndicators.Snapshot SNAPSHOT = TaxIndicators.SNAPSHOT_2026_10_16;

    @Test
    void theSnapshotIsDatedAndHoldsTheNinetySalesIndicatorsOfThePage() {
        assertThat(SNAPSHOT.retrievedOn()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(SNAPSHOT.all()).hasSize(90);
    }

    @Test
    void theFourRateGroupsAreStandardExceptGWhichTheGatewayCallsNula() {
        assertThat(SNAPSHOT.get("DDV-A")).isEqualTo(TaxIndicator.standard(VatGroup.DDV_A));
        assertThat(SNAPSHOT.get("DDV-V").group()).isEqualTo(VatGroup.DDV_V);
        assertThat(SNAPSHOT.get("DDV-B").vatImpact()).isEqualTo(VatImpact.STANDARD);
        assertThat(SNAPSHOT.get("DDV-G").vatImpact()).isEqualTo(VatImpact.NULA);
        assertThat(SNAPSHOT.get("DDV-G").note()).isEqualTo("ДДВ не е пресметан согласно член 51 став 3 од ЗДДВ");
    }

    @Test
    void everyIndicatorThatIsNotStandardCarriesTheNoteTheGatewayWants() {
        assertThat(SNAPSHOT.all()).filteredOn(indicator -> indicator.vatImpact() != VatImpact.STANDARD)
                .allSatisfy(indicator -> assertThat(indicator.note()).isNotBlank());
    }

    @Test
    void theImpactsFollowTheNamesOfTheIndicators() {
        assertThat(SNAPSHOT.get("DDV-8-2").vatImpact()).isEqualTo(VatImpact.NULA);
        assertThat(SNAPSHOT.get("DDV-9-1").vatImpact()).isEqualTo(VatImpact.OSLOBODEN);
        assertThat(SNAPSHOT.get("DDV-10-13-1-1").vatImpact()).isEqualTo(VatImpact.OSLOBODEN);
        assertThat(SNAPSHOT.get("DDV-11-A-G").vatImpact()).isEqualTo(VatImpact.PRENESEN);
        assertThat(SNAPSHOT.get("DDV-12-4").vatImpact()).isEqualTo(VatImpact.PRENESEN);
        assertThat(SNAPSHOT.get("DDV-M-18").vatImpact()).isEqualTo(VatImpact.MARGINALNA);
        assertThat(SNAPSHOT.get("ZDSJD-18").vatImpact()).isEqualTo(VatImpact.STANDARD);
        assertThat(SNAPSHOT.get("DDV-10-BN").vatImpact()).isEqualTo(VatImpact.STANDARD);
    }

    @Test
    void anIndicatorOfSeveralGroupsHasNoGroupOfItsOwn() {
        assertThat(SNAPSHOT.get("DDV-uvoz").group()).isNull();
        assertThat(SNAPSHOT.get("DDV-vaucer-E").group()).isNull();
    }

    @Test
    void codesAreStoredAsThePagePrintsThem() {
        // The page writes a Cyrillic О in DDV-11-A-О and a Cyrillic А in DDV-PNPI-А; a Latin spelling is another code.
        assertThat(SNAPSHOT.find("DDV-11-A-О")).isPresent();
        assertThat(SNAPSHOT.find("DDV-11-A-O")).isEmpty();
        assertThat(SNAPSHOT.find("DDV-PNPI-А")).isPresent();
    }

    @Test
    void theSecondRowPrintedAsDdv10144sixIsStoredUnderTheChangelogsCode() {
        assertThat(SNAPSHOT.get("DDV-10-14-4-6").note()).contains("14 став 4 точка 6");
        assertThat(SNAPSHOT.get("DDV-10-14-6").note()).contains("14 став 6");
    }

    @Test
    void anUnknownCodeIsRefusedByNameAndAnExplicitIndicatorIsPossible() {
        assertThatThrownBy(() -> SNAPSHOT.get("DDV-NEW"))
                .hasMessage("UJP07-19 taxIndicator DDV-NEW is not in the snapshot of 2026-10-19; "
                        + "state its vatImpact explicitly");
        assertThat(new TaxIndicator("DDV-NEW", VatImpact.OSLOBODEN, VatGroup.DDV_G, "note").code()).isEqualTo("DDV-NEW");
    }

    @Test
    void aNoteIsRequiredUnlessTheImpactIsStandard() {
        assertThatThrownBy(() -> new TaxIndicator("DDV-NEW", VatImpact.PRENESEN, VatGroup.DDV_A, null))
                .hasMessage("UJP09-01-02 vatTaxIndicatorNote is required for DDV-NEW, whose impact is PRENESEN");
        assertThat(new TaxIndicator("DDV-NEW", VatImpact.STANDARD, VatGroup.DDV_A, null).note()).isNull();
        assertThatThrownBy(() -> new TaxIndicator("DDV-NEW", null, VatGroup.DDV_A, null))
                .hasMessage("UJP07-19 taxIndicator.vatImpact is required");
        assertThatThrownBy(() -> new TaxIndicator(" ", VatImpact.STANDARD, null, null))
                .hasMessage("UJP07-19 taxIndicator.code is required");
    }
}
