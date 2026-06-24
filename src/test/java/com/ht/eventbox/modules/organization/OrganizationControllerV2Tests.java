package com.ht.eventbox.modules.organization;

import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.entities.Organization;
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

@WebMvcTest(OrganizationControllerV2.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class OrganizationControllerV2Tests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private com.ht.eventbox.filter.JwtService jwtService;

    @MockBean
    private PublicKey atPublicKey;

    @Test
    void getAll_shouldReturnPagedOrganizations() throws Exception {
        when(organizationService.getAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleOrganization(9L)), PageRequest.of(2, 5), 18));

        mockMvc.perform(get("/api/v2/organizations")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(9L))
                .andExpect(jsonPath("$.totalPages").value(4))
                .andExpect(jsonPath("$.totalElements").value(18))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(1));
    }

    private Organization sampleOrganization(Long id) {
        return Organization.builder()
                .id(id)
                .build();
    }
}
