import java.util.ArrayList;
import java.util.List;
import uk.gov.dwp.uc.pairtest.TicketService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

/**
 * Executable verification using only the JDK: no Maven or external test jars.
 * JUnit suites under src/test offer separately maintained integration/unit tests.
 */
public final class SmokeCheck {
    private static int cases = 0;

    public static void main(String[] args) {
        assertPurchase(42L, 25, 1, ticket(Type.ADULT, 1));
        assertPurchase(42L, 80, 4, ticket(Type.ADULT, 2), ticket(Type.CHILD, 2));
        assertPurchase(42L, 50, 2, ticket(Type.ADULT, 2), ticket(Type.INFANT, 2));
        assertPurchase(42L, 40, 2, ticket(Type.ADULT, 1), ticket(Type.CHILD, 1), ticket(Type.INFANT, 1));
        assertPurchase(42L, 625, 25, ticket(Type.ADULT, 25));
        assertPurchase(42L, 120, 6, ticket(Type.ADULT, 1), ticket(Type.CHILD, 1),
                ticket(Type.ADULT, 2), ticket(Type.CHILD, 2), ticket(Type.INFANT, 2));
        assertPurchase(42L, 325, 13, ticket(Type.ADULT, 13), ticket(Type.INFANT, 12));
        assertRejected(null, ticket(Type.ADULT, 1));
        assertRejected(0L, ticket(Type.ADULT, 1));
        assertRejected(-5L, ticket(Type.ADULT, 1));
        assertRejected(1L, (TicketTypeRequest[]) null);
        assertRejected(1L);
        assertRejected(1L, (TicketTypeRequest) null);
        assertRejected(1L, ticket(Type.ADULT, 1), null);
        assertRejected(1L, ticket(null, 1));
        assertRejected(1L, ticket(Type.ADULT, 0));
        assertRejected(1L, ticket(Type.ADULT, -1));
        assertRejected(1L, ticket(Type.ADULT, Integer.MIN_VALUE));
        assertRejected(1L, ticket(Type.CHILD, 1));
        assertRejected(1L, ticket(Type.INFANT, 1));
        assertPurchase(42L, 25, 1, ticket(Type.ADULT, 1), ticket(Type.INFANT, 2));
        assertRejected(1L, ticket(Type.ADULT, 1), ticket(Type.CHILD, 25));
        assertRejected(1L, ticket(Type.ADULT, Integer.MAX_VALUE));
        assertRejected(1L, ticket(Type.ADULT, 1), ticket(Type.CHILD, Integer.MAX_VALUE));

        // Exhaustively check all valid combinations of 1..25 tickets, under
        // all valid combinations with at least one adult and at most 25 tickets.
        for (int adults = 1; adults <= 25; adults++) {
            for (int children = 0; children <= 25 - adults; children++) {
                for (int infants = 0; infants <= 25 - adults - children; infants++) {
                    List<TicketTypeRequest> requests = new ArrayList<>();
                    requests.add(ticket(Type.ADULT, adults));
                    if (children > 0) requests.add(ticket(Type.CHILD, children));
                    if (infants > 0) requests.add(ticket(Type.INFANT, infants));
                    assertPurchase(10L, adults * 25 + children * 15, adults + children,
                            requests.toArray(TicketTypeRequest[]::new));
                }
            }
        }
        System.out.println("PASS: " + cases + " assertions (including exhaustive valid combinations)");
    }

    private static TicketTypeRequest ticket(Type type, int quantity) {
        return new TicketTypeRequest(type, quantity);
    }

    private static void assertPurchase(Long accountId, int expectedPayment, int expectedSeats,
                                       TicketTypeRequest... tickets) {
        List<String> events = new ArrayList<>();
        TicketService service = new TicketServiceImpl(
                (id, amount) -> events.add("payment:" + id + ":" + amount),
                (id, seats) -> events.add("seats:" + id + ":" + seats));
        service.purchaseTickets(accountId, tickets);
        List<String> expected = List.of("payment:" + accountId + ":" + expectedPayment,
                "seats:" + accountId + ":" + expectedSeats);
        if (!expected.equals(events)) {
            throw new AssertionError("Expected " + expected + " but got " + events);
        }
        cases++;
    }

    private static void assertRejected(Long accountId, TicketTypeRequest... tickets) {
        List<String> events = new ArrayList<>();
        TicketService service = new TicketServiceImpl(
                (id, amount) -> events.add("payment"),
                (id, seats) -> events.add("seats"));
        try {
            service.purchaseTickets(accountId, tickets);
            throw new AssertionError("Request was accepted but should be rejected");
        } catch (InvalidPurchaseException expected) {
            if (!events.isEmpty()) {
                throw new AssertionError("Invalid purchase called external services: " + events);
            }
            cases++;
        }
    }
}
