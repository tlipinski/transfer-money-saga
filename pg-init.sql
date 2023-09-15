CREATE TABLE IF NOT EXISTS public.balances
(
    id text COLLATE pg_catalog."default" NOT NULL,
    version integer NOT NULL,
    content json NOT NULL,
    CONSTRAINT balances_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.sagas
(
    id text COLLATE pg_catalog."default" NOT NULL,
    version integer NOT NULL,
    content json NOT NULL,
    CONSTRAINT sagas_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.outbox
(
    id text COLLATE pg_catalog."default" NOT NULL,
    topic text COLLATE pg_catalog."default" NOT NULL,
    key text COLLATE pg_catalog."default" NOT NULL,
    keyHash integer NOT NULL,
    message json NOT NULL,
    CONSTRAINT outbox_pkey PRIMARY KEY (id)
);
