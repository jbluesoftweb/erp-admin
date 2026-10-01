package lat.jbluesoft.erpadmin.core.config;

import lat.jbluesoft.erpadmin.core.security.PasswordForceChangeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final PasswordForceChangeInterceptor passwordForceChangeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(passwordForceChangeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/img/**", "/webjars/**",
                        "/login", "/logout", "/error");
    }
}
