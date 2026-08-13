ALTER TABLE cargo_order
    ADD COLUMN msds_file_name VARCHAR(255) NULL AFTER requires_msds,
    ADD COLUMN msds_content_type VARCHAR(100) NULL AFTER msds_file_name,
    ADD COLUMN msds_data LONGBLOB NULL AFTER msds_content_type;
