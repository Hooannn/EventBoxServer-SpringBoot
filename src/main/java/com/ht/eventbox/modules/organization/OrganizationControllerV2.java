package com.ht.eventbox.modules.organization;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.QueryResponse;
import com.ht.eventbox.entities.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v2/organizations")
@RequiredArgsConstructor
public class OrganizationControllerV2 {
    private final OrganizationService organizationService;

    @GetMapping
    @RequiredPermissions({"read:organizations"})
    public ResponseEntity<QueryResponse<Organization>> getAll(Pageable pageable) {
        var res = organizationService.getAll(pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase())
        );
    }
}
