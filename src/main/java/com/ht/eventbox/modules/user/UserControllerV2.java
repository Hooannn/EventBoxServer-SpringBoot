package com.ht.eventbox.modules.user;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.QueryResponse;
import com.ht.eventbox.entities.Permission;
import com.ht.eventbox.entities.Role;
import com.ht.eventbox.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v2/users")
@RequiredArgsConstructor
public class UserControllerV2 {
    private final UserService userService;

    @GetMapping
    @RequiredPermissions({"read:users"})
    public ResponseEntity<QueryResponse<User>> getAll(
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        var res = userService.getAll(search, pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase())
        );
    }

    @GetMapping("/roles")
    @RequiredPermissions({"read:roles"})
    public ResponseEntity<QueryResponse<Role>> getAllRoles(
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        var res = userService.getAllRoles(search, pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase())
        );
    }

    @GetMapping("/roles/permissions")
    @RequiredPermissions({"read:permissions"})
    public ResponseEntity<QueryResponse<Permission>> getAllPermissions(
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        var res = userService.getAllPermissions(search, pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase())
        );
    }
}
