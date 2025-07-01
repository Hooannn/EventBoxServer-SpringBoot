package com.ht.eventbox.modules.user;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Category;
import com.ht.eventbox.entities.FCMToken;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.modules.category.CategoryRepository;
import com.ht.eventbox.modules.category.dtos.CreateBulkCategoriesDto;
import com.ht.eventbox.modules.category.dtos.UpdateFCMTokensDto;
import com.ht.eventbox.modules.user.dtos.CreateBulkUsersDto;
import com.ht.eventbox.modules.user.dtos.CreateUserDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final FCMTokenRepository fcmTokenRepository;

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
        return userRepository.findAll();
    }
}
