package com.example.booking.service;

import com.example.booking.dto.ReservationPageResponse;
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
import com.example.booking.util.SortUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ReservationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "startTime",
            "endTime",
            "price",
            "reservationStatus"
    );

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
        if(!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException(
                    "End time must be after start time"
            );
        }
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

    public ReservationPageResponse getAllReservations(String username, boolean isAdmin, int page, int size, ReservationStatus reservationStatus, BigDecimal minPrice, BigDecimal maxPrice, String sortBy, String direction) {
        validatePaginationAndFilters(page, size, minPrice, maxPrice);
        Sort sort = SortUtil.buildSort(sortBy, direction, ALLOWED_SORT_FIELDS);

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Reservation> reservationPage;
        if(isAdmin) {
            reservationPage = reservationRepository.findReservations(reservationStatus, minPrice, maxPrice, pageable);
        } else {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Authenticated user not found"
                            )
                    );

            reservationPage = reservationRepository.findUserWithFilter(user.getId(), reservationStatus, minPrice, maxPrice, pageable);
        }

        List<ReservationResponse> reservations =
                reservationPage.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return new ReservationPageResponse(
                reservations,
                reservationPage.getNumber(),
                reservationPage.getSize(),
                reservationPage.getTotalElements(),
                reservationPage.getTotalPages()
        );
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
        if(!isAdmin) {
            throw new AccessDeniedException(
                    "Only admin can update reservations"
            );
        }

        if(!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException(
                    "End time must be after start time"
            );
        }
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() ->
                        new ReservationNotFoundException(
                                "Reservation not found with id: " + id
                        )
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
        if(!isAdmin) {
            throw new AccessDeniedException(
                    "Only administrators can update reservations"
            );
        }
        Reservation reservation = reservationRepository.findById(id)
                        .orElseThrow(() ->
                                new ReservationNotFoundException(
                                        "Reservation not found with id:" + id
                                )
                        );
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

    private BigDecimal calculatePrice(BigDecimal pricePerHour, LocalDateTime startTime, LocalDateTime endTime) {
            long minutes = Duration.between(
                    startTime, endTime
            ).toMinutes();

            if(minutes <= 0) {
                throw new BadRequestException(
                        "Reservation duration must be greater than zero"
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

    private void validatePaginationAndFilters(
            int page,
            int size,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        if (page < 0) {
            throw new BadRequestException(
                    "Page must be greater than or equal to zero");
        }

        if (size <= 0) {
            throw new BadRequestException(
                    "Size must be greater than zero");
        }

        if(size > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Page size must not exceed " + MAX_PAGE_SIZE
            );
        }

        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    "Minimum price cannot be negative");
        }

        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    "Maximum price cannot be negative");
        }

        if (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new BadRequestException(
                    "Minimum price cannot be greater than maximum price");
        }
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
