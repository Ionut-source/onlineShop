INSERT INTO user (city, number, street, zipcode, firstname, password, surname, username)values ('Bucuresti', 2, 'Lalelelor', '123', 'Admin first', 'password', 'lastName', 'adminUsername'),('Timisoara', 21, 'Aleea Libertatii', '22', 'Client first', 'password2', 'Clientlastname', 'Client username'),('Timisoara', 21, 'Aleea Libertatii', '22', 'Client first', 'password2', 'Clientlastname', 'Expeditor username');
INSERT INTO user_roles values (1, 'ADMIN'),(1, 'EXPEDITOR');
INSERT INTO user_roles values (2, 'CLIENT');
INSERT INTO user_roles values (3, 'EXPEDITOR');
