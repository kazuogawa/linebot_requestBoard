drop table if exists users;
create sequence users_id_seq start with 1;
create table users (
  id int not null default nextval('users_id_seq') primary key,
  name varchar(255) not null,
  lineuser_id varchar(255) not null,
);

insert into users (name, lineuser_id) values ('username1', 'Uad865e80703b0d4f52e706539c61ffd1');
insert into users (name, lineuser_id) values ('username2', 'Uad865e80703b0d4f52e706539c61ffd2');
insert into users (name, lineuser_id) values ('username5', 'Uad865e80703b0d4f52e706539c61ffd5');