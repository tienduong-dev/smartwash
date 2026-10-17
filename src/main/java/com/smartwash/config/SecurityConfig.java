package com.smartwash.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Spring Security chỉ dùng để: chống CSRF + mã hoá mật khẩu BCrypt.
 * Việc đăng nhập / phân quyền (auth, admin, customer) do AccessInterceptor xử lý,
 * giống middleware của bản Laravel.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Cổng thanh toán gọi về server nên không có token CSRF
                        .ignoringRequestMatchers("/payment/momo/ipn", "/payment/vnpay/ipn"))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable());
        return http.build();
    }

    /** Laravel dùng bcrypt ($2y$) -> BCryptPasswordEncoder đọc được, tài khoản cũ đăng nhập bình thường */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
