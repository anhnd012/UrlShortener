CREATE TABLE short_url (
       id UUID PRIMARY KEY,
       short_code VARCHAR(8) NOT NULL,
       long_url VARCHAR(2048) NOT NULL,
       status VARCHAR(20) NOT NULL,
       valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
       expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
       created_at TIMESTAMP WITH TIME ZONE,
       updated_at TIMESTAMP WITH TIME ZONE,
       CONSTRAINT uq_short_url_short_code UNIQUE (short_code)
);