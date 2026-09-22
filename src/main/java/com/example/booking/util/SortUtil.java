package com.example.booking.util;

import com.example.booking.exception.BadRequestException;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class SortUtil {

    private SortUtil() {}

    public static Sort buildSort(String sortBy, String direction, Set<String> allowedFields) {
        if(sortBy == null || sortBy.isBlank()) {
            return Sort.unsorted();
        }
        if(!allowedFields.contains(sortBy)) {
            throw new BadRequestException(
                    "Invalid sort field: " + sortBy
            );
        }

        Sort.Direction sortDirection;
        try{
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid sort direction: " + direction
            );
        }
        return Sort.by(sortDirection, sortBy);
    }
}
