create table post (
    id      serial primary key,
    name    varchar(512),
	link    text unique,
    text    text,
    created timestamp
);