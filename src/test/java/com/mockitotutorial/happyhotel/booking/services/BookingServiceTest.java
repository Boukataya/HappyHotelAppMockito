package com.mockitotutorial.happyhotel.booking.services;

import com.mockitotutorial.happyhotel.booking.BusinessException;
import com.mockitotutorial.happyhotel.booking.CurrencyConverter;
import com.mockitotutorial.happyhotel.booking.doa.BookingDAO;
import com.mockitotutorial.happyhotel.booking.entities.BookingRequest;
import com.mockitotutorial.happyhotel.booking.entities.Room;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @InjectMocks
    private BookingService bookingService;
    @Mock
    private PaymentService paymentServiceMock;
    @Mock
    private RoomService roomServiceMock;
    @Mock
    private BookingDAO bookingDAOMock;
    @Mock
    private MailSenderService mailSenderServiceMock;
    @Captor
    private ArgumentCaptor<Double> doubleCaptor;

    /**
     * @BeforeEach void setup() {
     * this.paymentServiceMock = mock(PaymentService.class);
     * this.roomServiceMock = mock(RoomService.class);
     * this.bookingDAOMock = mock(BookingDAO.class);
     * this.mailSenderServiceMock = mock(MailSenderService.class);
     * <p>
     * this.bookingService = new BookingService(paymentServiceMock, roomServiceMock, bookingDAOMock, mailSenderServiceMock);
     * <p>
     * System.out.println("Mock Default List values is " + roomServiceMock.getAvailableRooms());
     * System.out.println("Mock Default String value is " + roomServiceMock.findAvailableRoomId(null));
     * System.out.println("Mock Default Integer value is " + roomServiceMock.getRoomCount());
     * }
     */

    @Test
    void should_CalculateCorrectPrice_When_CorrectInput() {
        // Given
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 2, false);
        double expected = 4 * 2 * 50;
        // When
        double actual = bookingService.calculatePrice(bookingRequest);
        // Then
//        assertEquals(expected, actual);
        Assertions.assertThat(expected).isEqualTo(actual);

    }

    @Test
    void should_CountAvailablePlaces() {
        int expected = 0;
        int actual = roomServiceMock.getRoomCount();
        assertEquals(expected, actual);
    }

    @Test
    void should_CountAvailablePlaces_When_OneRoomAvailable() {
        when(roomServiceMock.getAvailableRooms()).thenReturn(Collections.singletonList(new Room("Room 1", 6)));
        int expected = 6;
        int actual = bookingService.getAvailablePlaceCount();
        assertEquals(expected, actual);
    }

    @Test
    void should_CountAvailablePlaces_When_MultipleRoomAvailable() {
        List<Room> roomList = Arrays.asList(new Room("Room 1", 2), new Room("Room 2", 4));
        when(roomServiceMock.getAvailableRooms()).thenReturn(roomList);
        int expected = 6;
        int actual = bookingService.getAvailablePlaceCount();
        assertEquals(expected, actual);
    }

    @Test
    void should_CountAvailablePlaces_When_CalledMultipleTimes() {
        when(roomServiceMock.getAvailableRooms()).thenReturn(Collections.singletonList(new Room("Room 1", 6))).thenReturn(Collections.emptyList());
        int expectedFirst = 6;
        int expectedSecond = 0;
        int actualFirstCall = bookingService.getAvailablePlaceCount();
        int actualSecondCall = bookingService.getAvailablePlaceCount();
        assertAll(() -> assertEquals(expectedFirst, actualFirstCall), () -> assertEquals(expectedSecond, actualSecondCall));

    }

    @Test
    void should_ThrowException_When_NoRoomAvailable() {
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 2, false);

        when(roomServiceMock.findAvailableRoomId(bookingRequest)).thenThrow(BusinessException.class);
        Executable executable = () -> bookingService.makeBooking(bookingRequest);
        assertThrows(BusinessException.class, executable);

    }

    @Test
    void should_NotCompleteBooking_When_PriceTooHigh() {
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 2, true);

        when(this.paymentServiceMock.pay(any(), anyDouble())).thenThrow(BusinessException.class);
        Executable executable = () -> bookingService.makeBooking(bookingRequest);
        assertThrows(BusinessException.class, executable);

    }

    @Test
    void should_InvokePayment_when_Prepaid() {
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 2, true);
        bookingService.makeBooking(bookingRequest);
        verify(paymentServiceMock, times(1)).pay(bookingRequest, 400.0);
        verifyNoMoreInteractions(paymentServiceMock);
    }

    @Test
    void should_NotInvokePayment_when_Prepaid() {
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 2, false);
        bookingService.makeBooking(bookingRequest);
        verify(paymentServiceMock, never()).pay(any(), anyDouble());
        verifyNoInteractions(paymentServiceMock);
    }

    @Test
    void should_MakeBooking_When_InputOk() {
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 2, false);
        String bookingId = bookingService.makeBooking(bookingRequest);
        verify(bookingDAOMock).save(bookingRequest);
        System.out.println("bookingId = " + bookingId);
    }

    @Test
    void should_CancelBooking_When_InputOk() {
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 2, true);
        bookingRequest.setRoomId("123");
        String bookingId = "1";

        // If Mock is Used
        when(bookingDAOMock.get(bookingId)).thenReturn(bookingRequest);

        // If Spy is Used
//        doReturn(bookingRequest).when(bookingDAOMock).get(bookingId);
        bookingService.cancelBooking(bookingId);
    }

    @Test
    void should_ThrowException_When_MailNotReady() {
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 2, false);
        doThrow(new BusinessException()).when(mailSenderServiceMock).sendBookingConfirmation(any());
        Executable executable = () -> bookingService.makeBooking(bookingRequest);
        assertThrows(BusinessException.class, executable);
    }

    @Test
    void should_NotThrowException_When_MailNotReady() {
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 2, false);
        doNothing().when(mailSenderServiceMock).sendBookingConfirmation(any());
        bookingService.makeBooking(bookingRequest);
//        assertThrows(BusinessException.class, executable);
    }

    @Test
    void should_PayCorrectPrice_When_InputOk() {
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 3, true);
        bookingService.makeBooking(bookingRequest);
        verify(paymentServiceMock).pay(eq(bookingRequest), doubleCaptor.capture());
        double roomPrice = doubleCaptor.getValue();
        System.out.println("roomPrice = " + roomPrice);

    }

    @Test
    void should_PayCorrectPrice_When_MultipleCalls() {
        BookingRequest bookingRequest1 = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 2, true);
        BookingRequest bookingRequest2 = new BookingRequest("1", LocalDate.of(2022, 12, 6), LocalDate.of(2022, 12, 8), 2, true);

        bookingService.makeBooking(bookingRequest1);
        bookingService.makeBooking(bookingRequest2);

        verify(paymentServiceMock, times(2)).pay(any(), doubleCaptor.capture());
        List<Double> priceRooms = doubleCaptor.getAllValues();
        System.out.println("priceRooms = " + priceRooms);
    }

    @Test
    void StrictStabbing() {
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 5), 3, false);
        // Strict Stabbing is simply a piece of unnecessary code
        // Use Lenient When u have Strict Stabbing
        lenient().when(paymentServiceMock.pay(any(), anyDouble())).thenReturn("1");

        bookingService.makeBooking(bookingRequest);

    }

    @Test
    void testingStaticMethod() {
        try (MockedStatic<CurrencyConverter> mockedConverter = mockStatic(CurrencyConverter.class)) {
            BookingRequest bookingRequest = new BookingRequest(
                    "1",
                    LocalDate.of(2022, 12, 1),
                    LocalDate.of(2022, 12, 5),
                    2, false);
            double expected = 400.00 * 0.8;
            mockedConverter.when(() -> CurrencyConverter.toEuro(anyDouble()))
                    .thenAnswer(invocationOnMock -> (double) invocationOnMock.getArgument(0) * 0.8);
            double actual = bookingService.calculatePriceEuro(bookingRequest);
            assertEquals(expected, actual);
        }
    }

}