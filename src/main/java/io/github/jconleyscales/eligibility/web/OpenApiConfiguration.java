package io.github.jconleyscales.eligibility.web;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
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

	/**
	 * springdoc documents a {@code @Nullable} enum property as {@code "type": ["string", "null"]} but leaves null out
	 * of its {@code enum}, and JSON Schema then rejects a null value. This adds null to the values of every nullable
	 * enum property, such as an inactive coverage's {@code planType}.
	 */
	@Bean
	OpenApiCustomizer nullableEnumsListNull() {
		return openApi -> {
			for (Schema<?> schema : openApi.getComponents().getSchemas().values()) {
				if (schema.getProperties() != null) {
					schema.getProperties()
						.values()
						.stream()
						.filter(OpenApiConfiguration::isNullableEnumWithoutNull)
						.forEach(OpenApiConfiguration::addNullToEnum);
				}
			}
		};
	}

	private static boolean isNullableEnumWithoutNull(Schema<?> property) {
		return property.getEnum() != null && property.getTypes() != null && property.getTypes().contains("null")
				&& !property.getEnum().contains(null);
	}

	/**
	 * Replaces the list rather than adding to it, because another property may share the same list.
	 */
	@SuppressWarnings("unchecked")
	private static void addNullToEnum(Schema<?> property) {
		var values = new ArrayList<Object>(property.getEnum());
		values.add(null);
		((Schema<Object>) property).setEnum(values);
	}

}
