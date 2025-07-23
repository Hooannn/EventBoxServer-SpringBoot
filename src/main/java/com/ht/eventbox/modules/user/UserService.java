package com.ht.eventbox.modules.user;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.*;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.modules.asset.AssetRepository;
import com.ht.eventbox.modules.storage.CloudinaryService;
import com.ht.eventbox.modules.user.dtos.*;
import com.ht.eventbox.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final FCMTokenRepository fcmTokenRepository;
    private final AssetRepository assetRepository;
    private final CloudinaryService cloudinaryService;

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    public boolean create(CreateUserDto createUserDto) {
        if (userRepository.existsByEmail(createUserDto.getEmail())) {
            throw new HttpException(Constant.ErrorCode.USER_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        var defaultRole = roleRepository.findByName(Constant.DefaultRole.USER)
                .orElseThrow(() -> new HttpException("Default user role not found in database", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = User.builder()
                .email(createUserDto.getEmail())
                .firstName(createUserDto.getFirstName())
                .lastName(createUserDto.getLastName())
                .password(passwordEncoder.encode(createUserDto.getPassword()))
                .roles(
                        new HashSet<>(
                                List.of(defaultRole)
                        )
                )
                .build();

        userRepository.save(user);
        return true;
    }

    public boolean createBulk(CreateBulkUsersDto createBulkUsersDto) {
        var defaultRole = roleRepository.findByName(Constant.DefaultRole.USER)
                .orElseThrow(() -> new HttpException("Default user role not found in database", HttpStatus.INTERNAL_SERVER_ERROR));

        List<User> users = createBulkUsersDto.getUsers().stream()
                .map(createUserDto -> User.builder()
                        .email(createUserDto.getEmail())
                        .firstName(createUserDto.getFirstName())
                        .lastName(createUserDto.getLastName())
                        .password(passwordEncoder.encode(createUserDto.getPassword()))
                        .roles(
                                new HashSet<>(
                                        List.of(defaultRole)
                                )
                        )
                        .build())
                .toList();

        userRepository.saveAll(users);
        return true;
    }

    public boolean updateFCMTokens(Long userId, UpdateFCMTokensDto updateFCMTokensDto) {
        FCMToken fcmToken = fcmTokenRepository.findByUserId(userId).orElse(null);
        if (fcmToken == null) {
            fcmToken = FCMToken.builder()
                    .user(User.builder().id(userId).build())
                    .build();
        }
        switch (updateFCMTokensDto.getPlatform()) {
            case ANDROID -> fcmToken.setAndroid(updateFCMTokensDto.getToken());
            case IOS -> fcmToken.setIos(updateFCMTokensDto.getToken());
            case WEB -> fcmToken.setWebPush(updateFCMTokensDto.getToken());
        }
        fcmTokenRepository.save(fcmToken);
        return true;
    }

    public List<User> getAll() {
        return userRepository.findAllByOrderByIdAsc();
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAllByOrderByIdAsc();
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAllByOrderByIdAsc();
    }

    public boolean updateUserRole(Long userId, UpdateUserRoleDto updateUserRoleDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        user.setRoles(new HashSet<>(
                roleRepository.findAllById(updateUserRoleDto.getRoleIds())
        ));

        userRepository.save(user);
        return true;
    }

    public boolean updateRole(Long roleId, UpdateRoleDto updateRoleDto) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.ROLE_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (roleRepository.existsByNameAndIdIsNot(updateRoleDto.getName(), roleId)) {
            throw new HttpException(Constant.ErrorCode.ROLE_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        role.setName(updateRoleDto.getName());
        role.setDescription(updateRoleDto.getDescription());
        role.setPermissions(new HashSet<>(
                permissionRepository.findAllById(updateRoleDto.getPermissionIds())
        ));

        roleRepository.save(role);
        return true;
    }

    public boolean deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.ROLE_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (userRepository.existsByRolesId(roleId)) {
            throw new HttpException(Constant.ErrorCode.ROLE_IN_USE, HttpStatus.BAD_REQUEST);
        }

        role.getPermissions().clear();

        roleRepository.delete(role);
        return true;
    }

    public boolean createRole(CreateRoleDto createRoleDto) {
        if (roleRepository.existsByName(createRoleDto.getName())) {
            throw new HttpException(Constant.ErrorCode.ROLE_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        Role role = Role.builder()
                .name(createRoleDto.getName())
                .description(createRoleDto.getDescription())
                .permissions(new HashSet<>(
                        permissionRepository.findAllById(createRoleDto.getPermissionIds())
                ))
                .build();

        roleRepository.save(role);
        return true;
    }

    public boolean createPermission(CreatePermissionDto createPermissionDto) {
        if (permissionRepository.existsByName(createPermissionDto.getName())) {
            throw new HttpException(Constant.ErrorCode.PERMISSION_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        Permission permission = Permission.builder()
                .name(createPermissionDto.getName())
                .description(createPermissionDto.getDescription())
                .build();

        permissionRepository.save(permission);
        return true;
    }

    public boolean updatePermission(Long permissionId, UpdatePermissionDto updatePermissionDto) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.PERMISSION_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (permissionRepository.existsByNameAndIdIsNot(updatePermissionDto.getName(), permissionId)) {
            throw new HttpException(Constant.ErrorCode.PERMISSION_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        permission.setName(updatePermissionDto.getName());
        permission.setDescription(updatePermissionDto.getDescription());

        permissionRepository.save(permission);
        return true;
    }

    public boolean deletePermission(Long permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.PERMISSION_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (roleRepository.existsByPermissionsId(permissionId)) {
            throw new HttpException(Constant.ErrorCode.PERMISSION_IN_USE, HttpStatus.BAD_REQUEST);
        }

        permissionRepository.delete(permission);
        return true;
    }

    public boolean changePassword(Long userId, ChangePasswordDto changePasswordDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
            throw new HttpException(Constant.ErrorCode.INVALID_CREDENTIALS, HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        userRepository.save(user);
        return true;
    }

    @Transactional
    public boolean updateInformation(Long userId, UpdateInformationDto updateInformationDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpException(Constant.ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        user.setFirstName(updateInformationDto.getFirstName());
        user.setLastName(updateInformationDto.getLastName());

        Set<Asset> assetsToRemove = null;
        if (updateInformationDto.isRemoveAvatar()) {
            user.getAssets().forEach(asset -> {
                try {
                    cloudinaryService.destroyByPublicId(asset.getPublicId(), asset.getResourceType());
                    logger.info("Deleted image: {}", asset.getPublicId());
                } catch (IOException e) {
                    throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            });
            assetsToRemove = new HashSet<>(user.getAssets());
            user.getAssets().clear();
        } else if (updateInformationDto.getAvatarBase64() != null && !updateInformationDto.getAvatarBase64().isEmpty()) {
            user.getAssets().forEach(asset -> {
                try {
                    cloudinaryService.destroyByPublicId(asset.getPublicId(), asset.getResourceType());
                    logger.info("Deleted image: {}", asset.getPublicId());
                } catch (IOException e) {
                    throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            });
            assetsToRemove = new HashSet<>(user.getAssets());
            user.getAssets().clear();
            Map uploadResult;
            try {
                uploadResult = cloudinaryService.uploadByBase64(
                        updateInformationDto.getAvatarBase64(),
                        Constant.StorageFolder.USER_ASSETS
                );
                logger.info("Uploaded image: {}", uploadResult);
            } catch (IOException e) {
                throw new HttpException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (uploadResult == null)
                throw new HttpException(Constant.ErrorCode.CLOUDINARY_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);

            var asset = Helper.getAssetFromUploadResult(uploadResult, AssetUsage.AVATAR);

            user.getAssets().add(asset);
        }

        userRepository.save(user);
        if (assetsToRemove != null) {
            assetRepository.deleteAll(assetsToRemove);
        }
        return true;
    }
}
