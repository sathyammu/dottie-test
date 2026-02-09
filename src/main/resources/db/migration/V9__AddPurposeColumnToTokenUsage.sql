ALTER TABLE token_usages
ADD column  purpose jsonb not null default '{}'::jsonb;

