-- Migration to add a generic trigger for column version tracking

-- Create the audit_log table
CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    table_name TEXT NOT NULL,
    column_name TEXT NOT NULL,
    row_id TEXT NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_at TIMESTAMP DEFAULT NOW()
);


CREATE OR REPLACE FUNCTION generic_audit_trigger()
RETURNS TRIGGER AS $$
DECLARE
    column_name TEXT;
    old_value TEXT;
    new_value TEXT;
    audit_rows TEXT[] := ARRAY[]::TEXT[]; -- Array to store rows for batch insert
BEGIN
    -- Loop through each column in the trigger's arguments
     FOR i IN array_lower(TG_ARGV, 1)..array_upper(TG_ARGV, 1) LOOP
		column_name := TG_ARGV[i];
        BEGIN
            -- Attempt to cast OLD and NEW values to TEXT
            old_value := COALESCE(to_jsonb(OLD) ->> column_name::TEXT, NULL);
            new_value := COALESCE(to_jsonb(NEW) ->> column_name::TEXT, NULL);
        EXCEPTION WHEN others THEN
            -- If casting fails, set values to NULL
            old_value := NULL;
            new_value := NULL;
        END;

        -- Check if the column value has changed
        IF old_value IS DISTINCT FROM new_value THEN
            -- Build the row for batch insert
            audit_rows := audit_rows || format(
                '(%L, %L, %L, %L, %L, %L)',
                TG_TABLE_NAME,         -- Source table name
                column_name,           -- Column name
                NEW.id::TEXT,          -- Row ID (assuming 'id' is the PK)
                old_value,             -- Old column value
                new_value,             -- New column value
                NOW()                  -- Timestamp of change
            );
        END IF;
    END LOOP;

    -- Perform batch insert if there are rows to insert
    IF array_length(audit_rows, 1) IS NOT NULL THEN
        EXECUTE format(
            'INSERT INTO audit_log (table_name, column_name, row_id, old_value, new_value, changed_at) VALUES %s',
            array_to_string(audit_rows, ',')
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER audit_tenant_settings
AFTER UPDATE OF meta ON tenant_settings
FOR EACH ROW
EXECUTE FUNCTION generic_audit_trigger('meta');