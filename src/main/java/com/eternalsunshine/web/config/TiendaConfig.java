package com.eternalsunshine.web.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TiendaConfig {

    @Bean
    public CurrentTenantIdentifierResolver<Integer> tiendaResolver() {

        return new CurrentTenantIdentifierResolver<>() {

            @Override
            public Integer resolveCurrentTenantIdentifier() {
                return TiendaContexto.actual();
            }

            @Override
            public boolean validateExistingCurrentSessions() {
                return false;
            }
        };
    }

    @Bean
    public HibernatePropertiesCustomizer tiendaHibernateCustomizer(CurrentTenantIdentifierResolver<Integer> resolver) {
        return propiedades -> propiedades.put("hibernate.tenant_identifier_resolver", resolver);
    }
}
