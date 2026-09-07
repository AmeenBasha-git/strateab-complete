CREATE TABLE strategies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES users(id),
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    strategy_type VARCHAR(20) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    active_version_id UUID,
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_strategies_owner ON strategies(owner_id);
CREATE INDEX idx_strategies_visibility ON strategies(visibility);

CREATE TABLE strategy_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    strategy_id UUID NOT NULL REFERENCES strategies(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    source_code TEXT NOT NULL,
    parameters_schema JSONB,
    changelog_note VARCHAR(1000),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_strategy_version UNIQUE (strategy_id, version_number)
);

ALTER TABLE strategies
    ADD CONSTRAINT fk_active_version FOREIGN KEY (active_version_id) REFERENCES strategy_versions(id);
