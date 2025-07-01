package com.ht.eventbox.modules.organization;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.enums.OrganizationRole;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;

    public List<Organization> getAll() {
        return organizationRepository.findAll();
    }

    public List<Organization> getByUserIdAndOrganizationRole(Long userId, OrganizationRole organizationRole) {
        return organizationRepository.findByUserOrganizationsUserIdAndUserOrganizationsRoleIs(userId, organizationRole);
    }

    public Organization getById(Long id) {
        return organizationRepository.findById(id).orElseThrow(() ->
                new HttpException(Constant.ErrorCode.ORGANIZATION_NOT_FOUND, HttpStatus.NOT_FOUND)
        );
    }
}
