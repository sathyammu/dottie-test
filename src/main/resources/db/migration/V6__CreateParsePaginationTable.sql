CREATE TABLE parse_pagination_results (
    id BIGSERIAL PRIMARY KEY,
    batch_checksum TEXT NOT NULL,
    batch_number INT NOT NULL,
    metadata_id BIGINT NOT NULL,
    output JSONB NOT NULL,
    CONSTRAINT fk_metadata FOREIGN KEY (metadata_id) REFERENCES document_metadata (id)
);