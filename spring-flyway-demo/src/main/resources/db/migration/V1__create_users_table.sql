create table users
(
    id         bigint    not null,
    email      text      not null,
    password   text      not null,
    name       text      not null,
    role       text      not null,
    created_at timestamp not null,
    updated_at timestamp,
    primary key (id),
    constraint user_email_unique unique (email)
);

insert into users(id, email, password, name, role, created_at) values
(1, 'admin@gmail.com', 'admin', 'Administrator', 'ROLE_ADMIN', CURRENT_TIMESTAMP),
(2, 'siva@gmail.com', 'siva', 'Siva','ROLE_USER', CURRENT_TIMESTAMP);
