-- Add target_roles column to courses for storing student role filters
IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.courses')
      AND name = 'target_roles'
)
BEGIN
    ALTER TABLE dbo.courses
        ADD target_roles NVARCHAR(255) NULL;
END;
