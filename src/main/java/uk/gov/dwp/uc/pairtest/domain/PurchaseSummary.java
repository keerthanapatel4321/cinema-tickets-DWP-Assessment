package uk.gov.dwp.uc.pairtest.domain;

/** Price and seats for a validated purchase, expressed as integer pounds. */
public record PurchaseSummary(int totalPriceInPounds, int seatsToReserve) { }
