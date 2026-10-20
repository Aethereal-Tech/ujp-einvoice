# The `einvoice_api` API description

`einvoice-api-openapi-2026-10-20.json` is the gateway's own published API description for the `/einvoice_api`
services: OpenAPI 3.1.0, `info.version` `v0`, one server (`/einvoice_api` on the sandbox host), 48 paths, 56
schemas. It is committed here unmodified, as test data for `OpenApiConformanceTest`.

- **sha256** `0c56a01ebd0e4ef6dde8f6e8612274a2f0dfc25ea830ed72b618eb70edc415a5`. A test checks it, so an edit is
  deliberate.
- **Retrieved** 2026-10-20, at the owner's request, by a person: plain GETs, no authentication and no signed
  request, to the Swagger UI the documentation links and the files it loads, the last being
  `/einvoice_api/swagger.json/public` on the sandbox host. `openspec/specs/specification-status/openapi-2026-10-20.md`
  records the retrieval and what the document settles. Nothing here, and no test, contacts that host.
- **Scope.** It describes `einvoice_api` only. The JSON Receiver, which takes the signed document, has no published
  description, so nothing in this folder can validate a `document` or the receiver's error body.

## What the conformance test can and cannot prove

The test validates with a small helper in the test sources (`OpenApiSchema`), which reads `$ref`, `type`,
`properties`, `required`, `items`, `additionalProperties`, `enum` and the `date` and `date-time` formats. It has
two properties worth knowing before trusting a green run:

- **It checks shape, and the description constrains little.** The schemas declare almost no `required` members, so
  "valid" mostly means every member that is present has the declared type, and a body missing a member the real
  gateway always sends still passes. The helper cannot tell optionality; it is not evidence of it.
- **A JSON `null` is accepted for any member that is not `required`.** The gateway's own examples serve
  `"errorStatus": null` on every success, under a schema that says `object`; a strict reading would fail the
  gateway's own documentation.

What it does prove: the operations and methods the client calls exist in the description, every header the
description declares required is sent, the request body the client builds is the declared `InvoiceSignedRequest`,
each `einvoice_api` response body listed in the conformance test (the bodies of `GatewayBodies`) validates against
the schema the description declares for that HTTP status, every member of `InvoiceStatus` and `DocumentStatusChange` is read by the client, and every code the
library lists under `EINVOICE_API` is in the description's `ErrorStatusBean.errorCode` enum.
