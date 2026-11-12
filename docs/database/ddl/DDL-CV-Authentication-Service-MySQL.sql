-- =====================================================================
-- CV Authentication Service - DDL MySQL 8 (Railway)
-- Convertido a partir de DDL-CV-Authentication-Service.sql (H2)
--
-- Execute conectado ao banco do Railway (por padrão, o banco "railway").
-- Os nomes das tabelas estão em minúsculas porque o Hibernate/Spring Boot
-- (CamelCaseToUnderscoresNamingStrategy) converte CV01_USUARIOS para
-- cv01_usuarios, e o MySQL no Linux diferencia maiúsculas em nomes de tabela.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- cv02_roles
-- ---------------------------------------------------------------------
-- DROP TABLE IF EXISTS cv01_usuarios;
-- DROP TABLE IF EXISTS cv03_instituicoes;
-- DROP TABLE IF EXISTS cv02_roles;

CREATE TABLE IF NOT EXISTS cv02_roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    descricao   VARCHAR(255)          NULL,
    nome        VARCHAR(255) NOT NULL,
    CONSTRAINT pk_cv02_roles PRIMARY KEY (id),
    CONSTRAINT uk_cv02_roles_nome UNIQUE (nome)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- cv03_instituicoes
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- cv01_usuarios
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cv01_usuarios (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    ativo            BIT(1)       NOT NULL,
    atualizado_em    DATETIME(6)           NULL,
    cpf              VARCHAR(255) NOT NULL,
    criado_em        DATETIME(6)  NOT NULL,
    data_nascimento  DATE                  NULL,
    email            VARCHAR(255) NOT NULL,
    nome             VARCHAR(255) NOT NULL,
    senha            VARCHAR(255) NOT NULL,
    bigint_role_id   BIGINT       NOT NULL,
    bigint_instituicao_id BIGINT  NOT NULL,
    CONSTRAINT pk_cv01_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_cv01_usuarios_email UNIQUE (email),
    CONSTRAINT uk_cv01_usuarios_cpf UNIQUE (cpf),
    INDEX idx_cv01_usuarios_role (bigint_role_id),
    INDEX idx_cv01_usuarios_instituicao (bigint_instituicao_id),
    CONSTRAINT fk_cv01_usuarios_role FOREIGN KEY (bigint_role_id)
        REFERENCES cv02_roles (id),
    CONSTRAINT fk_cv01_usuarios_instituicao FOREIGN KEY (bigint_instituicao_id)
        REFERENCES cv03_instituicoes (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Carga inicial de roles (mesmas do DataInitializer / RolesEnum).
-- Opcional: o DataInitializer cria as roles, a instituição padrão
-- "ClassVision" e o usuário admin automaticamente na primeira subida.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO cv02_roles (nome, descricao) VALUES
    ('ADMINISTRADOR', 'Administrador do sistema'),
    ('PROFESSOR',     'Professor da instituição'),
    ('COORDENADOR',   'Coordenador de curso'),
    ('SECRETARIO',    'Secretário da instituição'),
    ('ASSISTENTE',    'Assistente de coordenação');
