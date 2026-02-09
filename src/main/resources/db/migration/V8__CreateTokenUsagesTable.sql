ALTER TABLE task_tree
ADD CONSTRAINT pk_task_tree PRIMARY KEY (id);

CREATE TABLE token_usages (
    id BIGSERIAL PRIMARY KEY,
    task_tree_id INTEGER NOT NULL REFERENCES task_tree(id),
    inputs int default 0,
    outputs int default 0,
    request_start TIMESTAMP ,
    request_end TIMESTAMP
);
