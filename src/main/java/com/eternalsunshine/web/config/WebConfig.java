package com.eternalsunshine.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private AvisosInterceptor avisosInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new CsrfOrigenInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error"
                );

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/logout",
                        "/registro",
                        "/terminos",
                        "/recuperar",
                        "/restablecer",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error"
                );

        registry.addInterceptor(new AdminInterceptor())
                .addPathPatterns(
                        "/usuarios",
                        "/usuarios/**",
                        "/solicitudes",
                        "/solicitudes/aprobar/**",
                        "/solicitudes/rechazar/**",
                        "/ventas/*/anular",
                        "/entradas/anular/**",
                        "/conteos/*/aplicar",
                        "/conteos/*/descartar",
                        "/productos/eliminar/**",
                        "/productos/exportar"
                );

        registry.addInterceptor(avisosInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error",
                        "/avisos"
                );
    }
}