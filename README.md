# Cinema Tickets - Functional & Modular (Java 21)

An independent design, built from the **same unmodified DWP Java starter
interfaces and third-party package**. It uses streams, immutable records and
single-responsibility components rather than an all-in-one imperative service.

## Architecture

```text
TicketServiceImpl
  ├── PurchaseValidator   -> checks all rules; groups requests via Streams
  │      └── PurchaseOrder (defensive immutable record of counts)
  ├── PurchaseCalculator -> pure price/seat calculations via Streams
  │      └── PurchaseSummary (immutable Java record)
  ├── TicketPaymentService    (protected external interface)
  └── SeatReservationService (protected external interface)
```

The orchestration service calls payment and then reservation, **only after**
complete validation. `PurchaseOrder` defensively copies the grouped map so
callers cannot change validated order quantities afterwards. The quote
calculator is deterministic and has no third-party dependencies.

## Rules and policy assumptions

| Ticket | Price | Seats |
|---|---:|---:|
| ADULT | £25 | 1 |
| CHILD | £15 | 1 |
| INFANT | £0 | 0 |

- Account IDs must be non-null and >0.
- 1–25 tickets overall; all request elements/types non-null and counts positive.
- At least one adult is required; child-only and infant-only purchases fail.
- Repeated ticket types are aggregated before calculations.
- No additional infant-to-adult ratio is imposed; an adult ticket is required.
- Invalid requests never reach either external provider.
- Summing in `long` prevents invalid int quantities overflowing the 25-ticket check.

## Requirements / testing

Java **JDK 21+**, Maven **3.9+**.

```bash
mvn clean test
mvn package
```

`src/test/java` contains JUnit 5 tests using Mockito for service interactions,
plus direct tests for the validator, pure calculator and immutable order.

The project also includes a JDK-only verifier for environments without Maven:

```bash
bash scripts/verify-offline.sh
```

It compiles the production code, checks valid/invalid provider calls and
exhaustively verifies all all allowed quantity combinations.

## Sample

```java
TicketService service = new TicketServiceImpl();
service.purchaseTickets(42L,
    new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
    new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3),
    new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1));
// Requests £95 payment and reserves 5 seats.
```

The provided third-party implementation classes are deliberately empty
provider stubs. Inject live implementations for a real application.
This is a practice exercise and does not take actual payments.

## Starter compatibility

The original `TicketTypeRequest`, `InvalidPurchaseException`, `TicketService`, and all `thirdparty/` Java files are preserved byte-for-byte. The supplied exception has no message constructor, so validation throws `new InvalidPurchaseException()`. The one-infant-per-adult assumption was removed because it was not stated as a numeric limit in the starter requirements.
