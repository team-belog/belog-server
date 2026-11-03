package org.com.belog.prelog.repository

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Testcontainers(disabledWithoutDocker = true)
class PreLogPlanTypeMigrationTest {
    @Test
    fun `기존 LINK 계획을 LOCATION으로 변경하고 유형 제약 조건을 교체한다`() {
        flyway(target = "15").migrate()
        insertPlan(id = 1L, type = "LINK")

        flyway().migrate()

        assertEquals("LOCATION", findPlanType(1L))
        assertFailsWith<SQLException> {
            insertPlan(id = 2L, type = "LINK")
        }
        insertPlan(id = 3L, type = "LOCATION")
        assertEquals("LOCATION", findPlanType(3L))
    }

    private fun flyway(target: String? = null): Flyway {
        val configuration =
            Flyway
                .configure()
                .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
                .locations("classpath:db/migration")
        target?.let(configuration::target)
        return configuration.load()
    }

    private fun insertPlan(
        id: Long,
        type: String,
    ) {
        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("SET FOREIGN_KEY_CHECKS = 0")
                statement.executeUpdate(
                    """
                    INSERT INTO pre_log_plans (
                        id, meeting_id, created_by_group_member_id, type, category, title, url, content,
                        created_at, updated_at, pinned, location_status
                    ) VALUES (
                        $id, 1, 1, '$type', 'RESTAURANT', '장소', 'https://place.map.kakao.com/123456', NULL,
                        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), FALSE, 'NOT_APPLICABLE'
                    )
                    """.trimIndent(),
                )
            }
        }
    }

    private fun findPlanType(id: Long): String =
        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
            connection.prepareStatement("SELECT type FROM pre_log_plans WHERE id = ?").use { statement ->
                statement.setLong(1, id)
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "계획을 찾을 수 없습니다." }
                    resultSet.getString("type")
                }
            }
        }

    companion object {
        @Container
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
