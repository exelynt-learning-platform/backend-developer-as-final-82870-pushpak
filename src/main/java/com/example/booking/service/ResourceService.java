package com.example.booking.service;

import com.example.booking.dto.ResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.entity.Resource;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ResourceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {

    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "id", "name", "type", "available", "pricePerHour"
    );
    private final ResourceRepository resourceRepository;


    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = new Resource();

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setAvailable(request.getAvailable());
        resource.setPricePerHour(request.getPricePerHour());

        Resource savedResource = resourceRepository.save(resource);
        return mapToResponse(savedResource);
    }

    public List<Resource> getAllResources(int pageNo, int pageSize, String sortBy, String sortDir) {
        validatePagination(pageNo, pageSize);

        if(!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException(
                    "Invalid sort field. Allowed fields " + ALLOWED_SORT_FIELDS
            );
        }

        Pageable pageable;
        try{
            pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Sort direction must be ASC or DESC"
            );
        }

        return resourceRepository.findAll(pageable).getContent();
    }

    public ResourceResponse getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id " + id
                ));
        return mapToResponse(resource);
    }

    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with id " + id
                        ));
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setAvailable(request.getAvailable());
        resource.setPricePerHour(request.getPricePerHour());

        Resource updatedResource = resourceRepository.save(resource);
        return mapToResponse(updatedResource);
    }

    public void deleteResource(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with id: " + id
                        ));

        resourceRepository.delete(resource);
    }
    private ResourceResponse mapToResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.getAvailable(),
                resource.getPricePerHour()
        );
    }

    private void validatePagination(int pageNo, int pageSize) {
        if(pageNo < 0){
            throw new BadRequestException(
                    "Page number must be greater than or equal to 0"
            );
        }
        if(pageSize < 1) {
            throw new BadRequestException(
                    "Page size must be greater than 0"
            );
        }
    }
}
