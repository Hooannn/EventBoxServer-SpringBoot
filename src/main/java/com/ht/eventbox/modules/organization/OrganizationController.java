package com.ht.eventbox.modules.organization;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.enums.OrganizationRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping(path = "/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;

    @GetMapping
    @RequiredPermissions({"read:organizations"})
    public ResponseEntity<Response<List<Organization>>> getAll() {
        var res = organizationService.getAll();
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @GetMapping("/me")
    @RequiredPermissions({"read:organizations"})
    public ResponseEntity<Response<List<Organization>>> getMyOrganizations(
            @RequestAttribute("sub") String sub
    ) {
        var res = organizationService.getByUserIdAndOrganizationRole(Long.valueOf(sub), OrganizationRole.OWNER);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @GetMapping("/{id}")
    @RequiredPermissions({"read:organizations"})
    public ResponseEntity<Response<Organization>> getById(
            @RequestAttribute("sub") String sub,
            @PathVariable Long id) {
        var res = organizationService.getById(id);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }
}
