package uk.gov.dwp.uc.pairtest;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.uc.pairtest.domain.PurchaseOrder;
import uk.gov.dwp.uc.pairtest.domain.PurchaseSummary;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseCalculatorTest {
    private final PurchaseCalculator calculator = new PurchaseCalculator();

    @Test
    void calculatesPriceAndExcludesInfantsFromSeating() {
        PurchaseSummary summary = calculator.calculate(new PurchaseOrder(Map.of(
                Type.ADULT, 3, Type.CHILD, 2, Type.INFANT, 2)));
        assertEquals(105, summary.totalPriceInPounds());
        assertEquals(5, summary.seatsToReserve());
    }

    @Test
    void purchaseOrderDefensivelyCopiesInput() {
        EnumMap<Type, Integer> initial = new EnumMap<>(Type.class);
        initial.put(Type.ADULT, 1);
        PurchaseOrder order = new PurchaseOrder(initial);
        initial.put(Type.ADULT, 25);
        assertEquals(1, order.quantityOf(Type.ADULT));
    }
}
