CREATE SEQUENCE IF NOT EXISTS drc_processing_status_gen_seq
    INCREMENT 1
    START 1;

CREATE TABLE IF NOT EXISTS drc_processing_status
(
    id SERIAL PRIMARY KEY,
    maat_id INT,
    concor_contribution_id INT,
    fdc_id INT,
    title VARCHAR( 255),
    detail TEXT,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
