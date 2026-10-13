package edu.macslab.board;
import org.springframework.context.annotation.*;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
@Configuration
public class OpenApiConfig {
    @Bean OpenAPI apiDescription() {
        return new OpenAPI().info(new Info().title("Campus Board API").version("1.0")
            .description("Week 07 practice. Posts are public. Only an author can edit or delete their post."))
            .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
