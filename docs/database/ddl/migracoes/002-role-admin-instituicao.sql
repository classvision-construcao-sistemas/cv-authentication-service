-- =====================================================================
-- Migração 002 - Role ADMIN_INSTITUICAO (MySQL 8 / Railway)
-- Cria a role de administrador da instituição.
-- Opcional: a aplicação também cria as roles que faltarem ao iniciar.
-- =====================================================================

INSERT INTO cv02_roles (nome, descricao)
SELECT 'ADMIN_INSTITUICAO', 'Administrador da instituição'
WHERE NOT EXISTS (SELECT 1 FROM cv02_roles WHERE nome = 'ADMIN_INSTITUICAO');
