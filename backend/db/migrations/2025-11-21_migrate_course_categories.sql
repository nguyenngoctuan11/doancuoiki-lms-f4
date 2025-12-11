-- Migration: move comma-separated categories data into course_categories join table
IF OBJECT_ID('dbo.course_categories') IS NOT NULL AND COL_LENGTH('dbo.courses','categories') IS NOT NULL
BEGIN
    INSERT INTO dbo.course_categories (course_id, category_id)
    SELECT DISTINCT c.id,
           cat.id
    FROM dbo.courses c
    CROSS APPLY (
        SELECT LTRIM(RTRIM(value)) AS slug
        FROM STRING_SPLIT(ISNULL(c.categories, ''), ',')
    ) AS parts
    JOIN dbo.categories cat ON LOWER(cat.slug) = LOWER(parts.slug)
    WHERE parts.slug <> ''
      AND NOT EXISTS (
          SELECT 1 FROM dbo.course_categories cc
          WHERE cc.course_id = c.id AND cc.category_id = cat.id
      );

    ALTER TABLE dbo.courses
    DROP COLUMN categories;
END
