-- Migration script to update audit_logs table
-- This script removes the oldValue column and renames newValue to changes
-- 
-- IMPORTANT: Run this script manually on your database before deploying the code changes.
-- If you're using Hibernate's ddl-auto=update, you may need to set it to 'validate' or 'none'
-- after running this migration to prevent Hibernate from trying to recreate the old columns.

-- Step 1: Drop the oldValue column (try both naming conventions)
ALTER TABLE audit_logs DROP COLUMN IF EXISTS old_value;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS "oldValue";

-- Step 2: Rename newValue column to changes (try both naming conventions)
-- For snake_case (default JPA/Hibernate naming)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'audit_logs' AND column_name = 'new_value') THEN
        ALTER TABLE audit_logs RENAME COLUMN new_value TO changes;
    END IF;
END $$;

-- For camelCase (if using a custom naming strategy)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'audit_logs' AND column_name = 'newValue') THEN
        ALTER TABLE audit_logs RENAME COLUMN "newValue" TO changes;
    END IF;
END $$;

-- Verify the changes
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'audit_logs' 
  AND column_name IN ('old_value', 'oldValue', 'new_value', 'newValue', 'changes')
ORDER BY column_name;

