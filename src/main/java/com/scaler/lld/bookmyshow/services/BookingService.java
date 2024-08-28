package com.scaler.lld.bookmyshow.services;

import com.scaler.lld.bookmyshow.dtos.CreateBookingDTO;
import com.scaler.lld.bookmyshow.models.Booking;
import com.scaler.lld.bookmyshow.models.Customer;
import com.scaler.lld.bookmyshow.models.ShowSeat;
import com.scaler.lld.bookmyshow.repositories.interfaces.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingService {

    @Autowired
    private ShowSeatService showSeatService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CustomerService customerService;

    public Booking createBooking(CreateBookingDTO request) {
        //Validate if booking is open
        if (!request.getShow().isShowPending()) {
            throw new RuntimeException("Booking for this movie is closed");
        }

        // ============== Critical Section Start =================
        synchronized (this) {
            //Check if seat is available
            boolean isOccupied = checkIfSeatIsOccupied(request.getShowSeats());
            if (isOccupied) {
                throw new RuntimeException("Seat is already booked");
            }

            //Go ahead with booking
            //Mark the seats as FILLED
            for (ShowSeat showSeat : request.getShowSeats()) {
                showSeat.setOccupied(true);
                showSeatService.save(showSeat);
            }
        }
        // ============= Critical Section End =====================

        //Create and persist a booking
        Customer customer = customerService.getCustomer(request.getCustomerId());
        Booking newBooking = new Booking(customer,request.getShow());
        newBooking.setSeatsBooked(request.getShowSeats());
        bookingRepository.save(newBooking);
    }

    private boolean checkIfSeatIsOccupied(List<ShowSeat> seats) {
        for (ShowSeat seat : seats) {
            if (showSeatService.isOccupied(seat)) {
                return true;
            }
        }
        return false;
    }

}
