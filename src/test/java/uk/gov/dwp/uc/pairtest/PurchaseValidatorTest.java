package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.uc.pairtest.domain.PurchaseOrder;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseValidatorTest {
    private final PurchaseValidator validator = new PurchaseValidator();

    @Test
    void groupsDuplicateTicketTypesIntoAnImmutableSnapshot() {
        PurchaseOrder order = validator.validate(1L, new TicketTypeRequest[] {
                new TicketTypeRequest(Type.ADULT, 1),
                new TicketTypeRequest(Type.ADULT, 2),
                new TicketTypeRequest(Type.CHILD, 3),
                new TicketTypeRequest(Type.INFANT, 2)
        });
        assertEquals(3, order.quantityOf(Type.ADULT));
        assertEquals(3, order.quantityOf(Type.CHILD));
        assertEquals(2, order.quantityOf(Type.INFANT));
        assertThrows(UnsupportedOperationException.class,
                () -> order.quantities().put(Type.ADULT, 100));
    }

    @Test
    void rejectsNegativeAmountsAndMissingAdults() {
        assertThrows(InvalidPurchaseException.class,
                () -> validator.validate(1L, new TicketTypeRequest[] {
                        new TicketTypeRequest(Type.ADULT, -1)
                }));
        assertThrows(InvalidPurchaseException.class,
                () -> validator.validate(1L, new TicketTypeRequest[] {
                        new TicketTypeRequest(Type.CHILD, 2)
                }));
    }
}
