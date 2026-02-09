CREATE TABLE bayequity_package_list (
    id SERIAL PRIMARY KEY,
    loan_number TEXT UNIQUE NOT NULL,
    task_tree_id INTEGER REFERENCES task_tree(id)
);
