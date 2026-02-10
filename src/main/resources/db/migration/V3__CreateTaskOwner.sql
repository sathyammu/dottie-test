CREATE TABLE public.task_owner (
	"owner" varchar(250) NOT NULL,
	heartbeat timestamp NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'::text),
	CONSTRAINT "TASK_OWNER_pkey" PRIMARY KEY (owner)
);
CREATE INDEX task_owner_idx ON public.task_owner USING btree (heartbeat);