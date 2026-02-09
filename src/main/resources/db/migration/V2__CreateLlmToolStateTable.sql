CREATE TABLE llm_tool_state (
    sessionId TEXT PRIMARY KEY,
    state JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tool_name VARCHAR(255),
    qualifier VARCHAR(255)
);
