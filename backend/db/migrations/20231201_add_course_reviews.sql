IF OBJECT_ID('dbo.course_reviews', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.course_reviews;
END
GO

CREATE TABLE dbo.course_reviews (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    course_id         BIGINT       NOT NULL,
    instructor_id     BIGINT       NOT NULL,
    student_id        BIGINT       NOT NULL,
    course_score      INT          NOT NULL CHECK (course_score BETWEEN 1 AND 5),
    instructor_score  INT          NOT NULL CHECK (instructor_score BETWEEN 1 AND 5),
    support_score     INT          NOT NULL CHECK (support_score BETWEEN 1 AND 5),
    would_recommend   BIT          NULL,
    comment           NVARCHAR(2000) NULL,
    highlight         NVARCHAR(500)  NULL,
    improvement       NVARCHAR(500)  NULL,
    status            NVARCHAR(16)   NOT NULL DEFAULT N'pending',
    admin_note        NVARCHAR(1000) NULL,
    created_at        DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at        DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

ALTER TABLE dbo.course_reviews
ADD CONSTRAINT fk_course_reviews_course
FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE;
GO

ALTER TABLE dbo.course_reviews
ADD CONSTRAINT fk_course_reviews_instructor
FOREIGN KEY (instructor_id) REFERENCES dbo.users(id);
GO

ALTER TABLE dbo.course_reviews
ADD CONSTRAINT fk_course_reviews_student
FOREIGN KEY (student_id) REFERENCES dbo.users(id);
GO

CREATE INDEX idx_course_reviews_course
    ON dbo.course_reviews(course_id, status, created_at DESC);
GO

CREATE INDEX idx_course_reviews_instructor
    ON dbo.course_reviews(instructor_id, status);
GO

CREATE UNIQUE INDEX uq_course_reviews_course_student
    ON dbo.course_reviews(course_id, student_id);
GO
