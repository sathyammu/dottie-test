alter table llm_tool_state  add constraint
unique_llm_tool_state unique(sessionid, tool_name, qualifier);