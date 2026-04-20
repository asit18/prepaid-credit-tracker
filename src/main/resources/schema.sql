CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    role VARCHAR(32) NOT NULL DEFAULT 'EMPLOYEE',
    password_hash VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret TEXT,
    mfa_setup_pending BOOLEAN NOT NULL DEFAULT FALSE,
    backup_codes TEXT,
    last_used_totp_step BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS role VARCHAR(32) NOT NULL DEFAULT 'EMPLOYEE';
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS mfa_secret TEXT;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS mfa_setup_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS backup_codes TEXT;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS last_used_totp_step BIGINT;
CREATE INDEX IF NOT EXISTS idx_admin_users_mfa_enabled ON admin_users (mfa_enabled);
UPDATE admin_users SET role = 'OWNER' WHERE role IS NULL OR email = 'admin@example.com';

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    target_email VARCHAR(255),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_events_event_type ON audit_events (event_type);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_admin FOREIGN KEY (admin_user_id) REFERENCES admin_users(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_password_reset_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_password_reset_admin ON password_reset_tokens (admin_user_id);

CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(120) PRIMARY KEY,
    setting_value VARCHAR(500) NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(80) NOT NULL,
    notes TEXT,
    email VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customer_name ON customers (name);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE customers SET phone = '' WHERE phone IS NULL;
ALTER TABLE customers ALTER COLUMN phone SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_name_phone ON customers (LOWER(name), phone);

CREATE TABLE IF NOT EXISTS customer_contacts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(80),
    relationship TEXT,
    CONSTRAINT fk_customer_contacts_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    unit_label VARCHAR(80) NOT NULL DEFAULT 'units',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_prices (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    price_per_unit DECIMAL(10,2) NOT NULL,
    effective_from DATE NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    CONSTRAINT fk_product_prices_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_price_effective UNIQUE (product_id, effective_from)
);

CREATE INDEX IF NOT EXISTS idx_product_prices_current ON product_prices (product_id, effective_from);

CREATE TABLE IF NOT EXISTS credit_transactions (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    units DECIMAL(10,4) NOT NULL,
    amount_paid DECIMAL(10,2),
    price_per_unit_at_time DECIMAL(10,2),
    notes TEXT,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    CONSTRAINT fk_credit_transactions_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_credit_transactions_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_credit_transaction_type CHECK (transaction_type IN ('PURCHASE','CONSUMPTION','ADJUSTMENT','REFUND'))
);

CREATE INDEX IF NOT EXISTS idx_credit_customer_product_date ON credit_transactions (customer_id, product_id, transaction_date);
CREATE INDEX IF NOT EXISTS idx_credit_transaction_date ON credit_transactions (transaction_date);
CREATE INDEX IF NOT EXISTS idx_credit_transaction_type ON credit_transactions (transaction_type);
CREATE UNIQUE INDEX IF NOT EXISTS uk_products_name ON products (name);

ALTER TABLE product_prices ALTER COLUMN price_per_unit TYPE DECIMAL(10,2);
ALTER TABLE credit_transactions ALTER COLUMN price_per_unit_at_time TYPE DECIMAL(10,2);

INSERT INTO admin_users (email, display_name, is_active)
VALUES ('admin@example.com', 'Seed Admin', TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO app_settings (setting_key, setting_value)
VALUES ('business_name', 'Prepaid Credit Tracker')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO products (id, name, description, unit_label, is_active)
VALUES (1, 'Water Delivery', 'Prepaid water delivery credits', 'units', TRUE),
       (2, 'Equipment Service', 'Prepaid service time', 'hours', TRUE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('products', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM products), 1));

INSERT INTO product_prices (product_id, price_per_unit, effective_from, created_by)
VALUES (1, 5.0000, CURRENT_DATE, 'system'),
       (2, 45.0000, CURRENT_DATE, 'system')
ON CONFLICT (product_id, effective_from) DO NOTHING;
