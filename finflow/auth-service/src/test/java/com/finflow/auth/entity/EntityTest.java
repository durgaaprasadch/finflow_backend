package com.finflow.auth.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    void testUserEntity() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setFullName("Full Name");
        user.setEmail("test@example.com");
        user.setPassword("pass");
        user.setRole("ADMIN");
        user.setStatus("ACTIVE");
        user.setPhone("123456");
        user.setApprovedBy("admin");
        user.setApprovedAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        assertEquals(id, user.getId());
        assertEquals("Full Name", user.getFullName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("pass", user.getPassword());
        assertEquals("ADMIN", user.getRole());
        assertEquals("ACTIVE", user.getStatus());
        assertEquals("123456", user.getPhone());
        assertEquals("admin", user.getApprovedBy());
        assertNotNull(user.getApprovedAt());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());

        User builderUser = User.builder()
                .id(id)
                .fullName("Full Name")
                .email("TEST@EXAMPLE.COM")
                .password("pass")
                .role("ADMIN")
                .status("ACTIVE")
                .phone("123456")
                .build();
        assertEquals(id, builderUser.getId());
        assertEquals("Full Name", builderUser.getFullName());
        assertEquals("TEST@EXAMPLE.COM", builderUser.getEmail());
        assertEquals("pass", builderUser.getPassword());
        assertEquals("ADMIN", builderUser.getRole());
        assertEquals("ACTIVE", builderUser.getStatus());
        assertEquals("123456", builderUser.getPhone());
    }

    @Test
    void testUserLifecycleHooks() {
        User user = new User();
        user.setEmail("TEST@EXAMPLE.COM");
        user.onCreate();
        
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals("test@example.com", user.getEmail());
        
        user.onUpdate();
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void testUserOtpEntity() {
        UserOtp otp = new UserOtp();
        otp.setId(1L);
        otp.setEmail("test@example.com");
        otp.setOtp("123456");
        otp.setPurpose(UserOtp.OtpPurpose.REGISTRATION);
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        otp.setVerified(false);

        assertEquals(1L, otp.getId());
        assertEquals("test@example.com", otp.getEmail());
        assertEquals("123456", otp.getOtp());
        assertEquals(UserOtp.OtpPurpose.REGISTRATION, otp.getPurpose());
        assertFalse(otp.isExpired());
        assertFalse(otp.isVerified());

        UserOtp expiredOtp = UserOtp.builder()
                .expiryTime(LocalDateTime.now().minusMinutes(1))
                .build();
        assertTrue(expiredOtp.isExpired());
    }
}
