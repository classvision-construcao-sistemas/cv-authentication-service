-- =====================================================================
-- Migração 001 - Instituições (MySQL 8 / Railway)
-- Use SOMENTE se você já executou a versão anterior do
-- DDL-CV-Authentication-Service.sql (sem instituições).
-- Para um banco novo, execute apenas o DDL completo atualizado.
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS cv03_instituicoes (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nome           VARCHAR(255) NOT NULL,
    cnpj           VARCHAR(14)  NOT NULL,
    telefone       VARCHAR(20)  NOT NULL,
    situacao       VARCHAR(20)  NOT NULL,
    criado_em      DATETIME(6)  NOT NULL,
    atualizado_em  DATETIME(6)  NULL,
    CONSTRAINT pk_cv03_instituicoes PRIMARY KEY (id),
    CONSTRAINT uk_cv03_instituicoes_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_cv03_instituicoes_situacao CHECK (situacao IN ('ATIVA', 'INATIVA'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO cv03_instituicoes (nome, cnpj, telefone, situacao, criado_em, atualizado_em)
SELECT 'ClassVision', '00000000000000', '0000000000', 'ATIVA', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM cv03_instituicoes WHERE cnpj = '00000000000000');

ALTER TABLE cv01_usuarios ADD COLUMN bigint_instituicao_id BIGINT NULL;

UPDATE cv01_usuarios
   SET bigint_instituicao_id = (SELECT id FROM cv03_instituicoes WHERE cnpj = '00000000000000')
 WHERE bigint_instituicao_id IS NULL;

ALTER TABLE cv01_usuarios
    MODIFY bigint_instituicao_id BIGINT NOT NULL,
    ADD INDEX idx_cv01_usuarios_instituicao (bigint_instituicao_id),
    ADD CONSTRAINT fk_cv01_usuarios_instituicao FOREIGN KEY (bigint_instituicao_id)
        REFERENCES cv03_instituicoes (id);
