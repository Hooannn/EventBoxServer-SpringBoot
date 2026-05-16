package com.ht.eventbox.modules.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.entities.UserOrganization;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.organization.dtos.AddMemberDto;
import com.ht.eventbox.modules.organization.dtos.CreateOrganizationDto;
import com.ht.eventbox.modules.organization.dtos.RemoveMemberDto;
import com.ht.eventbox.modules.organization.dtos.UpdateMemberDto;
import com.ht.eventbox.modules.organization.dtos.UpdateOrganizationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.PublicKey;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "paypal.checkout.webhook.id=checkout-webhook",
        "paypal.payment.webhook.id=payment-webhook"
})
class OrganizationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private com.ht.eventbox.filter.JwtService jwtService;

    @MockBean
    private PublicKey atPublicKey;

    @Test
    void getAll_shouldReturnOrganizations() throws Exception {
        when(organizationService.getAll()).thenReturn(List.of(sampleOrganization(9L)));

        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(9L));
    }

    @Test
    void getMyOwn_shouldReturnOwnedOrganizations() throws Exception {
        when(organizationService.getByUserIdAndOrganizationRole(42L, OrganizationRole.OWNER))
                .thenReturn(List.of(sampleOrganization(9L)));

        mockMvc.perform(get("/api/v1/organizations/me")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(9L));
    }

    @Test
    void getMy_shouldReturnMemberOrganizations() throws Exception {
        when(organizationService.getByUserId(42L)).thenReturn(List.of(sampleOrganization(9L)));

        mockMvc.perform(get("/api/v1/organizations/me/member")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(9L));
    }

    @Test
    void getDetailsById_shouldReturnOrganizationDetails() throws Exception {
        when(organizationService.getDetailsById(9L)).thenReturn(OrganizationService.OrganizationDetails.builder()
                .organization(sampleOrganization(9L))
                .subscribersCount(12L)
                .eventsCount(3L)
                .build());

        mockMvc.perform(get("/api/v1/organizations/9/details")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.subscribers_count").value(12L))
                .andExpect(jsonPath("$.data.events_count").value(3L));
    }

    @Test
    void getById_shouldReturnOrganization() throws Exception {
        when(organizationService.getById(9L)).thenReturn(sampleOrganization(9L));

        mockMvc.perform(get("/api/v1/organizations/9")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value(9L));
    }

    @Test
    void updateById_shouldReturnUpdateResponse() throws Exception {
        when(organizationService.update(eq(42L), eq(9L), any(UpdateOrganizationDto.class))).thenReturn(true);

        mockMvc.perform(put("/api/v1/organizations/9")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateOrganizationDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void deleteById_shouldReturnDeleteResponse() throws Exception {
        when(organizationService.deleteById(42L, 9L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/organizations/9")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.DELETE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void create_shouldReturnCreatedResponse() throws Exception {
        when(organizationService.create(eq(42L), any(CreateOrganizationDto.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/organizations")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateOrganizationDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Created"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void addMember_shouldReturnCreatedResponse() throws Exception {
        when(organizationService.addMember(eq(42L), eq(9L), any(AddMemberDto.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/organizations/9/members")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAddMemberDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Created"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void updateMember_shouldReturnUpdateResponse() throws Exception {
        when(organizationService.updateMember(eq(42L), eq(9L), any(UpdateMemberDto.class))).thenReturn(true);

        mockMvc.perform(put("/api/v1/organizations/9/members")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateMemberDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void removeMember_shouldReturnUpdateResponse() throws Exception {
        when(organizationService.removeMember(eq(42L), eq(9L), any(RemoveMemberDto.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/organizations/9/members/remove")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRemoveMemberDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void subscribe_shouldReturnUpdateResponse() throws Exception {
        when(organizationService.subscribe(42L, 9L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/organizations/9/subscribe")
                        .requestAttr("sub", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(Constant.SuccessCode.UPDATE_SUCCESSFULLY))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void updateById_shouldBindRequestBody() throws Exception {
        when(organizationService.update(eq(42L), eq(9L), any(UpdateOrganizationDto.class))).thenReturn(true);

        mockMvc.perform(put("/api/v1/organizations/9")
                        .requestAttr("sub", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateOrganizationDto())))
                .andExpect(status().isOk());

        verify(organizationService).update(eq(42L), eq(9L), any(UpdateOrganizationDto.class));
    }

    private Organization sampleOrganization(Long id) {
        return Organization.builder()
                .id(id)
                .name("Eventbox")
                .paypalAccount("paypal@example.com")
                .description("Org")
                .userOrganizations(List.of(UserOrganization.builder().role(OrganizationRole.OWNER).build()))
                .build();
    }

    private CreateOrganizationDto sampleCreateOrganizationDto() {
        return CreateOrganizationDto.builder()
                .name("Eventbox")
                .description("Org")
                .paypalAccount("paypal@example.com")
                .logoBase64("logo")
                .phone("123456789")
                .website("https://example.com")
                .email("org@example.com")
                .build();
    }

    private UpdateOrganizationDto sampleUpdateOrganizationDto() {
        return UpdateOrganizationDto.builder()
                .name("Updated")
                .description("Updated org")
                .paypalAccount("paypal@example.com")
                .logoBase64("logo")
                .phone("123456789")
                .website("https://example.com")
                .email("org@example.com")
                .removeLogo(false)
                .build();
    }

    private AddMemberDto sampleAddMemberDto() {
        return AddMemberDto.builder()
                .email("member@example.com")
                .role(com.ht.eventbox.enums.AddableRole.MANAGER)
                .build();
    }

    private UpdateMemberDto sampleUpdateMemberDto() {
        return UpdateMemberDto.builder()
                .email("member@example.com")
                .role(com.ht.eventbox.enums.AddableRole.STAFF)
                .build();
    }

    private RemoveMemberDto sampleRemoveMemberDto() {
        return RemoveMemberDto.builder()
                .email("member@example.com")
                .build();
    }
}
