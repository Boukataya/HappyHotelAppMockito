package com.mockitotutorial.happyhotel.booking.services;

import com.mockitotutorial.happyhotel.booking.BusinessException;
import com.mockitotutorial.happyhotel.booking.doa.BookingDAO;
import com.mockitotutorial.happyhotel.booking.entities.BookingRequest;
import com.mockitotutorial.happyhotel.booking.entities.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private BookingService bookingService;
    private PaymentService paymentServiceMock;
    private RoomService roomServiceMock;
    private BookingDAO bookingDAOMock;
    private MailSenderService mailSenderServiceMock;

    @BeforeEach
    void setup() {
        this.paymentServiceMock = mock(PaymentService.class);
        this.roomServiceMock = mock(RoomService.class);
        this.bookingDAOMock = mock(BookingDAO.class);
        this.mailSenderServiceMock = mock(MailSenderService.class);

        this.bookingService = new BookingService(paymentServiceMock, roomServiceMock, bookingDAOMock, mailSenderServiceMock);

        System.out.println("Mock Default List values is " + roomServiceMock.getAvailableRooms());
        System.out.println("Mock Default String value is " + roomServiceMock.findAvailableRoomId(null));
        System.out.println("Mock Default Integer value is " + roomServiceMock.getRoomCount());
    }

    @Test
    void should_CalculateCorrectPrice_When_CorrectInput() {
        // Given
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2022, 12, 1),
                LocalDate.of(2022, 12, 5),
                2, false);
        double expected = 4 * 2 * 50;
        // When
        double actual = bookingService.calculatePrice(bookingRequest);
        // Then
        assertEquals(expected, actual);

    }

    @Test
    void should_CountAvailablePlaces() {
        int expected = 0;
        int actual = roomServiceMock.getRoomCount();
        assertEquals(expected, actual);
    }

    @Test
    void should_CountAvailablePlaces_When_OneRoomAvailable() {
        when(roomServiceMock.getAvailableRooms())
                .thenReturn(Collections.singletonList(new Room("Room 1", 6)));
        int expected = 6;
        int actual = bookingService.getAvailablePlaceCount();
        assertEquals(expected, actual);
    }

    @Test
    void should_CountAvailablePlaces_When_MultipleRoomAvailable() {
        List<Room> roomList = Arrays.asList(
                new Room("Room 1", 2),
                new Room("Room 2", 4)
        );
        when(roomServiceMock.getAvailableRooms())
                .thenReturn(roomList);
        int expected = 6;
        int actual = bookingService.getAvailablePlaceCount();
        assertEquals(expected, actual);
    }

    @Test
    void should_CountAvailablePlaces_When_CalledMultipleTimes() {
        when(roomServiceMock.getAvailableRooms())
                .thenReturn(Collections.singletonList(new Room("Room 1", 6)))
                .thenReturn(Collections.emptyList());
        int expectedFirst = 6;
        int expectedSecond = 0;
        int actualFirstCall = bookingService.getAvailablePlaceCount();
        int actualSecondCall = bookingService.getAvailablePlaceCount();
        assertAll(
                () -> assertEquals(expectedFirst, actualFirstCall),
                () -> assertEquals(expectedSecond, actualSecondCall)
        );

    }

    @Test
    void should_ThrowException_When_NoRoomAvailable() {
        BookingRequest bookingRequest = new BookingRequest(
                "1",
                LocalDate.of(2022, 12, 1),
                LocalDate.of(2022, 12, 5),
                2, false);

        when(roomServiceMock.findAvailableRoomId(bookingRequest))
                .thenThrow(BusinessException.class);
        Executable executable = () -> bookingService.makeBooking(bookingRequest);
        assertThrows(BusinessException.class, executable);


    }

    @Test
    void should_NotCompleteBooking_When_PriceTooHigh() {
        BookingRequest bookingRequest = new BookingRequest(
                "1",
                LocalDate.of(2022, 12, 1),
                LocalDate.of(2022, 12, 5),
                2, true);

        when(this.paymentServiceMock.pay(any(), anyDouble()))
                .thenThrow(BusinessException.class);
        Executable executable = () -> bookingService.makeBooking(bookingRequest);
        assertThrows(BusinessException.class, executable);


    }
}