CREATE TABLE workplace (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    owner_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_workplace_owner FOREIGN KEY (owner_id) REFERENCES app_user(id)
);

CREATE INDEX ix_workplace_owner ON workplace (owner_id);

CREATE TABLE workplace_member (
    id UUID PRIMARY KEY,
    workplace_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_workplace_member_workplace FOREIGN KEY (workplace_id) REFERENCES workplace(id) ON DELETE CASCADE,
    CONSTRAINT fk_workplace_member_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT uq_workplace_member UNIQUE (workplace_id, user_id)
);

CREATE INDEX ix_workplace_member_user ON workplace_member (user_id);
CREATE INDEX ix_workplace_member_workplace ON workplace_member (workplace_id);

ALTER TABLE short_url
    ADD COLUMN workplace_id UUID;

ALTER TABLE short_url
    ADD CONSTRAINT fk_short_url_workplace
        FOREIGN KEY (workplace_id)
            REFERENCES workplace(id)
            ON DELETE SET NULL;

CREATE INDEX ix_short_url_workplace ON short_url (workplace_id);
