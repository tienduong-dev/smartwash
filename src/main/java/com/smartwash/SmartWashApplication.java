package com.smartwash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SmartWashApplication {

    public static void main(String[] args) {
        // Bản Laravel dùng timezone UTC -> giữ nguyên để giờ hiển thị giống hệt
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        SpringApplication.run(SmartWashApplication.class, args);
    }
}
