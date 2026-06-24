package com.ht.eventbox.modules.user;

import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.entities.Permission;
import com.ht.eventbox.entities.Role;
import com.ht.eventbox.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.PublicKey;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserControllerV2.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class UserControllerV2Tests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private com.ht.eventbox.filter.JwtService jwtService;

    @MockBean
    private PublicKey atPublicKey;

    @Test
    void getAll_shouldReturnPagedUsers() throws Exception {
        when(userService.getAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleUser(9L)), PageRequest.of(1, 10), 25));

        mockMvc.perform(get("/api/v2/users")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(9L))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1));
    }

    @Test
    void getAllRoles_shouldReturnPagedRoles() throws Exception {
        when(userService.getAllRoles(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleRole(3L)), PageRequest.of(0, 15), 31));

        mockMvc.perform(get("/api/v2/users/roles")
                        .param("page", "0")
                        .param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(3L))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(31))
                .andExpect(jsonPath("$.size").value(15))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(1));
    }

    @Test
    void getAllPermissions_shouldReturnPagedPermissions() throws Exception {
        when(userService.getAllPermissions(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(samplePermission(5L)), PageRequest.of(2, 7), 21));

        mockMvc.perform(get("/api/v2/users/roles/permissions")
                        .param("page", "2")
                        .param("size", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(5L))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.size").value(7))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(1));
    }

    private User sampleUser(Long id) {
        return User.builder()
                .id(id)
                .build();
    }

    private Role sampleRole(Long id) {
        return Role.builder()
                .id(id)
                .build();
    }

    private Permission samplePermission(Long id) {
        return Permission.builder()
                .id(id)
                .build();
    }
}
