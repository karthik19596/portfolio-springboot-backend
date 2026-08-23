package com.premkarthik.portfolio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Prints which database the application actually bound to.
 * The default profile is an in-memory H2 that is recreated empty on every
 * start, which is indistinguishable from a MySQL run until a login fails with
 * "Bad credentials".
 */
@Component
public class StartupInfoLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupInfoLogger.class);
    private static final String LINE = "=".repeat(72);

    private final Environment environment;

    public StartupInfoLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupInfo() {
        String[] active = environment.getActiveProfiles();
        String profiles = active.length == 0 ? "default" : String.join(", ", active);
        String url = environment.getProperty("spring.datasource.url", "unknown");
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto", "unset");
        boolean auditEnabled = environment.getProperty("audit.log.enabled", Boolean.class, false);
        boolean inMemory = url.contains(":mem:");

        log.info(LINE);
        log.info("  Active profile(s) : {}", profiles);
        log.info("  Server port       : {}", resolvePort());
        log.info("  Database          : {} ({})", url, inMemory ? "in-memory, not persisted" : "persistent");
        log.info("  Schema mode       : {}", ddlAuto);
        log.info("  Audit logging     : {}", auditEnabled ? "enabled (MongoDB)" : "disabled");
        log.info(LINE);

        if (inMemory) {
            log.warn("Running on an IN-MEMORY database. Every user and task is discarded on shutdown,");
            log.warn("so accounts created in a previous run will fail to log in.");
            log.warn("For persistent MySQL data, start with the 'mongo' profile:");
            log.warn("  mvn spring-boot:run \"-Dspring-boot.run.profiles=mongo\"");
            log.warn("  (in IntelliJ: Run > Edit Configurations > Active profiles: mongo)");
        }
    }

    private String resolvePort() {
        String port = environment.getProperty("local.server.port");
        if (port != null) return port;
        return environment.getProperty("server.port", "8080");
    }
}
