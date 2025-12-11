-- Migration: add categories column to courses for English topic tagging
IF COL_LENGTH('dbo.courses','categories') IS NULL
BEGIN
    ALTER TABLE dbo.courses
    ADD categories VARCHAR(255);
END

-- Optional: populate existing rows with default empty string if needed
UPDATE dbo.courses
SET categories = ''
WHERE categories IS NULL;
