package com.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration pour activer le scheduling (nettoyage automatique des tokens)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
