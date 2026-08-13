-- Fixes referential integrity flaws introduced in V1 and V2, and aligns the
-- remaining DATETIME columns with the precision Hibernate 6 expects.
--
-- Context: deleting a user account returned HTTP 500 as soon as the account
-- owned at least one project, because fk_projects_owner had no ON DELETE
-- clause and no JPA cascade compensated for it.

-- ---------------------------------------------------------------------------
-- 1. projects.owner_id -> ON DELETE CASCADE
-- ---------------------------------------------------------------------------
-- Account deletion is documented as irreversible and must carry away the data
-- owned by the account, which is also what the right to erasure requires.
-- A project cannot exist without its owner: no orphan state is meaningful.
ALTER TABLE projects DROP FOREIGN KEY fk_projects_owner;

ALTER TABLE projects
    ADD CONSTRAINT fk_projects_owner
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 2. tasks.assignee_id -> ON DELETE SET NULL
-- ---------------------------------------------------------------------------
-- Deliberately NOT cascade. A task belongs to its project, not to its
-- assignee: deleting an account must never destroy tasks owned by a third
-- party project. The column is already nullable, which is the precondition
-- MySQL requires for SET NULL.
ALTER TABLE tasks DROP FOREIGN KEY fk_tasks_assignee;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_assignee
        FOREIGN KEY (assignee_id) REFERENCES users (id) ON DELETE SET NULL;

-- ---------------------------------------------------------------------------
-- 3. and 4. Enum values enforced at database level
-- ---------------------------------------------------------------------------
-- status and priority are persisted as strings by @Enumerated(EnumType.STRING).
-- Without a CHECK constraint the database accepts any value, and an invalid
-- row would only fail at read time when Hibernate tries to resolve the enum.
-- MySQL validates existing rows when the constraint is added: this was
-- verified beforehand, no row violates either constraint.
ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_status
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'));

ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

-- ---------------------------------------------------------------------------
-- 5. DATETIME -> DATETIME(6)
-- ---------------------------------------------------------------------------
-- Hibernate 6 maps LocalDateTime to fractional-second precision 6 by default,
-- and writes the fractional part. Inserting into a DATETIME(0) column makes
-- MySQL round to the nearest second, which silently shifts stored timestamps
-- by up to half a second. user_providers.linked_at was already declared
-- DATETIME(6) in V2 and serves as the reference.
--
-- The conversion is lossless: MySQL pads the fractional part with zeros.
-- tasks.due_date is deliberately left untouched: it is a DATE mapped to a
-- LocalDate and carries no time component.
--
-- NOT NULL is restated on every column: MySQL's MODIFY COLUMN replaces the
-- whole definition, so omitting it would silently make the column nullable.
ALTER TABLE users
    MODIFY COLUMN created_at DATETIME(6) NOT NULL;

ALTER TABLE projects
    MODIFY COLUMN created_at DATETIME(6) NOT NULL;

ALTER TABLE tasks
    MODIFY COLUMN created_at DATETIME(6) NOT NULL;

ALTER TABLE refresh_tokens
    MODIFY COLUMN expires_at DATETIME(6) NOT NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL;

-- ---------------------------------------------------------------------------
-- 6. Drop users.provider
-- ---------------------------------------------------------------------------
-- Added by V2 as a denormalised discriminant alongside the user_providers
-- table, but never wired: no Java entity maps it, no code reads or writes it,
-- and every row still holds the 'LOCAL' default. Keeping it would guarantee
-- two diverging sources of truth for the same information once third-party
-- sign-in is implemented, with no constraint keeping them in sync.
--
-- The normalised user_providers table is kept: third-party sign-in is a
-- deferred requirement documented in EXPRESSION_DES_BESOINS.md. When it
-- lands, "does this account have a usable local password" is better answered
-- by making users.password nullable than by resurrecting this column.
--
-- Verified before dropping: SELECT provider, COUNT(*) FROM users GROUP BY
-- provider returned LOCAL only. This statement is irreversible under Flyway.
ALTER TABLE users DROP COLUMN provider;