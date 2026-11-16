package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/** Interaction tests verifying order, counts, prices and lack of side effects. */
class TicketServiceImplTest {
    private TicketPaymentService payment;
    private SeatReservationService seats;
    private TicketService service;

    @BeforeEach
    void setUp() {
        payment = mock(TicketPaymentService.class);
        seats = mock(SeatReservationService.class);
        service = new TicketServiceImpl(payment, seats);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 0, 0, 25, 1",
            "2, 2, 0, 80, 4",
            "2, 0, 2, 50, 2",
            "1, 1, 1, 40, 2",
            "25, 0, 0, 625, 25",
            "1, 24, 0, 385, 25"
    })
    void paysAndReservesCorrectAmounts(int adults, int children, int infants,
                                      int amount, int seatCount) {
        java.util.List<TicketTypeRequest> requests = new java.util.ArrayList<>();
        if (adults > 0) requests.add(ticket(Type.ADULT, adults));
        if (children > 0) requests.add(ticket(Type.CHILD, children));
        if (infants > 0) requests.add(ticket(Type.INFANT, infants));

        service.purchaseTickets(14L, requests.toArray(TicketTypeRequest[]::new));

        InOrder ordered = inOrder(payment, seats);
        ordered.verify(payment).makePayment(14L, amount);
        ordered.verify(seats).reserveSeat(14L, seatCount);
        ordered.verifyNoMoreInteractions();
    }

    @Test
    void aggregatesDuplicateRequestTypes() {
        service.purchaseTickets(19L, ticket(Type.ADULT, 1), ticket(Type.ADULT, 2),
                ticket(Type.CHILD, 1), ticket(Type.INFANT, 2), ticket(Type.CHILD, 2));
        verify(payment).makePayment(19L, 120);
        verify(seats).reserveSeat(19L, 6);
        verifyNoMoreInteractions(payment, seats);
    }

    @Test
    void supportsTwentyFiveTicketsIncludingInfants() {
        service.purchaseTickets(8L, ticket(Type.ADULT, 13), ticket(Type.INFANT, 12));
        verify(payment).makePayment(8L, 325);
        verify(seats).reserveSeat(8L, 13);
    }

    @Test
    void rejectsInvalidAccountsWithoutExternalCalls() {
        assertRejected(null, ticket(Type.ADULT, 1));
        assertRejected(0L, ticket(Type.ADULT, 1));
        assertRejected(-40L, ticket(Type.ADULT, 1));
    }

    @Test
    void rejectsMissingOrMalformedRequestsWithoutExternalCalls() {
        assertRejected(1L, (TicketTypeRequest[]) null);
        assertRejected(1L);
        assertRejected(1L, (TicketTypeRequest) null);
        assertRejected(1L, ticket(Type.ADULT, 1), null);
        assertRejected(1L, ticket(null, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsInvalidQuantities(int count) {
        assertRejected(1L, ticket(Type.ADULT, count));
    }

    @Test
    void rejectsChildOnlyOrInfantOnlyPurchases() {
        assertRejected(1L, ticket(Type.CHILD, 1));
        assertRejected(1L, ticket(Type.INFANT, 1));
        assertRejected(1L, ticket(Type.CHILD, 1), ticket(Type.INFANT, 1));
    }

    @Test
    void acceptsMultipleInfantsWithOneAdultWhenWithinTicketLimit() {
        service.purchaseTickets(11L, ticket(Type.ADULT, 1), ticket(Type.INFANT, 2));
        verify(payment).makePayment(11L, 25);
        verify(seats).reserveSeat(11L, 1);
        verifyNoMoreInteractions(payment, seats);
    }

    @Test
    void rejectsMoreThanTwentyFiveTicketsAndIntegerOverflow() {
        assertRejected(1L, ticket(Type.ADULT, 1), ticket(Type.CHILD, 25));
        assertRejected(1L, ticket(Type.ADULT, Integer.MAX_VALUE));
        assertRejected(1L, ticket(Type.ADULT, Integer.MAX_VALUE),
                ticket(Type.CHILD, Integer.MAX_VALUE));
    }

    private TicketTypeRequest ticket(Type type, int count) {
        return new TicketTypeRequest(type, count);
    }

    private void assertRejected(Long accountId, TicketTypeRequest... requests) {
        assertThrows(InvalidPurchaseException.class,
                () -> service.purchaseTickets(accountId, requests));
        verifyNoInteractions(payment, seats);
    }
}
