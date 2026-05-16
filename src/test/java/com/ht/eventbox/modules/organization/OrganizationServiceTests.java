package com.ht.eventbox.modules.organization;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Asset;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.entities.UserOrganization;
import com.ht.eventbox.entities.UserOrganizationId;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.asset.AssetRepository;
import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.organization.dtos.AddMemberDto;
import com.ht.eventbox.modules.organization.dtos.CreateOrganizationDto;
import com.ht.eventbox.modules.organization.dtos.RemoveMemberDto;
import com.ht.eventbox.modules.organization.dtos.UpdateMemberDto;
import com.ht.eventbox.modules.organization.dtos.UpdateOrganizationDto;
import com.ht.eventbox.modules.storage.CloudinaryService;
import com.ht.eventbox.modules.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTests {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private OrganizationService organizationService;

    @Test
    void getById_shouldThrowWhenMissing() {
        when(organizationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getById(9L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.ORGANIZATION_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    void getById_shouldReturnOrganizationWhenFound() {
        var org = sampleOrganization(9L, 42L);
        when(organizationRepository.findById(9L)).thenReturn(Optional.of(org));

        var result = organizationService.getById(9L);

        assertThat(result).isSameAs(org);
    }

    @Test
    void getDetailsById_shouldReturnCounts() {
        var org = sampleOrganization(9L, 42L);
        when(organizationRepository.findById(9L)).thenReturn(Optional.of(org));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(9L))).thenReturn(12L);
        when(eventRepository.countAllByOrganizationIdAndStatusIs(9L, EventStatus.PUBLISHED)).thenReturn(3L);

        var result = organizationService.getDetailsById(9L);

        assertThat(result.organization).isSameAs(org);
        assertThat(result.subscribersCount).isEqualTo(12L);
        assertThat(result.eventsCount).isEqualTo(3L);
    }

    @Test
    void create_shouldPersistOrganizationWithoutLogo() throws Exception {
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        var result = organizationService.create(42L, sampleCreateOrganizationDto(null));

        assertThat(result).isTrue();
        verify(cloudinaryService, never()).uploadByBase64(anyString(), anyString());
        verify(organizationRepository, org.mockito.Mockito.times(2)).save(any(Organization.class));
    }

    @Test
    void create_shouldUploadLogoAndPersistAsset() throws Exception {
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(cloudinaryService.uploadByBase64(eq("logo-base64"), anyString())).thenReturn(sampleUploadResult("logo"));

        var captor = ArgumentCaptor.forClass(Organization.class);

        var result = organizationService.create(42L, sampleCreateOrganizationDto("logo-base64"));

        assertThat(result).isTrue();
        verify(organizationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getValue().getAssets()).extracting(Asset::getUsage).containsExactly(AssetUsage.AVATAR);
    }

    @Test
    void update_shouldRejectNonOwner() {
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.update(42L, 9L, sampleUpdateOrganizationDto(false, null)))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.ORGANIZATION_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    void update_shouldRemoveExistingAssetsWhenRemoveLogoIsTrue() {
        var org = sampleOrganizationWithAsset(9L, 42L);
        var assetsBeforeUpdate = new HashSet<>(org.getAssets());
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));

        var result = organizationService.update(42L, 9L, sampleUpdateOrganizationDto(true, null));

        assertThat(result).isTrue();
        assertThat(org.getAssets()).isEmpty();
        verify(assetRepository).deleteAll(assetsBeforeUpdate);
    }

    @Test
    void update_shouldReplaceLogoWhenNewLogoIsProvided() throws Exception {
        var org = sampleOrganizationWithAsset(9L, 42L);
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));
        when(cloudinaryService.uploadByBase64(eq("updated-logo"), anyString()))
                .thenReturn(sampleUploadResult("updated"));

        var result = organizationService.update(42L, 9L, sampleUpdateOrganizationDto(false, "updated-logo"));

        assertThat(result).isTrue();
        assertThat(org.getAssets()).extracting(Asset::getUsage).containsExactly(AssetUsage.AVATAR);
    }

    @Test
    void deleteById_shouldRejectWhenOrganizationHasEvents() {
        var org = sampleOrganization(9L, 42L);
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));
        when(eventRepository.existsByOrganizationId(9L)).thenReturn(true);

        assertThatThrownBy(() -> organizationService.deleteById(42L, 9L))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.ORGANIZATION_HAS_EVENTS);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void deleteById_shouldDeleteOrganizationAndAssets() throws Exception {
        var org = sampleOrganizationWithAsset(9L, 42L);
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));
        when(eventRepository.existsByOrganizationId(9L)).thenReturn(false);

        var result = organizationService.deleteById(42L, 9L);

        assertThat(result).isTrue();
        verify(organizationRepository).delete(org);
        verify(assetRepository).deleteAll(org.getAssets());
    }

    @Test
    void addMember_shouldRejectDuplicateMember() {
        var org = sampleOrganization(9L, 42L);
        org.getUserOrganizations().add(sampleUserOrganization(99L, "member@example.com", OrganizationRole.STAFF));
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));

        assertThatThrownBy(() -> organizationService.addMember(42L, 9L, sampleAddMemberDto("member@example.com")))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.USER_ALREADY_IN_ORGANIZATION);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void addMember_shouldRejectMissingUser() {
        var org = sampleOrganization(9L, 42L);
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.addMember(42L, 9L, sampleAddMemberDto("new@example.com")))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.USER_NOT_FOUND);
                    assertThat(ex.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    void addMember_shouldPersistNewMember() throws Exception {
        var org = sampleOrganization(9L, 42L);
        var user = User.builder().id(77L).email("new@example.com").firstName("New").lastName("User").build();
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));

        var result = organizationService.addMember(42L, 9L, sampleAddMemberDto("new@example.com"));

        assertThat(result).isTrue();
        verify(organizationRepository).save(org);

        verify(mailService, timeout(500)).sendMemberAddedEmail("new@example.com", "New User", "Eventbox");
    }

    @Test
    void updateMember_shouldRejectMissingTarget() {
        var org = sampleOrganization(9L, 42L);
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));

        assertThatThrownBy(
                () -> organizationService.updateMember(42L, 9L, sampleUpdateMemberDto("missing@example.com")))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.USER_NOT_IN_ORGANIZATION);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void updateMember_shouldPersistRoleChange() {
        var org = sampleOrganization(9L, 42L);
        var member = sampleUserOrganization(77L, "member@example.com", OrganizationRole.STAFF);
        org.getUserOrganizations().add(member);
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));

        var result = organizationService.updateMember(42L, 9L, sampleUpdateMemberDto("member@example.com"));

        assertThat(result).isTrue();
        assertThat(member.getRole()).isEqualTo(OrganizationRole.MANAGER);
    }

    @Test
    void removeMember_shouldRejectMissingTarget() {
        var org = sampleOrganization(9L, 42L);
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));

        assertThatThrownBy(
                () -> organizationService.removeMember(42L, 9L, sampleRemoveMemberDto("missing@example.com")))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    var ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.USER_NOT_IN_ORGANIZATION);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void removeMember_shouldRemoveMemberAndPersistOrganization() throws Exception {
        var org = sampleOrganization(9L, 42L);
        var member = sampleUserOrganization(77L, "member@example.com", OrganizationRole.STAFF);
        org.getUserOrganizations().add(member);
        when(organizationRepository.findByIdAndUserOrganizationsUserIdAndUserOrganizationsRoleIs(9L, 42L,
                OrganizationRole.OWNER))
                .thenReturn(Optional.of(org));

        var result = organizationService.removeMember(42L, 9L, sampleRemoveMemberDto("member@example.com"));

        assertThat(result).isTrue();
        verify(organizationRepository).save(org);
        verify(mailService, timeout(500)).sendMemberRemovedEmail("member@example.com", "Member User", "Eventbox");
    }

    @Test
    void subscribe_shouldAddSubscriptionWhenMissing() {
        var user = User.builder().id(42L).subscriptions(new HashSet<>()).build();
        var org = sampleOrganization(9L, 99L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(9L)).thenReturn(Optional.of(org));

        var result = organizationService.subscribe(42L, 9L);

        assertThat(result).isTrue();
        assertThat(user.getSubscriptions()).contains(org);
        verify(userRepository).save(user);
    }

    @Test
    void subscribe_shouldRemoveSubscriptionWhenPresent() {
        var org = sampleOrganization(9L, 99L);
        var user = User.builder().id(42L).subscriptions(new HashSet<>(Set.of(org))).build();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(9L)).thenReturn(Optional.of(org));

        var result = organizationService.subscribe(42L, 9L);

        assertThat(result).isTrue();
        assertThat(user.getSubscriptions()).doesNotContain(org);
        verify(userRepository).save(user);
    }

    private Organization sampleOrganization(Long orgId, Long ownerUserId) {
        var org = Organization.builder()
                .id(orgId)
                .name("Eventbox")
                .paypalAccount("paypal@example.com")
                .description("Org")
                .assets(new HashSet<>())
                .userOrganizations(new java.util.ArrayList<>())
                .build();
        org.getUserOrganizations()
                .add(sampleUserOrganization(ownerUserId, "owner@example.com", OrganizationRole.OWNER));
        return org;
    }

    private Organization sampleOrganizationWithAsset(Long orgId, Long ownerUserId) {
        var org = sampleOrganization(orgId, ownerUserId);
        org.getAssets().add(sampleAsset("logo"));
        return org;
    }

    private Asset sampleAsset(String suffix) {
        return Asset.builder()
                .id(suffix + "-asset")
                .signature(suffix + "-signature")
                .publicId(suffix + "-public")
                .originalUrl("https://example.com/" + suffix)
                .secureUrl("https://example.com/" + suffix + "?secure=1")
                .usage(AssetUsage.AVATAR)
                .format("png")
                .resourceType("image")
                .folder("organization-assets")
                .eTag(suffix + "-etag")
                .width(100)
                .height(100)
                .bytes(1234)
                .build();
    }

    private UserOrganization sampleUserOrganization(Long userId, String email, OrganizationRole role) {
        return UserOrganization.builder()
                .id(UserOrganizationId.builder().userId(userId).organizationId(9L).build())
                .user(User.builder().id(userId).email(email).firstName("Member").lastName("User").build())
                .organization(Organization.builder().id(9L).build())
                .role(role)
                .build();
    }

    private CreateOrganizationDto sampleCreateOrganizationDto(String logoBase64) {
        return CreateOrganizationDto.builder()
                .name("Eventbox")
                .description("Org")
                .paypalAccount("paypal@example.com")
                .logoBase64(logoBase64)
                .phone("123456789")
                .website("https://example.com")
                .email("org@example.com")
                .build();
    }

    private UpdateOrganizationDto sampleUpdateOrganizationDto(boolean removeLogo, String logoBase64) {
        return UpdateOrganizationDto.builder()
                .name("Updated")
                .description("Updated org")
                .paypalAccount("paypal@example.com")
                .logoBase64(logoBase64)
                .phone("123456789")
                .website("https://example.com")
                .email("org@example.com")
                .removeLogo(removeLogo)
                .build();
    }

    private AddMemberDto sampleAddMemberDto(String email) {
        return AddMemberDto.builder()
                .email(email)
                .role(com.ht.eventbox.enums.AddableRole.MANAGER)
                .build();
    }

    private UpdateMemberDto sampleUpdateMemberDto(String email) {
        return UpdateMemberDto.builder()
                .email(email)
                .role(com.ht.eventbox.enums.AddableRole.MANAGER)
                .build();
    }

    private RemoveMemberDto sampleRemoveMemberDto(String email) {
        return RemoveMemberDto.builder()
                .email(email)
                .build();
    }

    private Map<String, Object> sampleUploadResult(String suffix) {
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("resource_type", "image");
        uploadResult.put("public_id", suffix + "-public");
        uploadResult.put("signature", suffix + "-signature");
        uploadResult.put("asset_id", suffix + "-asset");
        uploadResult.put("url", "https://example.com/" + suffix);
        uploadResult.put("secure_url", "https://example.com/" + suffix + "?secure=1");
        uploadResult.put("folder", "organization-assets");
        uploadResult.put("format", "png");
        uploadResult.put("width", 100);
        uploadResult.put("height", 100);
        uploadResult.put("bytes", 1234);
        uploadResult.put("etag", suffix + "-etag");
        return uploadResult;
    }
}
