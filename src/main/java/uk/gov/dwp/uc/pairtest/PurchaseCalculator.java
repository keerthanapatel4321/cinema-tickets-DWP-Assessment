package uk.gov.dwp.uc.pairtest;

import java.util.Map;
import uk.gov.dwp.uc.pairtest.domain.PurchaseOrder;
import uk.gov.dwp.uc.pairtest.domain.PurchaseSummary;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;

/** Pure price/seat computation, independent from payment and reservation IO. */
final class PurchaseCalculator {
    private static final Map<Type, Integer> PRICES_IN_POUNDS = Map.of(
            Type.ADULT, 25,
            Type.CHILD, 15,
            Type.INFANT, 0);

    PurchaseSummary calculate(PurchaseOrder order) {
        int amount = order.quantities().entrySet().stream()
                .mapToInt(entry -> PRICES_IN_POUNDS.get(entry.getKey()) * entry.getValue())
                .sum();
        int seats = order.quantities().entrySet().stream()
                .filter(entry -> entry.getKey() != Type.INFANT)
                .mapToInt(Map.Entry::getValue)
                .sum();
        return new PurchaseSummary(amount, seats);
    }
}
