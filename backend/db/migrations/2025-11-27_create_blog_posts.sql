-- Migration: create blog_posts table for student blog workflow
IF OBJECT_ID('dbo.blog_posts', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.blog_posts (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        slug NVARCHAR(160) NOT NULL UNIQUE,
        title NVARCHAR(255) NOT NULL,
        summary NVARCHAR(500) NULL,
        content NVARCHAR(MAX) NOT NULL,
        thumbnail_url NVARCHAR(512) NULL,
        status NVARCHAR(16) NOT NULL CONSTRAINT df_blog_posts_status DEFAULT N'draft',
        rejection_reason NVARCHAR(500) NULL,
        author_id BIGINT NOT NULL,
        approver_id BIGINT NULL,
        submitted_at DATETIME2 NULL,
        approved_at DATETIME2 NULL,
        published_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_blog_posts_created DEFAULT GETUTCDATE(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_blog_posts_updated DEFAULT GETUTCDATE(),
        CONSTRAINT ck_blog_posts_status
            CHECK (status IN (N'draft', N'pending', N'published', N'rejected')),
        CONSTRAINT fk_blog_posts_author FOREIGN KEY (author_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
        CONSTRAINT fk_blog_posts_approver FOREIGN KEY (approver_id) REFERENCES dbo.users(id)
    );
END;
