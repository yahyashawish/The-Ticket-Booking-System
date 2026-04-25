public class BookingManager {

    private final IPaymentGateway paymentGateway;
    private final INotificationService notificationService;
    private final IEventRepository eventRepository;

    public BookingManager(IPaymentGateway paymentGateway,
                          INotificationService notificationService,
                          IEventRepository eventRepository) {
        this.paymentGateway = paymentGateway;
        this.notificationService = notificationService;
        this.eventRepository = eventRepository;
    }

    /**
     * Processes a ticket booking for the given event.
     *
     * @param userId  the ID of the user making the booking (must not be null or blank)
     * @param eventId the ID of the event to book (must not be null or blank)
     * @param amount  the payment amount (must be > 0)
     * @return true if the booking was successful, false otherwise
     */
    public boolean bookTicket(String userId, String eventId, double amount) {

        // US-02: Validate inputs — do nothing if invalid
        if (userId == null || userId.isBlank() ||
            eventId == null || eventId.isBlank() ||
            amount <= 0) {
            return false;
        }

        // US-03: Check availability — stop if sold out
        if (eventRepository.isSoldOut(eventId)) {
            return false;
        }

        // US-01: Process payment, persist booking, and notify user
        String transactionId = paymentGateway.processPayment(userId, amount);
        eventRepository.saveBooking(userId, eventId, transactionId);
        notificationService.sendConfirmation(userId, eventId, transactionId);

        return true;
    }

    // ---------------------------------------------------------------------------
    // Interfaces (defined as inner interfaces for single-file simplicity;
    // in a real project these would be separate files)
    // ---------------------------------------------------------------------------

    public interface IPaymentGateway {
        /**
         * Charges the user and returns a unique transaction ID.
         */
        String processPayment(String userId, double amount);
    }

    public interface INotificationService {
        /**
         * Sends a booking-confirmation message to the user.
         */
        void sendConfirmation(String userId, String eventId, String transactionId);
    }

    public interface IEventRepository {
        /**
         * Returns true if no tickets are available for the given event.
         */
        boolean isSoldOut(String eventId);

        /**
         * Persists the booking record.
         */
        void saveBooking(String userId, String eventId, String transactionId);
    }
}