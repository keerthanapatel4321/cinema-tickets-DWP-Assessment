package uk.gov.dwp.uc.pairtest.domain;

import java.util.Map;
import java.util.Objects;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;

/** Defensive immutable snapshot of the quantities for one valid purchase. */
public record PurchaseOrder(Map<Type, Integer> quantities) {
    public PurchaseOrder {
        quantities = Map.copyOf(Objects.requireNonNull(quantities, "quantities"));
    }

    public int quantityOf(Type type) {
        return quantities.getOrDefault(type, 0);
    }
}
