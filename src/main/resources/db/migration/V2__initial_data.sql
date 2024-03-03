INSERT INTO privilege(name)
VALUES ('priv-write-item');

INSERT INTO privilege(name)
VALUES ('priv-read-item');

INSERT INTO role(name)
VALUES ('ADMIN');
INSERT INTO role(name)
VALUES ('USER');

INSERT INTO link_role_privilege(role_id, privilege_id)
VALUES ((SELECT id FROM role WHERE name = 'ADMIN'),
        (SELECT id FROM privilege WHERE name = 'priv-write-item'));

INSERT INTO link_role_privilege(role_id, privilege_id)
VALUES ((SELECT id FROM role WHERE name = 'ADMIN'),
        (SELECT id FROM privilege WHERE name = 'priv-write-item'));

INSERT INTO link_role_privilege(role_id, privilege_id)
VALUES ((SELECT id FROM role WHERE name = 'USER'),
        (SELECT id FROM privilege WHERE name = 'priv-read-item'));