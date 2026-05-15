package com.stocklab.core.config;

import com.stocklab.core.api.security.AuthenticatedUserIdArgumentResolver;
import com.stocklab.core.api.security.UserIdentityInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserIdentityInterceptor userIdentityInterceptor;
    private final AuthenticatedUserIdArgumentResolver authenticatedUserIdArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userIdentityInterceptor)
                .addPathPatterns("/api/orders/**", "/api/executions/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedUserIdArgumentResolver);
    }
}
