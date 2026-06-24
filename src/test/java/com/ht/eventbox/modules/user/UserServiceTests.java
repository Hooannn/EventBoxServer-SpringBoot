package com.ht.eventbox.modules.user;

import com.ht.eventbox.entities.User;
import com.ht.eventbox.modules.asset.AssetRepository;
import com.ht.eventbox.modules.storage.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void getAllRolesPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Role.builder().id(3L).build()), PageRequest.of(0, 15), 31);
        when(roleRepository.findAllByOrderByIdAsc(any())).thenReturn(page);

        var result = userService.getAllRoles(PageRequest.of(0, 15));

        assertThat(result).isSameAs(page);
        verify(roleRepository).findAllByOrderByIdAsc(PageRequest.of(0, 15));
    }

    @Test
    void getAllPermissionsPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Permission.builder().id(5L).build()), PageRequest.of(2, 7), 21);
        when(permissionRepository.findAllByOrderByIdAsc(any())).thenReturn(page);

        var result = userService.getAllPermissions(PageRequest.of(2, 7));

        assertThat(result).isSameAs(page);
        verify(permissionRepository).findAllByOrderByIdAsc(PageRequest.of(2, 7));
    }
}
