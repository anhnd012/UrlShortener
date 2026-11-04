ALTER TABLE short_url
    ADD COLUMN owner_id UUID NOT NULL;

ALTER TABLE short_url
    ADD CONSTRAINT fk_short_url_owner
        FOREIGN KEY (owner_id)
            REFERENCES app_user(id);

CREATE INDEX ix_short_url_owner_created
    ON short_url (owner_id, created_at);
