package io.github.foundry4j.forge.observability;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;

/// Adds the `bearerAuth` security scheme to the generated OpenAPI document.
///
/// With the scheme in place Swagger UI offers an "Authorize" button, so protected
/// endpoints can be tried with a JWT without manual header editing.
public class BearerAuthOpenApiCustomizer implements OpenApiCustomizer {

    /// Name of the security scheme.
    public static final String SCHEME_NAME = "bearerAuth";

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        openApi.getComponents().addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));
    }
}
