package net.aetherealtech.ujpeinvoice.testsupport;

/**
 * Response bodies the transport tests feed the client, in the shapes D1 and the API description show. Invented
 * identifiers throughout; the host in a link is the reserved {@code .test} domain, never a UJP host.
 * {@code OpenApiConformanceTest} validates the {@code einvoice_api} ones against the committed description.
 */
public final class GatewayBodies {

    public static final String EUID = "019df254-acd6-723c-a1a9-09516b85af93";

    private GatewayBodies() {
    }

    public static String serverTime(String timestamp) {
        return "{\"timestamp\":\"" + timestamp + "\"}";
    }

    /** D1 „Праќање на документ", the accepted response, with the host replaced. */
    public static final String SUBMITTED = """
            {
              "euid": "019b8d43-7840-7433-b358-08891b53605c",
              "message": "Фактура успешно зачувана",
              "qr_link": "https://gateway.test/euid/019b8d43-7840-7433-b358-08891b53605c",
              "status": 200,
              "e_invoice_user": false,
              "timestamp": "2026-01-05T08:26:07.846Z"
            }""";

    /** D1 „Тековен статус на излезна фактура", the response, with every member of the description's InvoiceStatus. */
    public static final String CURRENT_STATUS = """
            {
              "success": true,
              "createdDate": "2025-10-21T07:00:00",
              "errorStatus": null,
              "docStatus": {
                "euid": "019df254-acd6-723c-a1a9-09516b85af93",
                "statusCode": "03",
                "statusName": "Прифатена",
                "docNumber": "e6e70a12-fdbe-4542-8",
                "docDate": "2026-05-20",
                "docDeliveryDate": "2026-05-20",
                "docNetAmount": 1500.5,
                "docVatAmount": 270,
                "docGrossAmountR": 1771,
                "docAvansAmount": 100,
                "docFinalAmount": 1671,
                "sellerTin": "4030995135699",
                "buyerTin": "4030995135700",
                "senderTin": "",
                "receiverTin": "",
                "docType": "100",
                "docTypeName": "Фактура",
                "comment": "прв коментар",
                "cancelComment": "Грешка во износот",
                "voidComment": "Грешка во износот",
                "voidReason": "Рекламација",
                "voidReasonCode": "S-1",
                "rejectReason": "Invalid date",
                "rejectReasonCode": "O-2",
                "correctionReasonComment": "Погрешна цена",
                "correctionReason": "Грешка во цена",
                "correctionReasonCode": "C01"
              }
            }""";

    /** A status code the statuses page does not list. */
    public static final String UNKNOWN_STATUS = """
            {"success":true,"createdDate":"2026-10-20T07:00:00","errorStatus":null,
             "docStatus":{"euid":"019df254-acd6-723c-a1a9-09516b85af93","statusCode":"99","statusName":"Нов статус"}}""";

    /** D1 „Статус историја на документ", the response (the members the description does not declare dropped). */
    public static final String STATUS_HISTORY = """
            {
              "success": true,
              "createdDate": "2025-10-21T07:00:00",
              "errorStatus": null,
              "statusHistory": [
                {
                  "euid": "019df254-acd6-723c-a1a9-09516b85af93",
                  "oldStatus": {"code": "01", "name": "Испратена (Нова)"},
                  "newStatus": {"code": "07", "name": "Сторнирана"},
                  "timestamp": "2024-07-20T16:25:39",
                  "comment": "коментар",
                  "cancelComment": "Грешка во износот",
                  "voidComment": "Грешка во износот",
                  "voidReasonCode": "S-1",
                  "voidReason": "Рекламација",
                  "rejectReasonCode": "O-2",
                  "rejectReason": "Invalid date"
                }
              ]
            }""";

    /** An {@code einvoice_api} failure as the description's examples show it: a GenericAPIResponse. */
    public static String wrappedError(String code, String message) {
        return """
                {"success":false,"createdDate":"2025-11-19T09:27:18",
                 "errorStatus":{"errorCode":"%s","errorMessage":"%s","errorPropertyMap":null}}""".formatted(code, message);
    }

    /** {@code current-status} declares its 400 and 500 as a bare ErrorStatusBean. */
    public static String bareError(String code, String message) {
        return """
                {"errorCode":"%s","errorMessage":"%s","errorPropertyMap":{"euid":["must not be blank"]}}"""
                .formatted(code, message);
    }
}
