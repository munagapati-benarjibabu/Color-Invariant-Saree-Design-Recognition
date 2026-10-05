package com.saree.search.config;

import com.saree.search.service.SareeImageIndexer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Makes the hosted preview usable immediately; production imports through the protected admin endpoint. */
@Configuration
@Profile("demo")
public class DemoCatalogueConfiguration {
  @Bean CommandLineRunner loadDemoCatalogue(SareeImageIndexer indexer) {
    return args -> indexer.reindex();
  }
}
