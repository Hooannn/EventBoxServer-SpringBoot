package com.ht.eventbox.modules.organization;

import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.enums.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    List<Organization> findByUserOrganizationsUserIdAndUserOrganizationsRoleIs(Long userId, OrganizationRole organizationRole);

    Optional<Organization> findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(Long orgId, Long userId, OrganizationRole userOrganizations_role);
}
