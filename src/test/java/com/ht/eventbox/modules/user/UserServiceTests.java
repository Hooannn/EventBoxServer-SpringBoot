package com.ht.eventbox.modules.user;

import com.ht.eventbox.entities.User;
import com.ht.eventbox.entities.Permission;
import com.ht.eventbox.entities.Role;
import com.ht.eventbox.modules.asset.AssetRepository;
import com.ht.eventbox.modules.storage.CloudinaryService;
import com.ht.eventbox.modules.user.dtos.UpdateUserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private FCMTokenRepository fcmTokenRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(User.builder().id(9L).build()), PageRequest.of(1, 10), 21);
        when(userRepository.findAllByOrderByIdAsc(any())).thenReturn(page);

        var result = userService.getAll(PageRequest.of(1, 10));

        assertThat(result).isSameAs(page);
        verify(userRepository).findAllByOrderByIdAsc(PageRequest.of(1, 10));
    }

    @Test
    void getAllSearchPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(User.builder().id(9L).build()), PageRequest.of(1, 10), 21);
        when(userRepository.searchAllByOrderByIdAsc(eq("alice"), any())).thenReturn(page);

        var result = userService.getAll("alice", PageRequest.of(1, 10));

        assertThat(result).isSameAs(page);
        verify(userRepository).searchAllByOrderByIdAsc("alice", PageRequest.of(1, 10));
    }

    @Test
    void getAllRolesPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Role.builder().id(3L).build()), PageRequest.of(0, 15), 31);
        when(roleRepository.findAllByOrderByIdAsc(any())).thenReturn(page);

        var result = userService.getAllRoles(PageRequest.of(0, 15));

        assertThat(result).isSameAs(page);
        verify(roleRepository).findAllByOrderByIdAsc(PageRequest.of(0, 15));
    }

    @Test
    void getAllRolesSearchPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Role.builder().id(3L).build()), PageRequest.of(0, 15), 31);
        when(roleRepository.searchAllByOrderByIdAsc(eq("admin"), any())).thenReturn(page);

        var result = userService.getAllRoles("admin", PageRequest.of(0, 15));

        assertThat(result).isSameAs(page);
        verify(roleRepository).searchAllByOrderByIdAsc("admin", PageRequest.of(0, 15));
    }

    @Test
    void getAllPermissionsPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Permission.builder().id(5L).build()), PageRequest.of(2, 7), 21);
        when(permissionRepository.findAllByOrderByIdAsc(any())).thenReturn(page);

        var result = userService.getAllPermissions(PageRequest.of(2, 7));

        assertThat(result).isSameAs(page);
        verify(permissionRepository).findAllByOrderByIdAsc(PageRequest.of(2, 7));
    }

    @Test
    void getAllPermissionsSearchPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Permission.builder().id(5L).build()), PageRequest.of(2, 7), 21);
        when(permissionRepository.searchAllByOrderByIdAsc(eq("manage"), any())).thenReturn(page);

        var result = userService.getAllPermissions("manage", PageRequest.of(2, 7));

        assertThat(result).isSameAs(page);
        verify(permissionRepository).searchAllByOrderByIdAsc("manage", PageRequest.of(2, 7));
    }

    @Test
    void updateById_shouldPatchOnlyProvidedFields() {
        var user = User.builder()
                .id(1L)
                .firstName("Old")
                .lastName("Name")
                .email("old@example.com")
                .password("encoded-old")
                .phone("111")
                .birthday(LocalDateTime.of(2000, 1, 1, 0, 0))
                .build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");

        var result = userService.updateById(1L, UpdateUserDto.builder()
                .firstName("New")
                .email("new@example.com")
                .password("new-password")
                .build());

        assertThat(result).isTrue();
        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("Name");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPassword()).isEqualTo("encoded-new");
        assertThat(user.getPhone()).isEqualTo("111");
        assertThat(user.getBirthday()).isEqualTo(LocalDateTime.of(2000, 1, 1, 0, 0));
        verify(userRepository).save(user);
    }
}
