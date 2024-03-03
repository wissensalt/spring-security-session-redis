CREATE TABLE account
(
    id       BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    email    VARCHAR(255),
    password VARCHAR(255),
    CONSTRAINT pk_account PRIMARY KEY (id)
);

ALTER TABLE account
    ADD CONSTRAINT uc_account_email UNIQUE (email);

CREATE TABLE role
(
    id   BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name VARCHAR(255),
    CONSTRAINT pk_role PRIMARY KEY (id)
);

CREATE TABLE privilege
(
    id     BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name   VARCHAR(255),
    access VARCHAR(255),
    CONSTRAINT pk_privilege PRIMARY KEY (id)
);

ALTER TABLE privilege
    ADD CONSTRAINT uc_privilege_name UNIQUE (name);

CREATE TABLE link_role_privilege
(
    privilege_id BIGINT NOT NULL,
    role_id      BIGINT NOT NULL
);

ALTER TABLE link_role_privilege
    ADD CONSTRAINT fk_linrolpri_on_privilege FOREIGN KEY (privilege_id) REFERENCES privilege (id);

ALTER TABLE link_role_privilege
    ADD CONSTRAINT fk_linrolpri_on_role FOREIGN KEY (role_id) REFERENCES role (id);

CREATE TABLE link_account_role
(
    account_id BIGINT NOT NULL,
    role_id    BIGINT NOT NULL
);

ALTER TABLE link_account_role
    ADD CONSTRAINT fk_linaccrol_on_account FOREIGN KEY (account_id) REFERENCES account (id);

ALTER TABLE link_account_role
    ADD CONSTRAINT fk_linaccrol_on_role FOREIGN KEY (role_id) REFERENCES role (id);