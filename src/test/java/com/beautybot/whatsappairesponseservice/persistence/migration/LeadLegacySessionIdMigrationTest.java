package com.beautybot.whatsappairesponseservice.persistence.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class LeadLegacySessionIdMigrationTest {

    @Test
    void sessionIdColumnBecomesNullableAfterV7Migration() throws Exception {
        String url = "jdbc:h2:mem:lead_legacy_session_id_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            String isNullable;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT IS_NULLABLE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE UPPER(TABLE_NAME) = 'LEADS'
                      AND UPPER(COLUMN_NAME) = 'SESSION_ID'
                    """);
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                isNullable = resultSet.getString(1);
            }

            assertThat(isNullable).isEqualTo("YES");

            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO LEADS (
                        CLINIC_ID,
                        CONVERSATION_SESSION_ID,
                        PHONE_NUMBER,
                        STATUS,
                        SOURCE,
                        TEMPERATURE,
                        SCORE,
                        CREATED_AT,
                        UPDATED_AT
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """)) {
                insert.setLong(1, 1L);
                insert.setObject(2, null);
                insert.setString(3, "5491112345678");
                insert.setString(4, "NEW");
                insert.setString(5, "UNKNOWN");
                insert.setString(6, "COLD");
                insert.setInt(7, 0);
                int inserted = insert.executeUpdate();
                assertThat(inserted).isEqualTo(1);
            }
        }
    }
}
