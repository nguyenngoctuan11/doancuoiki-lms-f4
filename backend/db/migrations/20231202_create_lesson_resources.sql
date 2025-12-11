IF OBJECT_ID('dbo.lesson_resources', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.lesson_resources;
END
GO

CREATE TABLE dbo.lesson_resources (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    course_id       BIGINT       NOT NULL,
    lesson_id       BIGINT       NOT NULL,
    title           NVARCHAR(255) NOT NULL,
    description     NVARCHAR(1000) NULL,
    source_type     NVARCHAR(16)  NOT NULL DEFAULT N'file',
    storage_path    NVARCHAR(1024) NULL,
    file_url        NVARCHAR(600) NULL,
    external_url    NVARCHAR(1000) NULL,
    file_type       NVARCHAR(64)  NULL,
    file_size       BIGINT        NULL,
    visibility      NVARCHAR(32)  NOT NULL DEFAULT N'enrolled',
    status          NVARCHAR(16)  NOT NULL DEFAULT N'pending',
    download_count  INT           NOT NULL DEFAULT 0,
    created_by      BIGINT        NULL,
    created_at      DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

ALTER TABLE dbo.lesson_resources
ADD CONSTRAINT fk_lesson_resources_course
FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE;
GO

ALTER TABLE dbo.lesson_resources
ADD CONSTRAINT fk_lesson_resources_lesson
FOREIGN KEY (lesson_id) REFERENCES dbo.lessons(id) ON DELETE NO ACTION;
GO

ALTER TABLE dbo.lesson_resources
ADD CONSTRAINT fk_lesson_resources_user
FOREIGN KEY (created_by) REFERENCES dbo.users(id);
GO

CREATE INDEX idx_lesson_resources_course_lesson
    ON dbo.lesson_resources(course_id, lesson_id, status);
GO

CREATE INDEX idx_lesson_resources_status
    ON dbo.lesson_resources(status, visibility);
GO
