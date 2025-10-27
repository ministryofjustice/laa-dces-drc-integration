CREATE SEQUENCE IF NOT EXISTS case_submission_error_gen_seq
    INCREMENT 1
    START 1;

CREATE TABLE IF NOT EXISTS case_submission_error
(
    id
    SERIAL
    PRIMARY
    KEY,
    maat_id
    INT,
    concor_contribution_id
    INT,
    fdc_id
    INT,
    title
    VARCHAR
(
    255
),
    status INT,
    detail TEXT,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
