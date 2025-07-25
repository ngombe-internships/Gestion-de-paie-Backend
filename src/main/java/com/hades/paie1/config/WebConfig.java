package com.hades.paie1.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;
    private final Environment environment;

    public WebConfig(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configuration des ressources statiques seulement en développement
        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            if (uploadDir != null && !uploadDir.isEmpty()) {
                String normalizedUploadDir = uploadDir.replace("\\", "/");
                registry.addResourceHandler("/logos/**")
                        .addResourceLocations("file:///" + normalizedUploadDir + "/");
            }
        }

        // Ressources statiques communes
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static", "classpath:/public/", "classpath:/META-INF/resources/");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // IMPORTANT : JSON en PREMIER pour OpenAPI/Swagger
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                new MediaType("application", "json", StandardCharsets.UTF_8)
        ));

        // Configuration de l'ObjectMapper pour JSON
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        jsonConverter.setObjectMapper(objectMapper);
        converters.add(0, jsonConverter); // JSON en premier

        // Convertisseur pour les bytes (PDF, images, etc.)
        ByteArrayHttpMessageConverter byteArrayConverter = new ByteArrayHttpMessageConverter();
        byteArrayConverter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_PDF,
                MediaType.APPLICATION_OCTET_STREAM,
                MediaType.IMAGE_JPEG,
                MediaType.IMAGE_PNG
        ));
        converters.add(byteArrayConverter);

        // String converter APRÈS JSON pour éviter les conflits avec OpenAPI
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setSupportedMediaTypes(Arrays.asList(
                MediaType.TEXT_HTML,
                MediaType.TEXT_PLAIN,
                new MediaType("text", "html", StandardCharsets.UTF_8),
                new MediaType("text", "plain", StandardCharsets.UTF_8)
                // Retirer APPLICATION_JSON d'ici !
        ));
        converters.add(stringConverter);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .defaultContentType(MediaType.APPLICATION_JSON)
                .favorParameter(false)
                .parameterName("mediaType")
                .ignoreAcceptHeader(false)
                .useRegisteredExtensionsOnly(false)
                .mediaType("html", MediaType.TEXT_HTML)
                .mediaType("pdf", MediaType.APPLICATION_PDF)
                .mediaType("json", MediaType.APPLICATION_JSON);
    }
}