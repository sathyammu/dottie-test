CREATE TABLE task_outputs (
    id SERIAL PRIMARY KEY,
    task_sequence int NOT NULL,
    output JSONB
);
