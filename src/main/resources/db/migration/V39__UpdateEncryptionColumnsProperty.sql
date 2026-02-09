

DO $$
BEGIN
    -- fees_update_result
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'title_order'
          AND column_name = 'fees_update_result'
          AND data_type <> 'text'
    ) THEN
        ALTER TABLE "title_order"
        ALTER COLUMN fees_update_result TYPE text USING fees_update_result::text;
    END IF;

    -- fees_details
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'title_order'
          AND column_name = 'fees_details'
          AND data_type <> 'text'
    ) THEN
        ALTER TABLE "title_order"
        ALTER COLUMN fees_details TYPE text USING fees_details::text;
    END IF;

    -- fees_request
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'title_order'
          AND column_name = 'fees_request'
          AND data_type <> 'text'
    ) THEN
        ALTER TABLE "title_order"
        ALTER COLUMN fees_request TYPE text USING fees_request::text;
    END IF;

    -- fees_response
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'title_order'
          AND column_name = 'fees_response'
          AND data_type <> 'text'
    ) THEN
        ALTER TABLE "title_order"
        ALTER COLUMN fees_response TYPE text USING fees_response::text;
    END IF;

    -- order_detail
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'title_order'
          AND column_name = 'order_detail'
          AND data_type <> 'text'
    ) THEN
        ALTER TABLE "title_order"
        ALTER COLUMN order_detail TYPE text USING order_detail::text;
    END IF;
END $$;
