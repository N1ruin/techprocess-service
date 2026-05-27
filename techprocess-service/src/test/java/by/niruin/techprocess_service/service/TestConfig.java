package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.security.JwtParser;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class TestConfig {
    @Bean
    @Primary
    public JwtParser jwtParser() {
        return new JwtParser() {
            @Override
            public String getFirstName() {
                return "Евгений";
            }

            @Override
            public String getLastName() {
                return "Лагун";
            }

            @Override
            public String getFatherName() {
                return "Сергеевич";
            }

            @Override
            public String getClaim(String claimName) { return null; }
        };
    }

    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
