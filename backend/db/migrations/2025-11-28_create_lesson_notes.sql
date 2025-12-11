-- Migration: create tables for lesson notes & comments
IF OBJECT_ID('dbo.lesson_notes', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.lesson_notes (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        course_id BIGINT NOT NULL,
        lesson_id BIGINT NOT NULL,
        student_id BIGINT NOT NULL,
        content NVARCHAR(2000) NOT NULL,
        last_comment_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_lesson_notes_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_lesson_notes_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT fk_lesson_notes_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE,
        CONSTRAINT fk_lesson_notes_lesson FOREIGN KEY (lesson_id) REFERENCES dbo.lessons(id),
        CONSTRAINT fk_lesson_notes_student FOREIGN KEY (student_id) REFERENCES dbo.users(id) ON DELETE CASCADE
    );
    CREATE INDEX ix_lesson_notes_lesson ON dbo.lesson_notes(lesson_id, created_at);
    CREATE INDEX ix_lesson_notes_course ON dbo.lesson_notes(course_id, last_comment_at DESC);
END;

IF OBJECT_ID('dbo.lesson_note_comments', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.lesson_note_comments (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        note_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        author_role NVARCHAR(32) NULL,
        content NVARCHAR(2000) NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_lesson_note_comments_created DEFAULT SYSUTCDATETIME(),
        CONSTRAINT fk_lesson_note_comments_note FOREIGN KEY (note_id) REFERENCES dbo.lesson_notes(id) ON DELETE CASCADE,
        CONSTRAINT fk_lesson_note_comments_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
    CREATE INDEX ix_lesson_note_comments_note ON dbo.lesson_note_comments(note_id, created_at);
END;
