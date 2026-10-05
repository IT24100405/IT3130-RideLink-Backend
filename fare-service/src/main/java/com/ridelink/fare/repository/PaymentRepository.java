package com.ridelink.fare.repository;

import com.ridelink.fare.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByRideId(String rideId);

    Optional<Payment> findByReceiptNumber(String receiptNumber);

    List<Payment> findByPassengerId(String passengerId);

    List<Payment> findByDriverId(String driverId);

    boolean existsByRideId(String rideId);
}
