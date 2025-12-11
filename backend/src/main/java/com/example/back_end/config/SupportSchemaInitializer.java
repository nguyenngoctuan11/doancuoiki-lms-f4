package com.example.back_end.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.dao.DataAccessException;

@Component
public class SupportSchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(SupportSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean autoMigrate;

    public SupportSchemaInitializer(JdbcTemplate jdbcTemplate,
                                    @Value("${app.support.auto-migrate:true}") boolean autoMigrate) {
        this.jdbcTemplate = jdbcTemplate;
        this.autoMigrate = autoMigrate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSupportSchema() {
        if (!autoMigrate) {
            log.info("Support schema auto-migrate disabled (app.support.auto-migrate=false)");
            return;
        }
        try {
            createSupportThreads();
            createSupportMessages();
            createSupportAttachments();
            createSupportRatings();
        } catch (DataAccessException ex) {
            log.error("Không thể tạo bảng support chat, vui lòng kiểm tra kết nối DB", ex);
        }
    }

    private void createSupportThreads() {
        String sql = """
            IF OBJECT_ID('dbo.support_threads', 'U') IS NULL
            BEGIN
                CREATE TABLE dbo.support_threads (
                    id BIGINT IDENTITY(1,1) PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    manager_id BIGINT NULL,
                    course_id BIGINT NULL,
                    topic NVARCHAR(64) NOT NULL,
                    subject NVARCHAR(255) NULL,
                    origin NVARCHAR(64) NULL,
                    status NVARCHAR(32) NOT NULL CONSTRAINT df_support_threads_status DEFAULT N'new',
                    priority NVARCHAR(16) NULL,
                    channel NVARCHAR(32) NULL,
                    metadata NVARCHAR(MAX) NULL,
                    last_message_preview NVARCHAR(512) NULL,
                    last_message_at DATETIME2 NULL,
                    last_sender NVARCHAR(16) NULL,
                    last_student_activity_at DATETIME2 NULL,
                    last_manager_activity_at DATETIME2 NULL,
                    has_unread_for_student BIT NOT NULL CONSTRAINT df_support_threads_unread_student DEFAULT 0,
                    has_unread_for_manager BIT NOT NULL CONSTRAINT df_support_threads_unread_manager DEFAULT 1,
                    created_at DATETIME2 NOT NULL CONSTRAINT df_support_threads_created DEFAULT GETUTCDATE(),
                    updated_at DATETIME2 NOT NULL CONSTRAINT df_support_threads_updated DEFAULT GETUTCDATE(),
                    closed_at DATETIME2 NULL,
                    CONSTRAINT fk_support_threads_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
                    CONSTRAINT fk_support_threads_manager FOREIGN KEY (manager_id) REFERENCES dbo.users(id),
                    CONSTRAINT fk_support_threads_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id),
                    CONSTRAINT ck_support_threads_status CHECK (status IN (N'new', N'in_progress', N'waiting_student', N'closed'))
                );

                CREATE INDEX ix_support_threads_status_created ON dbo.support_threads(status, created_at);
                CREATE INDEX ix_support_threads_manager_status ON dbo.support_threads(manager_id, status) WHERE manager_id IS NOT NULL;
                CREATE INDEX ix_support_threads_student_status ON dbo.support_threads(student_id, status);
            END;
            """;
        jdbcTemplate.execute(sql);
    }

    private void createSupportMessages() {
        String sql = """
            IF OBJECT_ID('dbo.support_messages', 'U') IS NULL
            BEGIN
                CREATE TABLE dbo.support_messages (
                    id BIGINT IDENTITY(1,1) PRIMARY KEY,
                    thread_id BIGINT NOT NULL,
                    sender_id BIGINT NULL,
                    sender_type NVARCHAR(16) NOT NULL,
                    content NVARCHAR(MAX) NOT NULL,
                    created_at DATETIME2 NOT NULL CONSTRAINT df_support_messages_created DEFAULT GETUTCDATE(),
                    CONSTRAINT fk_support_messages_thread FOREIGN KEY (thread_id) REFERENCES dbo.support_threads(id) ON DELETE CASCADE,
                    CONSTRAINT fk_support_messages_sender FOREIGN KEY (sender_id) REFERENCES dbo.users(id),
                    CONSTRAINT ck_support_messages_sender_type CHECK (sender_type IN (N'student', N'manager', N'system', N'bot'))
                );
                CREATE INDEX ix_support_messages_thread ON dbo.support_messages(thread_id, id);
            END;
            """;
        jdbcTemplate.execute(sql);
    }

    private void createSupportAttachments() {
        String sql = """
            IF OBJECT_ID('dbo.support_message_attachments', 'U') IS NULL
            BEGIN
                CREATE TABLE dbo.support_message_attachments (
                    message_id BIGINT NOT NULL,
                    attachment_url NVARCHAR(1024) NOT NULL,
                    PRIMARY KEY (message_id, attachment_url),
                    CONSTRAINT fk_support_msg_attachments_message FOREIGN KEY (message_id) REFERENCES dbo.support_messages(id) ON DELETE CASCADE
                );
            END;
            """;
        jdbcTemplate.execute(sql);
    }

    private void createSupportRatings() {
        String sql = """
            IF OBJECT_ID('dbo.support_ratings', 'U') IS NULL
            BEGIN
                CREATE TABLE dbo.support_ratings (
                    id BIGINT IDENTITY(1,1) PRIMARY KEY,
                    thread_id BIGINT NOT NULL UNIQUE,
                    student_id BIGINT NOT NULL,
                    rating TINYINT NOT NULL,
                    comment NVARCHAR(1000) NULL,
                    created_at DATETIME2 NOT NULL CONSTRAINT df_support_ratings_created DEFAULT GETUTCDATE(),
                    CONSTRAINT fk_support_ratings_thread FOREIGN KEY (thread_id) REFERENCES dbo.support_threads(id) ON DELETE CASCADE,
                    CONSTRAINT fk_support_ratings_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
                    CONSTRAINT ck_support_ratings_value CHECK (rating BETWEEN 1 AND 5)
                );
            END;
            """;
        jdbcTemplate.execute(sql);
    }
}
