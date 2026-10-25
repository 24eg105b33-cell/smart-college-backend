package com.smartcollege.demo.service;

import com.smartcollege.demo.dto.PageResponse;
import com.smartcollege.demo.dto.ResourceDto;
import com.smartcollege.demo.dto.ResourceRequest;
import com.smartcollege.demo.dto.ResourceStatusUpdateRequest;
import com.smartcollege.demo.entity.Resource;
import com.smartcollege.demo.entity.ResourceCategory;
import com.smartcollege.demo.entity.ResourceStatus;
import org.springframework.data.domain.Pageable;

public interface ResourceService {

    ResourceDto createResource(ResourceRequest request);

    PageResponse<ResourceDto> getAllResources(String search,
                                              ResourceCategory category,
                                              String location,
                                              ResourceStatus status,
                                              Boolean active,
                                              Pageable pageable);

    ResourceDto getResourceById(Long id);

    ResourceDto updateResource(Long id, ResourceRequest request);

    ResourceDto updateResourceStatus(Long id, ResourceStatusUpdateRequest request);

    ResourceDto deactivateResource(Long id);

    Resource getEntityById(Long id);
}
