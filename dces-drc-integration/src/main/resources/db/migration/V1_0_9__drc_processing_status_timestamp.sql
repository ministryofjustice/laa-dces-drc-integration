
ALTER TABLE drc_processing_status DROP COLUMN detail;
ALTER TABLE drc_processing_status ADD COLUMN drc_processing_timestamp TIMESTAMP WITH TIME ZONE;