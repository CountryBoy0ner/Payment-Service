package com.innowise.config;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LiquibaseMongoRunnerConfig {

    @Value("${app.liquibase.enabled:true}")
    private boolean enabled;

    @Value("${app.liquibase.url}")
    private String mongoUrl;

    @Value("${app.liquibase.change-log}")
    private String changeLog;

    @Bean
    public ApplicationRunner liquibaseMongoRunner() {
        return args -> {
            if (!enabled) {
                log.info("Liquibase(Mongo) disabled");
                return;
            }

            String changeLogPath = changeLog.startsWith("classpath:")
                    ? changeLog.substring("classpath:".length())
                    : changeLog;

            ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(getClass().getClassLoader());

            log.info("Running Liquibase(Mongo). url={}, changelog={}", mongoUrl, changeLogPath);

            Database database = null;
            try {
                database = DatabaseFactory.getInstance().openDatabase(
                        mongoUrl,
                        null,
                        null,
                        null,
                        resourceAccessor
                );

                Liquibase liquibase = new Liquibase(changeLogPath, resourceAccessor, database);
                liquibase.update(new Contexts(), new LabelExpression());

                log.info("Liquibase(Mongo) done");
            } finally {
                if (database != null) {
                    database.close();
                }
            }
        };
    }
}
