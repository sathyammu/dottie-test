CREATE TABLE notifications (
    id SERIAL PRIMARY KEY,
    task_sequence int NOT NULL,
    status VARCHAR NOT NULL,
    created_at TIMESTAMP,
    last_updated_at TIMESTAMP,
    delivered_at TIMESTAMP,
    request_id VARCHAR
);
