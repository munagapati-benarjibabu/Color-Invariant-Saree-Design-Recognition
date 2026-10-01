package com.saree.search.config;

import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.Configuration; import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Path;
@Configuration public class WebConfig implements WebMvcConfigurer {
  @Value("${saree.image.directory:./saree-images}") String directory;
  @Override public void addResourceHandlers(ResourceHandlerRegistry registry) { registry.addResourceHandler("/saree-images/**").addResourceLocations(Path.of(directory).toAbsolutePath().normalize().toUri().toString()); }
}
