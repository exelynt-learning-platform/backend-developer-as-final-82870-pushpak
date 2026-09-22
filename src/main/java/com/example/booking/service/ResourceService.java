package com.example.booking.service;

import com.example.booking.dto.ResourcePageResponse;
import com.example.booking.dto.ResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.entity.Resource;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.util.SortUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ResourceService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
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

    public ResourcePageResponse getAllResources(int page, int size, String sortBy, String direction) {
        if(page < 0){
            throw new BadRequestException(
                    "Page number must be greater than or equal to 0"
            );
        }
        if(size < 1) {
            throw new BadRequestException(
                    "Page size must be greater than 0"
            );
        }
        if(size > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Page size must not exceed " + MAX_PAGE_SIZE
            );
        }
        if(!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException(
                    "Invalid sort field. Allowed fields " + ALLOWED_SORT_FIELDS
            );
        }

        Sort sort = SortUtil.buildSort(sortBy, direction, ALLOWED_SORT_FIELDS);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Resource> resources = resourceRepository.findAll(pageable);

        List<ResourceResponse> content = resources.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
        return new ResourcePageResponse(
                content,
                resources.getNumber(),
                resources.getSize(),
                resources.getTotalElements(),
                resources.getTotalPages()
        );
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

}
