package com.smartcollege.demo.controller;

import com.smartcollege.demo.dto.ApiResponse;
import com.smartcollege.demo.dto.PageResponse;
import com.smartcollege.demo.dto.ResourceDto;
import com.smartcollege.demo.dto.ResourceRequest;
import com.smartcollege.demo.dto.ResourceStatusUpdateRequest;
import com.smartcollege.demo.entity.ResourceCategory;
import com.smartcollege.demo.entity.ResourceStatus;
import com.smartcollege.demo.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ResourceDto>>> getAllResources(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ResourceCategory category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ResourceStatus status,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<ResourceDto> response = resourceService.getAllResources(search, category, location, status, active, pageable);
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceDto>> getResourceById(@PathVariable Long id) {
        ResourceDto resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(ApiResponse.success("Resource retrieved successfully", resource));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResourceDto>> createResource(@Valid @RequestBody ResourceRequest request) {
        ResourceDto created = resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResourceDto>> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request
    ) {
        ResourceDto updated = resourceService.updateResource(id, request);
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResourceDto>> updateResourceStatus(
            @PathVariable Long id,
            @Valid @RequestBody ResourceStatusUpdateRequest request
    ) {
        ResourceDto updated = resourceService.updateResourceStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Resource status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResourceDto>> deactivateResource(@PathVariable Long id) {
        ResourceDto deactivated = resourceService.deactivateResource(id);
        return ResponseEntity.ok(ApiResponse.success("Resource deactivated successfully", deactivated));
    }
}
