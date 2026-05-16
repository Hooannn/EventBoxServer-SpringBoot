package com.ht.eventbox.modules.auth;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.FCMToken;
import com.ht.eventbox.entities.Role;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.enums.Platform;
import com.ht.eventbox.filter.JwtService;
import com.ht.eventbox.modules.auth.dtos.AuthenticateDto;
import com.ht.eventbox.modules.auth.dtos.ForgotPasswordDto;
import com.ht.eventbox.modules.auth.dtos.LogoutDto;
import com.ht.eventbox.modules.auth.dtos.RefreshDto;
import com.ht.eventbox.modules.auth.dtos.RegisterDto;
import com.ht.eventbox.modules.auth.dtos.ResendVerifyDto;
import com.ht.eventbox.modules.auth.dtos.ResetPasswordDto;
import com.ht.eventbox.modules.auth.dtos.VerifyDto;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.redis.RedisService;
import com.ht.eventbox.modules.user.FCMTokenRepository;
import com.ht.eventbox.modules.user.RoleRepository;
import com.ht.eventbox.modules.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.timeout;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private MailService mailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RedisService redisService;

    @Mock
    private FCMTokenRepository fcmTokenRepository;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpiration", 7_200_000L);
        ReflectionTestUtils.setField(authService, "passwordExpiration", 3_600_000L);
        ReflectionTestUtils.setField(authService, "refreshSecretKey", "refresh-secret");
    }

    @Test
    void authenticate_shouldReturnCredentialsAndStoreRefreshToken() {
        var user = sampleUser();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        var response = authService.authenticate(AuthenticateDto.builder()
                .username("user@example.com")
                .password("password")
                .build());

        assertThat(response.getUser()).isEqualTo(user);
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(redisService).setValue("refresh_token:user_id:42", "refresh-token", 7200L);
    }

    @Test
    void authenticate_shouldRejectInvalidPassword() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(AuthenticateDto.builder()
                .username("user@example.com")
                .password("wrong-password")
                .build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    HttpException ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.INVALID_CREDENTIALS);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });

        verifyNoInteractions(jwtService, redisService);
    }

    @Test
    void register_shouldPersistRegistrationDataAndSendEmail() throws Exception {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp", "hashed-password");

        var captor = ArgumentCaptor.forClass(AuthService.RegisterData.class);

        boolean result = authService.register(RegisterDto.builder()
                .username("new@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .build());

        assertThat(result).isTrue();
        verify(redisService).setObject(eq("register:username:new@example.com"), captor.capture(), eq(3600L));
        assertThat(captor.getValue().getFirstName()).isEqualTo("Test");
        assertThat(captor.getValue().getLastName()).isEqualTo("User");
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getOtp()).isEqualTo("hashed-otp");
        verify(mailService, timeout(1000)).sendRegistrationEmail(eq("new@example.com"), eq("Test User"), anyString());
    }

    @Test
    void register_shouldRejectExistingUser() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(RegisterDto.builder()
                .username("new@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    HttpException ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.USER_ALREADY_EXISTS);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void verify_shouldCreateUserAndIssueCredentials() throws Exception {
        var registerData = AuthService.RegisterData.builder()
                .firstName("Test")
                .lastName("User")
                .password("hashed-password")
                .otp("hashed-otp")
                .build();
        var role = Role.builder().id(7L).name(Constant.DefaultRole.USER).build();

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(redisService.getObject("register:username:user@example.com", AuthService.RegisterData.class))
                .thenReturn(registerData);
        when(passwordEncoder.matches("123456", "hashed-otp")).thenReturn(true);
        when(roleRepository.findByName(Constant.DefaultRole.USER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(42L);
            return savedUser;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        var response = authService.verify(VerifyDto.builder()
                .username("user@example.com")
                .otp("123456")
                .build());

        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        assertThat(response.getUser().getRoles()).contains(role);
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(redisService).deleteValue("register:username:user@example.com");
        verify(redisService).setValue("refresh_token:user_id:42", "refresh-token", 7200L);
    }

    @Test
    void verify_shouldRejectWrongOtp() throws Exception {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(redisService.getObject("register:username:user@example.com", AuthService.RegisterData.class))
                .thenReturn(AuthService.RegisterData.builder()
                        .firstName("Test")
                        .lastName("User")
                        .password("hashed-password")
                        .otp("hashed-otp")
                        .build());
        when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);

        assertThatThrownBy(() -> authService.verify(VerifyDto.builder()
                .username("user@example.com")
                .otp("000000")
                .build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    HttpException ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.INVALID_CREDENTIALS);
                    assertThat(ex.getStatus().value()).isEqualTo(400);
                });

        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    void resendVerify_shouldUpdateOtpAndSendEmail() throws Exception {
        var registerData = AuthService.RegisterData.builder()
                .firstName("Test")
                .lastName("User")
                .password("hashed-password")
                .otp("old-hashed-otp")
                .build();

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(redisService.getObject("register:username:user@example.com", AuthService.RegisterData.class))
                .thenReturn(registerData);
        when(passwordEncoder.encode(anyString())).thenReturn("new-hashed-otp");

        boolean result = authService.resendVerify(ResendVerifyDto.builder()
                .username("user@example.com")
                .build());

        assertThat(result).isTrue();
        verify(redisService).setObject(eq("register:username:user@example.com"), any(AuthService.RegisterData.class), eq(3600L));
        verify(mailService, timeout(1000)).sendRegistrationEmail(eq("user@example.com"), eq("Test User"), anyString());
    }

    @Test
    void forgotPassword_shouldStoreOtpAndSendEmail() throws Exception {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        boolean result = authService.forgotPassword(ForgotPasswordDto.builder()
                .username("user@example.com")
                .build());

        assertThat(result).isTrue();
        verify(redisService).setValue("reset_password_otp:username:user@example.com", "hashed-otp", 3600L);
        verify(mailService, timeout(1000)).sendForgotPasswordEmail(eq("user@example.com"), anyString());
    }

    @Test
    void resetPassword_shouldUpdateStoredPasswordAndClearOtp() {
        var user = sampleUser();
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);
        when(redisService.getValue("reset_password_otp:username:user@example.com")).thenReturn("stored-otp");
        when(passwordEncoder.matches("123456", "stored-otp")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("hashed-new-password");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        boolean result = authService.resetPassword(ResetPasswordDto.builder()
                .username("user@example.com")
                .otp("123456")
                .password("new-password")
                .build());

        assertThat(result).isTrue();
        assertThat(user.getPassword()).isEqualTo("hashed-new-password");
        verify(redisService).deleteValue("reset_password_otp:username:user@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void refresh_shouldReturnNewCredentialsWhenTokenIsValidAndMatchesStoredToken() {
        var user = sampleUser();

        when(jwtService.isTokenValid("refresh-token", "refresh-secret")).thenReturn(true);
        when(jwtService.extractSub("refresh-token", "refresh-secret")).thenReturn("42");
        when(redisService.getValue("refresh_token:user_id:42")).thenReturn("refresh-token");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");

        var credentials = authService.refresh(RefreshDto.builder()
                .token("refresh-token")
                .build());

        assertThat(credentials.getAccessToken()).isEqualTo("new-access-token");
        assertThat(credentials.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(redisService).setValue("refresh_token:user_id:42", "new-refresh-token", 7200L);
    }

    @Test
    void refresh_shouldRejectInvalidToken() {
        when(jwtService.isTokenValid("refresh-token", "refresh-secret")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(RefreshDto.builder()
                .token("refresh-token")
                .build()))
                .isInstanceOf(HttpException.class)
                .satisfies(throwable -> {
                    HttpException ex = (HttpException) throwable;
                    assertThat(ex.getMessage()).isEqualTo(Constant.ErrorCode.INVALID_CREDENTIALS);
                    assertThat(ex.getStatus().value()).isEqualTo(403);
                });

        verifyNoInteractions(redisService, userRepository);
    }

    @Test
    void logout_shouldClearRefreshTokenAndFcmTokenForAndroid() {
        var token = FCMToken.builder()
                .id(5L)
                .android("android-token")
                .ios("ios-token")
                .webPush("web-token")
                .build();
        when(fcmTokenRepository.findByUserId(42L)).thenReturn(Optional.of(token));

        boolean result = authService.logout(42L, LogoutDto.builder()
                .platform(Platform.ANDROID)
                .build());

        assertThat(result).isTrue();
        assertThat(token.getAndroid()).isNull();
        assertThat(token.getIos()).isEqualTo("ios-token");
        assertThat(token.getWebPush()).isEqualTo("web-token");
        verify(redisService).deleteValue("refresh_token:user_id:42");
        verify(fcmTokenRepository).save(token);
    }

    private User sampleUser() {
        return User.builder()
                .id(42L)
                .email("user@example.com")
                .firstName("Test")
                .lastName("User")
                .password("hashed-password")
                .build();
    }
}
