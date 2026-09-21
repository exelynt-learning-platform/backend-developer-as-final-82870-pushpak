package com.example.booking.service;

import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ReservationNotFoundException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    private static final List<ReservationStatus> ACTIVE_STATUSES = List.of(
            ReservationStatus.PENDING,
            ReservationStatus.CONFIRMED
    );

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository, ResourceRepository resourceRepository, UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public ReservationResponse createReservation(ReservationRequest request, String username) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new BadRequestException("Authenticated user not found")
                );

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with id: " + request.getResourceId()
                        )
                );
        if(!Boolean.TRUE.equals(resource.getAvailable())){
            throw new BadRequestException("Resource is currently unavailable");
        }

        boolean isOverlapping = reservationRepository.existsOverlappingReservation(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                ACTIVE_STATUSES
        );
        if(isOverlapping) {
            throw new ConflictException("Resource is already reserved for the requested time");
        }
        BigDecimal price = calculatePrice(resource.getPricePerHour(), request.getStartTime(), request.getEndTime());

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(price);
        reservation.setReservationStatus(ReservationStatus.PENDING);

        Reservation savedReservation = reservationRepository.save(reservation);
        return mapToResponse(savedReservation);
    }

    public List<Reservation> getAllReservations(String username, boolean isAdmin, int pageNo, int pageSize, ReservationStatus reservationStatus, BigDecimal minPrice, BigDecimal maxPrice, String sortBy, String sortDir) {
        validatePagination(pageNo, pageSize);
        validatePriceRange(minPrice, maxPrice);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.fromString(sortDir), sortBy));

        if(isAdmin) {
            return reservationRepository.findReservations(reservationStatus, minPrice, maxPrice, pageable).getContent();
        } else {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Authenticated user not found"
                            )
                    );

            return reservationRepository.findUserWithFilter(user.getId(), reservationStatus, minPrice, maxPrice, pageable).getContent();
        }
    }

    public ReservationResponse getReservationById(Long id, String username, boolean isAdmin) {
        if(isAdmin) {
            return mapToResponse(reservationRepository.findById(id)
                    .orElseThrow(() ->
                            new ReservationNotFoundException(
                                    "Reservation not found with id: " + id
                            )
                    )
            );
        } else {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Authenticated user not found"
                            )
                    );
            return mapToResponse(reservationRepository.findByIdAndUserId(id, user.getId())
                    .orElseThrow(() ->
                            new ReservationNotFoundException(
                                    "Reservation not found with id: " + id
                            )
                    )
            );
        }
    }

    public ReservationResponse updateReservation(Long id, ReservationRequest request, String username, boolean isAdmin) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        Reservation reservation = getReservationEntity(id, username, isAdmin);

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with id: " + request.getResourceId()
                        )
                );

        if(!Boolean.TRUE.equals(resource.getAvailable())){
            throw new BadRequestException("Resource is currently unavailable");
        }

        boolean isOverlapping = reservationRepository.existsOverlappingReservationExcludingId(
                resource.getId(),
                id,
                request.getStartTime(),
                request.getEndTime(),
                ACTIVE_STATUSES
                );

        if(isOverlapping) {
            throw new ConflictException("Resource is already reserved for the requested time");
        }

        BigDecimal price = calculatePrice(resource.getPricePerHour(), request.getStartTime(), request.getEndTime());
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(price);

        return mapToResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse updateReservationStatus(Long id, ReservationStatus newReservationStatus) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() ->
                        new ReservationNotFoundException(
                                "Reservation not found with id: " + id
                        )
                );

        ReservationStatus currReservationStatus = reservation.getReservationStatus();

        if(!isValidStatusTransition(currReservationStatus, newReservationStatus)) {
            throw new BadRequestException(
                    "Invalid status transition from"
                    + currReservationStatus
                    + " to "
                    + newReservationStatus
            );
        }

        reservation.setReservationStatus(newReservationStatus);
        return mapToResponse(reservationRepository.save(reservation));
    }

    public void deleteReservation(Long id, String username, boolean isAdmin) {
        Reservation reservation = getReservationEntity(id, username, isAdmin);
        reservationRepository.delete(reservation);
    }

    private boolean isValidStatusTransition(ReservationStatus currReservationStatus, ReservationStatus newReservationStatus) {
        return switch (currReservationStatus) {
            case PENDING ->
                newReservationStatus == ReservationStatus.CONFIRMED
                        || newReservationStatus == ReservationStatus.CANCELLED;

            case CONFIRMED ->
                newReservationStatus == ReservationStatus.CANCELLED;

            case CANCELLED -> false;
        };
    }

    private Reservation getReservationEntity(Long id, String username, boolean isAdmin) {
        if(isAdmin) {
            return reservationRepository.findById(id)
                    .orElseThrow(() ->
                            new ReservationNotFoundException(
                                    "Reservation not found with id: " + id

                            )
                    );
        } else {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Authenticated user not found"
                            )
                    );

            return reservationRepository.findByIdAndUserId(id, user.getId())
                    .orElseThrow(() ->
                            new ReservationNotFoundException(
                                    "Reservation not found with id: " + id
                            )
                    );
        }
    }
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if(!endTime.isAfter(startTime)) {
            throw new BadRequestException(
                    "End time must be after start time"
            );
        }
    }

    private void validatePagination(int pageNo, int pageSize) {
        if(pageNo < 0) {
            throw new BadRequestException(
                    "Page must be greater than or equal to 0"
            );
        }
        if(pageSize < 1) {
            throw new BadRequestException(
                    "Page size must be greater than or equal to 1"
            );
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if(minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum price cannot be negative");
        }
        if(maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Maximum price cannot be negative");
        }
        if(minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Minimum price cannot be greater than maximum price");
        }
    }

    private BigDecimal calculatePrice(BigDecimal pricePerHour, LocalDateTime startTime, LocalDateTime endTime) {
            long minutes = Duration.between(
                    startTime, endTime
            ).toMinutes();

            if(minutes <= 0) {
                throw new BadRequestException(
                        "Reservation duration must be greater then zero"
                );
            }
            BigDecimal hours = BigDecimal.valueOf(minutes)
                    .divide(
                            BigDecimal.valueOf(60),
                            2,
                            RoundingMode.HALF_UP
                    );
            return pricePerHour.multiply(hours)
                    .setScale(2, RoundingMode.HALF_UP);
    }


    private ReservationResponse mapToResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getResource().getId(),
                reservation.getResource().getName(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPrice(),
                reservation.getReservationStatus()
        );
    }
}
