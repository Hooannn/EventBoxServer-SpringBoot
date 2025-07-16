package com.ht.eventbox;

import com.corundumstudio.socketio.SocketIOServer;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Permission;
import com.ht.eventbox.entities.Role;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.modules.user.PermissionRepository;
import com.ht.eventbox.modules.user.RoleRepository;
import com.ht.eventbox.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServerCommandLineRunner implements CommandLineRunner {
    private final SocketIOServer server;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${root.user.email}")
    private String rootUserEmail;

    @Value("${root.user.password}")
    private String rootUserPassword;

    @Value("${root.role.name}")
    private String rootRoleName;

    @Value("${root.role.description}")
    private String rootRoleDescription;

    @Value("${root.role.permissions}")
    private String rootRolePermissions;

    @Override
    public void run(String... args) throws Exception {
        server.start();

        List<Permission> appPermissions = List.of(
                Permission.builder().name("create:orders").build(),
                Permission.builder().name("read:orders").build(),
                Permission.builder().name("update:orders").build(),
                Permission.builder().name("read:events").build(),
                Permission.builder().name("read:categories").build()
        );

        boolean shouldBootstrap = userRepository.count() == 0;

        if (!shouldBootstrap) return;

        permissionRepository.saveAll(appPermissions);


        var permissions = permissionRepository.saveAll(
                Arrays.stream(rootRolePermissions.split(",")).map(permissionName ->
                        Permission.builder()
                                .name(permissionName.trim())
                                .build()
                ).collect(Collectors.toList())
        );

        var role = roleRepository.save(
                Role.builder()
                        .name(rootRoleName)
                        .description(rootRoleDescription)
                        .permissions(new HashSet<>(permissions))
                        .build()
        );

        userRepository.save(
                User.builder()
                        .firstName("Root")
                        .lastName("User")
                        .email(rootUserEmail)
                        .password(passwordEncoder.encode(rootUserPassword))
                        .roles(new HashSet<>(List.of(role)))
                        .build()
        );

        roleRepository.save(
                Role.builder()
                        .name(Constant.DefaultRole.USER)
                        .description(Constant.DefaultRole.USER_DESCRIPTION)
                        .permissions(new HashSet<>(
                                appPermissions
                        ))
                        .build()
        );
    }
}