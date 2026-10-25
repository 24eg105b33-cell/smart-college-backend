package com.smartcollege.demo.service.impl;

import com.smartcollege.demo.dto.PageResponse;
import com.smartcollege.demo.dto.ResourceDto;
import com.smartcollege.demo.dto.ResourceRequest;
import com.smartcollege.demo.dto.ResourceStatusUpdateRequest;
import com.smartcollege.demo.entity.Resource;
import com.smartcollege.demo.entity.ResourceCategory;
import com.smartcollege.demo.entity.ResourceStatus;
import com.smartcollege.demo.exception.ConflictException;
import com.smartcollege.demo.exception.ResourceNotFoundException;
import com.smartcollege.demo.repository.ResourceRepository;
import com.smartcollege.demo.service.ResourceService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceServiceImpl(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    public ResourceDto createResource(ResourceRequest request) {
        String normalizedCode = request.getResourceCode().trim().toUpperCase();

        if (resourceRepository.existsByResourceCode(normalizedCode)) {
            throw new ConflictException("A resource with code '" + normalizedCode + "' already exists");
        }

        Resource resource = new Resource();
        resource.setResourceCode(normalizedCode);
        resource.setName(request.getName().trim());
        resource.setCategory(request.getCategory());
        resource.setLocation(request.getLocation().trim());
        resource.setCapacity(request.getCapacity());
        resource.setDescription(request.getDescription());
        resource.setStatus(request.getStatus() != null ? request.getStatus() : ResourceStatus.AVAILABLE);
        resource.setActive(request.getActive() != null ? request.getActive() : true);

        Resource saved = resourceRepository.save(resource);
        return ResourceDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ResourceDto> getAllResources(String search,
                                                     ResourceCategory category,
                                                     String location,
                                                     ResourceStatus status,
                                                     Boolean active,
                                                     Pageable pageable) {
        Specification<Resource> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate codeMatch = cb.like(cb.lower(root.get("resourceCode")), searchPattern);
                Predicate locationMatch = cb.like(cb.lower(root.get("location")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(nameMatch, codeMatch, locationMatch, descMatch));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (StringUtils.hasText(location)) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.trim().toLowerCase() + "%"));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Resource> page = resourceRepository.findAll(spec, pageable);
        return PageResponse.fromPage(page.map(ResourceDto::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceDto getResourceById(Long id) {
        return ResourceDto.fromEntity(getEntityById(id));
    }

    @Override
    public ResourceDto updateResource(Long id, ResourceRequest request) {
        Resource resource = getEntityById(id);
        String normalizedCode = request.getResourceCode().trim().toUpperCase();

        if (resourceRepository.existsByResourceCodeAndIdNot(normalizedCode, id)) {
            throw new ConflictException("A resource with code '" + normalizedCode + "' already exists");
        }

        resource.setResourceCode(normalizedCode);
        resource.setName(request.getName().trim());
        resource.setCategory(request.getCategory());
        resource.setLocation(request.getLocation().trim());
        resource.setCapacity(request.getCapacity());
        resource.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            resource.setStatus(request.getStatus());
        }
        if (request.getActive() != null) {
            resource.setActive(request.getActive());
        }

        Resource updated = resourceRepository.save(resource);
        return ResourceDto.fromEntity(updated);
    }

    @Override
    public ResourceDto updateResourceStatus(Long id, ResourceStatusUpdateRequest request) {
        Resource resource = getEntityById(id);
        resource.setStatus(request.getStatus());

        if (request.getActive() != null) {
            resource.setActive(request.getActive());
        } else if (request.getStatus() == ResourceStatus.OUT_OF_SERVICE) {
            resource.setActive(false);
        } else if (request.getStatus() == ResourceStatus.AVAILABLE) {
            resource.setActive(true);
        }

        Resource updated = resourceRepository.save(resource);
        return ResourceDto.fromEntity(updated);
    }

    @Override
    public ResourceDto deactivateResource(Long id) {
        Resource resource = getEntityById(id);
        resource.setActive(false);
        resource.setStatus(ResourceStatus.OUT_OF_SERVICE);

        Resource updated = resourceRepository.save(resource);
        return ResourceDto.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getEntityById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }
}
