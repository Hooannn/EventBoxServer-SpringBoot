package com.ht.elearning.user;

import com.ht.elearning.auth.AuthProcessor;
import com.ht.elearning.config.HttpException;
import com.ht.elearning.user.dtos.CreateUserDto;
import com.ht.elearning.user.dtos.UpdateUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthProcessor authProcessor;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository repository;
    public User findUserById(String userId) {
        return repository.findById(userId).orElseThrow(() -> new HttpException("User not found", HttpStatus.BAD_REQUEST));
    }

    public boolean deleteUserById(String userId) {
        try {
            var exists = repository.existsById(userId);
            if (!exists) throw new HttpException("User not found", HttpStatus.BAD_REQUEST);
            repository.deleteById(userId);
            return true;
        } catch (HttpException e) {
            throw new HttpException(e.getMessage(), e.getStatus());
        } catch (Exception e) {
            throw new HttpException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public User createUser(CreateUserDto createUserDto) {
        try {
            var exists = repository.existsByEmail(createUserDto.getEmail());
            if (exists) throw new HttpException("Email is already registered", HttpStatus.BAD_REQUEST);
            var user = User.builder()
                    .lastName(createUserDto.getLastName())
                    .firstName(createUserDto.getFirstName())
                    .avatarUrl(createUserDto.getAvatarUrl())
                    .email(createUserDto.getEmail())
                    .password(passwordEncoder.encode(createUserDto.getPassword()))
                    .role(createUserDto.getRole())
                    .verified(createUserDto.isVerified())
                    .build();

            var savedUser = repository.save(user);

            if (savedUser.isVerified()) {
                authProcessor.processWelcomeUser(savedUser);
            } else {
                authProcessor.processAccountVerification(savedUser);
            }

            return savedUser;
        } catch (HttpException e) {
            throw new HttpException(e.getMessage(), e.getStatus());
        } catch (Exception e) {
            throw new HttpException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    public User updateUser(String userId, UpdateUserDto updateUserDto) {
        try {
            User user = repository.findById(userId)
                    .orElseThrow(() -> new HttpException("User not found", HttpStatus.BAD_REQUEST));

            Optional.ofNullable(updateUserDto.getAvatarUrl()).ifPresent(user::setAvatarUrl);
            Optional.ofNullable(updateUserDto.getPassword()).ifPresent(password -> user.setPassword(passwordEncoder.encode(password)));
            Optional.ofNullable(updateUserDto.getRole()).ifPresent(user::setRole);
            Optional.ofNullable(updateUserDto.getFirstName()).ifPresent(user::setFirstName);
            Optional.ofNullable(updateUserDto.getLastName()).ifPresent(user::setLastName);

            repository.save(user);

            return user;
        } catch (HttpException e) {
            throw new HttpException(e.getMessage(), e.getStatus());
        } catch (Exception e) {
            throw new HttpException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
