CREATE SEQUENCE  IF NOT EXISTS primary_sequence START WITH 10000 INCREMENT BY 1;

CREATE TABLE repositories (
    id BIGINT NOT NULL,
    repo_name TEXT NOT NULL,
    repo_created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    repo_updated_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    clone_url TEXT NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT repositories_pkey PRIMARY KEY (id)
);

ALTER TABLE repositories ADD CONSTRAINT unique_repositories_repo_name UNIQUE (repo_name);
