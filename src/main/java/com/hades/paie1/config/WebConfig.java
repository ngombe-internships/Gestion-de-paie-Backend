package com.hades.paie1.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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

    // UTILISE LE OBJECTMAPPER EXISTANT DE JACKSONCONFIG
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Trouver le convertisseur JSON existant et le configurer
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;

                // Configuration ObjectMapper SANS CRÉER UN NOUVEAU BEAN
                ObjectMapper objectMapper = jsonConverter.getObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

                // S'assurer que JSON supporte UTF-8
                jsonConverter.setSupportedMediaTypes(Arrays.asList(
                        MediaType.APPLICATION_JSON,
                        new MediaType("application", "json", StandardCharsets.UTF_8)
                ));
                break;
            }
        }

        // Ajouter le convertisseur pour les fichiers PDF/Images
        ByteArrayHttpMessageConverter byteArrayConverter = new ByteArrayHttpMessageConverter();
        byteArrayConverter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_PDF,
                MediaType.APPLICATION_OCTET_STREAM,
                MediaType.IMAGE_JPEG,
                MediaType.IMAGE_PNG
        ));
        converters.add(byteArrayConverter);

        // Configurer le convertisseur String pour UTF-8 (SANS JSON)
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof StringHttpMessageConverter) {
                StringHttpMessageConverter stringConverter = (StringHttpMessageConverter) converter;
                stringConverter.setSupportedMediaTypes(Arrays.asList(
                        MediaType.TEXT_HTML,
                        MediaType.TEXT_PLAIN,
                        new MediaType("text", "html", StandardCharsets.UTF_8),
                        new MediaType("text", "plain", StandardCharsets.UTF_8)
                        // PAS DE APPLICATION_JSON ici !
                ));
                break;
            }
        }
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

    // PAS DE BEAN OBJECTMAPPER ICI - IL EXISTE DÉJÀ DANS JACKSONCONFIG
}