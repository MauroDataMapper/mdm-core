CREATE TABLE core.async_job (
    id                UUID         NOT NULL
        PRIMARY KEY,
    version           BIGINT       NOT NULL,
    date_created      TIMESTAMP    NOT NULL,
    last_updated      TIMESTAMP    NOT NULL,
    path              TEXT,
    started_by_user   VARCHAR(255) NOT NULL,
    date_time_started TIMESTAMP    NOT NULL,
    job_name          VARCHAR(255) NOT NULL,
    created_by        VARCHAR(255) NOT NULL,
    status            VARCHAR(255) NOT NULL,
    message           TEXT
);

CREATE INDEX asyncjob_created_by_idx
    ON core.async_job(created_by);

