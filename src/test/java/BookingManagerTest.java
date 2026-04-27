import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingManager covering all three user stories:
 *   US-01 – Happy Path
 *   US-02 – Invalid Input Path
 *   US-03 – Sold-Out Path
 */
@ExtendWith(MockitoExtension.class)
public class BookingManagerTest {

    // -----------------------------------------------------------------------
    // Mocks (Test Doubles)
    // -----------------------------------------------------------------------

    @Mock
    private BookingManager.IPaymentGateway paymentGateway;

    @Mock
    private BookingManager.INotificationService notificationService;

    @Mock
    private BookingManager.IEventRepository eventRepository;

    // System Under Test
    private BookingManager bookingManager;

    @BeforeEach
    void setUp() {
        bookingManager = new BookingManager(paymentGateway, notificationService, eventRepository);
    }

    // -----------------------------------------------------------------------
    // US-01: Happy Path
    // -----------------------------------------------------------------------

    /**
     * US-01: Given valid input, available event, and successful payment,
     * saveBooking() and sendConfirmation() must each be called exactly once.
     */
    @Test
    void us01_happyPath_saveBookingAndSendConfirmationCalledOnce() {
        // Arrange
        String userId    = "user-123";
        String eventId   = "event-456";
        double amount    = 99.99;
        String txId      = "txn-abc";

        when(eventRepository.isSoldOut(eventId)).thenReturn(false);
        when(paymentGateway.processPayment(userId, amount)).thenReturn(txId);

        // Act
        boolean result = bookingManager.bookTicket(userId, eventId, amount);

        // Assert
        assertTrue(result, "bookTicket should return true on the happy path");
        verify(eventRepository,       times(1)).isSoldOut(eventId);
        verify(paymentGateway,        times(1)).processPayment(userId, amount);
        verify(eventRepository,       times(1)).saveBooking(userId, eventId, txId);
        verify(notificationService,   times(1)).sendConfirmation(userId, eventId, txId);
    }

    // -----------------------------------------------------------------------
    // US-02: Invalid Input Paths
    // -----------------------------------------------------------------------

    /**
     * US-02a: Null userId — no downstream methods should be called.
     */
    @Test
    void us02_nullUserId_noMethodsCalled() {
        boolean result = bookingManager.bookTicket(null, "event-456", 50.0);

        assertFalse(result);
        verify(paymentGateway,      never()).processPayment(anyString(), anyDouble());
        verify(eventRepository,     never()).saveBooking(anyString(), anyString(), anyString());
        verify(notificationService, never()).sendConfirmation(anyString(), anyString(), anyString());
    }

    /**
     * US-02b: Blank eventId — no downstream methods should be called.
     */
    @Test
    void us02_blankEventId_noMethodsCalled() {
        boolean result = bookingManager.bookTicket("user-123", "   ", 50.0);

        assertFalse(result);
        verify(paymentGateway,      never()).processPayment(anyString(), anyDouble());
        verify(eventRepository,     never()).saveBooking(anyString(), anyString(), anyString());
        verify(notificationService, never()).sendConfirmation(anyString(), anyString(), anyString());
    }

    /**
     * US-02c: Non-positive amount — no downstream methods should be called.
     */
    @Test
    void us02_zeroAmount_noMethodsCalled() {
        boolean result = bookingManager.bookTicket("user-123", "event-456", 0);
        assertFalse(result);
        verify(paymentGateway,      never()).processPayment(anyString(), anyDouble());
        verify(eventRepository,     never()).saveBooking(anyString(), anyString(), anyString());
        verify(notificationService, never()).sendConfirmation(anyString(), anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // US-03: Sold-Out Path
    // -----------------------------------------------------------------------

    /**
     * US-03: When the event is sold out, isSoldOut() is called but all
     * subsequent methods (processPayment, saveBooking, sendConfirmation) are never called.
     */
    @Test
    void us03_soldOut_onlyIsSoldOutCalledNothingElse() {
        // Arrange
        String userId  = "user-123";
        String eventId = "event-sold-out";

        when(eventRepository.isSoldOut(eventId)).thenReturn(true);

        // Act
        boolean result = bookingManager.bookTicket(userId, eventId, 75.0);

        // Assert
        assertFalse(result, "bookTicket should return false when event is sold out");
        verify(eventRepository,     times(1)).isSoldOut(eventId);
        verify(paymentGateway,      never()).processPayment(anyString(), anyDouble());
        verify(eventRepository,     never()).saveBooking(anyString(), anyString(), anyString());
        verify(notificationService, never()).sendConfirmation(anyString(), anyString(), anyString());
    }
}