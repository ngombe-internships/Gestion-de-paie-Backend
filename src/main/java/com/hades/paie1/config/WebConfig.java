package com.hades.paie1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String normalizedUploadDir = uploadDir.replace("\\", "/");
        registry.addResourceHandler("/logos/**")
                .addResourceLocations("file:///" + normalizedUploadDir + "/");
       registry.addResourceHandler("/**")
               .addResourceLocations("classpath:/static", "classpath:/public/", "classpath:/META-INF/resources/");
    }



}
