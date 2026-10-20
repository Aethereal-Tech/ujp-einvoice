package net.aetherealtech.ujpeinvoice.transport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static net.aetherealtech.ujpeinvoice.transport.ErrorNamespace.EINVOICE_API;
import static net.aetherealtech.ujpeinvoice.transport.ErrorNamespace.JSON_RECEIVER;
import static net.aetherealtech.ujpeinvoice.transport.ErrorNamespace.WEB_APP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UjpErrorCodesTest {

    @Test
    void theCatalogueHoldsEveryEntryD5ListsPerNamespace() {
        assertThat(UjpErrorCodes.all(EINVOICE_API)).hasSize(55);
        assertThat(UjpErrorCodes.all(JSON_RECEIVER)).hasSize(104);
        assertThat(UjpErrorCodes.all(WEB_APP)).hasSize(32);
    }

    @ParameterizedTest
    @EnumSource(ErrorNamespace.class)
    void everyEntryBelongsToTheNamespaceItIsListedUnderAndNoCodeRepeatsWithinOne(ErrorNamespace namespace) {
        List<UjpErrorCode> entries = UjpErrorCodes.all(namespace);

        assertThat(entries).allSatisfy(entry -> {
            assertThat(entry.namespace()).isEqualTo(namespace);
            assertThat(entry.code()).matches("E\\d+");
            assertThat(entry.meaning()).isNotBlank();
            assertThat(UjpErrorCodes.lookup(namespace, entry.code())).contains(entry);
        });
        assertThat(entries).extracting(UjpErrorCode::code).doesNotHaveDuplicates();
    }

    @Test
    void theListedEntriesAreImmutable() {
        List<UjpErrorCode> entries = UjpErrorCodes.all(EINVOICE_API);
        UjpErrorCode any = entries.getFirst();

        assertThatThrownBy(() -> entries.add(any)).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "EINVOICE_API | E1012 | Certificate not found",
            "JSON_RECEIVER | E1012 | Certificate not found",
            "WEB_APP | E5004 | Certificate validation failed",
            "JSON_RECEIVER | E5004 | Certificate validation failed",
            "EINVOICE_API | E5004 | Certificate validation failed",
            "JSON_RECEIVER | E10001 | Missing TIN during company validation process",
            "JSON_RECEIVER | E10002 | An incorrect company name was provided",
            "JSON_RECEIVER | E10003 | An incorrect VAT number was provided for the company",
            "JSON_RECEIVER | E1033 | Invalid DocIndicator",
            "JSON_RECEIVER | E1034 | Invalid DocAlt",
            "JSON_RECEIVER | E1035 | Invalid Attribute",
            "JSON_RECEIVER | E500719 | docIndicator mismatch",
            "JSON_RECEIVER | E500720 | docAlt mismatch",
            "EINVOICE_API | E5014 | The allowed period for modifying Purchase VAT totals has expired",
            "EINVOICE_API | E5018 | Attribute value validation failed.",
            "EINVOICE_API | E5019 | Attribute type is required.",
            "JSON_RECEIVER | E4007 | Rate limit exceeded",
            "JSON_RECEIVER | E40122 | The requestTimestamp difference is greater than 5 minutes",
            "JSON_RECEIVER | E1023 | Invoice already exists for that ID",
            "JSON_RECEIVER | E1013 | User is not authorized to sign documents for this company",
    })
    void specificCodesResolveWithTheMeaningD5States(ErrorNamespace namespace, String code, String meaning) {
        assertThat(UjpErrorCodes.lookup(namespace, code))
                .contains(new UjpErrorCode(namespace, code, meaning));
    }

    @Test
    void theSameCodeResolvesDifferentlyWhereD5SaysSo() {
        assertThat(UjpErrorCodes.lookup(EINVOICE_API, "E1020").orElseThrow().meaning())
                .isEqualTo("This certificate for this company already exists!");
        assertThat(UjpErrorCodes.lookup(JSON_RECEIVER, "E1020").orElseThrow().meaning())
                .isEqualTo("Invalid calculated totals");
        assertThat(UjpErrorCodes.lookup(WEB_APP, "E1020")).isEmpty();

        assertThat(UjpErrorCodes.lookup(EINVOICE_API, "E4023").orElseThrow().meaning())
                .isEqualTo("Notification with ID was not found");
        assertThat(UjpErrorCodes.lookup(JSON_RECEIVER, "E4023").orElseThrow().meaning())
                .isEqualTo("Invalid X-EDB");

        assertThat(UjpErrorCodes.lookup(WEB_APP, "E1019").orElseThrow().meaning())
                .isEqualTo("Selected date range exceeds allowed maximum limit of days");
        assertThat(UjpErrorCodes.lookup(EINVOICE_API, "E1019").orElseThrow().meaning())
                .isEqualTo("Status History List not found.");
    }

    @Test
    void aCodeListedUnderOneNamespaceIsNotFoundUnderAnother() {
        assertThat(UjpErrorCodes.lookup(JSON_RECEIVER, "E10001")).isPresent();
        assertThat(UjpErrorCodes.lookup(EINVOICE_API, "E10001")).isEmpty();
        assertThat(UjpErrorCodes.lookup(WEB_APP, "E10001")).isEmpty();
    }

    @Test
    void theOpenApiOnlyCodesAreNotInventedForEinvoiceApi() {
        List.of("E1003", "E1005", "E1007", "E1008", "E4006", "E4017", "E4018", "E4019", "E4020", "E4021", "E4022",
                        "E4024", "E4027", "E5009", "E5010", "E5012", "E5013")
                .forEach(code -> assertThat(UjpErrorCodes.lookup(EINVOICE_API, code)).as(code).isEmpty());
    }

    @Test
    void lookupOfAnUnlistedOrAbsentCodeIsEmpty() {
        assertThat(UjpErrorCodes.lookup(JSON_RECEIVER, "E9999")).isEmpty();
        assertThat(UjpErrorCodes.lookup(JSON_RECEIVER, "e1012")).isEmpty();
        assertThat(UjpErrorCodes.lookup(JSON_RECEIVER, null)).isEmpty();
        assertThat(UjpErrorCodes.lookup(null, "E1012")).isEmpty();
    }

    @Test
    void meaningsKeepThePagesOwnSpelling() {
        assertThat(UjpErrorCodes.lookup(JSON_RECEIVER, "E1024").orElseThrow().meaning())
                .isEqualTo("Voided document must refrence a document");
        assertThat(UjpErrorCodes.lookup(JSON_RECEIVER, "E10005").orElseThrow().meaning())
                .isEqualTo("An incorrect postal code was provided for the company");
    }

    @Test
    void namespacesNameTheirD5Heading() {
        assertThat(ErrorNamespace.values()).extracting(ErrorNamespace::catalogueName)
                .containsExactly("JSON Receiver", "eInvoiceApi", "Web App");
    }

    @Test
    void anErrorCodeEntryRequiresAllThreeParts() {
        assertThatThrownBy(() -> new UjpErrorCode(null, "E1", "m")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UjpErrorCode(WEB_APP, null, "m")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UjpErrorCode(WEB_APP, "E1", null)).isInstanceOf(NullPointerException.class);
    }
}
