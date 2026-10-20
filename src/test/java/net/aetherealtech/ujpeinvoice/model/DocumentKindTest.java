package net.aetherealtech.ujpeinvoice.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentKindTest {

    @ParameterizedTest
    @CsvSource({
            "INVOICE,100,Фактура",
            "CREDIT_NOTE,110,Книжно одобрение",
            "DEBIT_NOTE,120,Книжно задолжение",
            "ADVANCE_INVOICE,130,Авансна фактура",
            "CASH_TURNOVER,310,Фактура за регистриран готовински промет"})
    void eachKindCarriesItsCodeAndTheNameOfTheCodeList(DocumentKind kind, String code, String name) {
        assertThat(kind.code()).isEqualTo(code);
        assertThat(kind.ujpName()).isEqualTo(name);
    }

    @ParameterizedTest
    @CsvSource({
            "INVOICE,true,false,true,false,true,true,false,false",
            "CREDIT_NOTE,false,true,false,false,false,false,false,false",
            "DEBIT_NOTE,false,true,false,false,false,false,false,false",
            "ADVANCE_INVOICE,false,false,true,true,false,false,true,true",
            "CASH_TURNOVER,true,false,true,true,false,false,true,true"})
    void theRulesAreTheOnesTheTypePagesState(DocumentKind kind, boolean turnoverDate, boolean references,
                                             boolean payment, boolean paymentType, boolean advance,
                                             boolean attributes, boolean currencyRate, boolean unit) {
        assertThat(kind.requiresTurnoverDate()).isEqualTo(turnoverDate);
        assertThat(kind.requiresReferences()).isEqualTo(references);
        assertThat(kind.allowsPayment()).isEqualTo(payment);
        assertThat(kind.requiresPaymentType()).isEqualTo(paymentType);
        assertThat(kind.allowsAdvanceDeduction()).isEqualTo(advance);
        assertThat(kind.allowsAttributes()).isEqualTo(attributes);
        assertThat(kind.listsCurrencyRate()).isEqualTo(currencyRate);
        assertThat(kind.requiresUnit()).isEqualTo(unit);
    }

    @ParameterizedTest
    @EnumSource(DocumentKind.class)
    void only310IsShorterThanTheOthers(DocumentKind kind) {
        boolean cash = kind == DocumentKind.CASH_TURNOVER;

        assertThat(kind.docIdMaxLength()).isEqualTo(cash ? 12 : 30);
        assertThat(kind.partyNameMaxLength()).isEqualTo(cash ? 80 : 150);
        assertThat(kind.streetMaxLength()).isEqualTo(cash ? 60 : 80);
        assertThat(kind.streetNumberMaxLength()).isEqualTo(cash ? 10 : 20);
        assertThat(kind.subsidiaryNameMaxLength()).isEqualTo(cash ? 100 : 150);
    }

    @Test
    void thereAreFiveKinds() {
        assertThat(DocumentKind.values()).hasSize(5);
    }
}
