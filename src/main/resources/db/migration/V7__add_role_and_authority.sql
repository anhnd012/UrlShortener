CREATE TABLE role (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    key VARCHAR(30) NOT NULL
);

CREATE TABLE authority (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    key VARCHAR(30) NOT NULL
);

CREATE TABLE user_role (
    id SERIAL PRIMARY KEY,
    role_id UUID NOT NULL,
    user_id UUID NOT NULL,

    FOREIGN KEY (user_id) REFERENCES app_user(id),
    FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE TABLE role_authority (
    id SERIAL PRIMARY KEY,
    role_id UUID NOT NULL,
    authority_id UUID NOT NULL,

    FOREIGN KEY (role_id) REFERENCES role(id),
    FOREIGN KEY (authority_id) REFERENCES authority(id)
);



