INSERT INTO usuario (nome, email, senha, cargo_id, status)
    SELECT 'admin', 'admin@email.com', '$2a$10$f7AuiX802ORaPWJHITPkj.PR36qPaKXmhxs9XJb/idQ4c1VHuCXD6', id, 'PENDENTE'
    FROM cargo
    WHERE nome = 'ADMINISTRADOR';

INSERT INTO usuario (nome, email, senha, cargo_id, status)
    SELECT 'operador', 'operador@email.com', '$2a$10$I6BHeKOy5omMYPUlF5HCweWV5GTk.IhVyzxidI3YSTf0.U5Zs.YXa', id, 'PENDENTE'
    FROM cargo
    WHERE nome = 'OPERADOR';