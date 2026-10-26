package com.papererp.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paperErpOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("PaperERP Enterprise — واجهة برمجة تطبيقات مصانع الورق والكرتون")
                        .description("Arabic-first Multi-tenant Industrial Paper Manufacturing ERP Backend API.\n\n" +
                                "يوفر النظام إدارة كاملة وشاملة لعمليات إنتاج بكر الجامبو، عمليات التقطيع الذري مع توازن الكتلة وحساب فروق الموازين، " +
                                "بوابات مراقبة الجودة المعملية، ودفتر أستاذ حركات المخزون غير القابل للتعديل.")
                        .version("1.0.0")
                        .contact(new Contact().name("PaperERP Architecture Team").email("architecture@papererp.com"))
                        .license(new License().name("Proprietary").url("https://papererp.com/license")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT Bearer token obtained from /api/v1/auth/login")));
    }
}
