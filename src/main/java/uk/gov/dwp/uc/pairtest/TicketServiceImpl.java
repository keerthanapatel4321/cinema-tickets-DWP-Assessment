package uk.gov.dwp.uc.pairtest;

import java.util.Objects;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationService;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.PurchaseOrder;
import uk.gov.dwp.uc.pairtest.domain.PurchaseSummary;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

/** Thin orchestration layer: validation, quoting and external integration. */
public class TicketServiceImpl implements TicketService {
    private final TicketPaymentService paymentService;
    private final SeatReservationService seatReservationService;
    private final PurchaseValidator validator;
    private final PurchaseCalculator calculator;

    public TicketServiceImpl() {
        this(new TicketPaymentServiceImpl(), new SeatReservationServiceImpl());
    }

    public TicketServiceImpl(TicketPaymentService paymentService,
                             SeatReservationService seatReservationService) {
        this.paymentService = Objects.requireNonNull(paymentService, "paymentService");
        this.seatReservationService = Objects.requireNonNull(seatReservationService, "seatReservationService");
        this.validator = new PurchaseValidator();
        this.calculator = new PurchaseCalculator();
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        PurchaseOrder order = validator.validate(accountId, ticketTypeRequests);
        PurchaseSummary summary = calculator.calculate(order);

        paymentService.makePayment(accountId, summary.totalPriceInPounds());
        seatReservationService.reserveSeat(accountId, summary.seatsToReserve());
    }
}
