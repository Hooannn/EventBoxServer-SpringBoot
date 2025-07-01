package com.ht.eventbox.modules.organization;

import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.enums.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    List<Organization> findByUserOrganizationsUserIdAndUserOrganizationsRoleIs(Long userId, OrganizationRole organizationRole);
}
