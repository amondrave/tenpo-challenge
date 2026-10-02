CREATE TABLE call_history (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at     TIMESTAMPTZ   NOT NULL,
    method          VARCHAR(10)   NOT NULL,
    path            VARCHAR(2048) NOT NULL,
    request_params  JSONB         NOT NULL,
    response_status INT           NOT NULL,
    response_body   JSONB         NOT NULL
);

CREATE INDEX idx_call_history_occurred_at ON call_history (occurred_at DESC, id DESC);
