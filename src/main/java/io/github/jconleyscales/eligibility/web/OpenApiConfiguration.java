package io.github.jconleyscales.eligibility.web;

import java.util.List;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The parts of the OpenAPI document that don't come from the controllers. springdoc generates the rest and
 * serves it at {@code /v3/api-docs}; the committed copy is {@code docs/api/openapi.json}.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

	/**
	 * Where {@code docker compose up} serves the API. It is fixed because springdoc would otherwise take it from
	 * each request, and the generated contract would change with the port.
	 */
	private static final String SERVER_URL = "http://localhost:8080";

	@Bean
	OpenAPI openApi() {
		return new OpenAPI()
			.info(new Info().title("Eligibility & Benefits API")
				.version("v1")
				.description("""
						Answers an Eligibility check for one Member, one Procedure, and one Service date: is the \
						Member's coverage active, what is their Plan's Benefit for the procedure, does it need prior \
						authorization, and what is their cost share. Every Member and Plan is synthetic. The service \
						runs locally and has no authentication.""")
				.license(new License().name("MIT")
					.url("https://github.com/jconley-scales/eligibility-benefits-service/blob/main/LICENSE")))
			.externalDocs(new ExternalDocumentation().description("Source, glossary, and design decisions")
				.url("https://github.com/jconley-scales/eligibility-benefits-service"))
			.servers(List.of(new Server().url(SERVER_URL).description("Local: docker compose up")));
	}

}
