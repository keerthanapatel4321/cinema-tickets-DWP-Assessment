package uk.gov.dwp.uc.pairtest;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import uk.gov.dwp.uc.pairtest.domain.PurchaseOrder;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

/** Converts validated ticket requests into an immutable aggregate order. */
final class PurchaseValidator {
    private static final int MAX_TICKETS = 25;

    PurchaseOrder validate(Long accountId, TicketTypeRequest[] requests) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException();
        }
        if (requests == null || requests.length == 0) {
            throw new InvalidPurchaseException();
        }

        // Check nulls before dereferencing requests in downstream stream operations.
        if (Arrays.stream(requests).anyMatch(Objects::isNull)) {
            throw new InvalidPurchaseException();
        }
        if (Arrays.stream(requests).anyMatch(request -> request.getTicketType() == null)) {
            throw new InvalidPurchaseException();
        }
        if (Arrays.stream(requests).anyMatch(request -> request.getNoOfTickets() <= 0)) {
            throw new InvalidPurchaseException();
        }

        // Use long so adversarial int quantities cannot overflow the total.
        long total = Arrays.stream(requests)
                .mapToLong(TicketTypeRequest::getNoOfTickets)
                .sum();
        if (total > MAX_TICKETS) {
            throw new InvalidPurchaseException();
        }

        Map<Type, Integer> quantities = Arrays.stream(requests).collect(
                Collectors.groupingBy(TicketTypeRequest::getTicketType,
                        () -> new EnumMap<Type, Integer>(Type.class),
                        Collectors.summingInt(TicketTypeRequest::getNoOfTickets)));

        int adults = quantities.getOrDefault(Type.ADULT, 0);
        int infants = quantities.getOrDefault(Type.INFANT, 0);
        if (adults == 0) {
            throw new InvalidPurchaseException();
        }
        return new PurchaseOrder(quantities);
    }
}
