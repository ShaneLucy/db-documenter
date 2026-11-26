-- Enable extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -------------------------
-- Users table
-- -------------------------
CREATE TABLE app_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username varchar(50) NOT NULL UNIQUE,
    email varchar(255) NOT NULL UNIQUE,
    display_name varchar(100),
    birth_date date,                                  -- DATE type
    created_at timestamptz NOT NULL DEFAULT now(),    -- TIMESTAMP WITH TIME ZONE
    profile jsonb DEFAULT '{}'::jsonb,                -- JSONB for arbitrary profile data
    is_active boolean NOT NULL DEFAULT true,
    CONSTRAINT chk_username_length CHECK (char_length(username) >= 3)
);

CREATE TABLE role (
    id smallserial PRIMARY KEY,
    code varchar(50) NOT NULL UNIQUE,
    description varchar(255)
);

-- User ↔ Role mapping table with composite PK
CREATE TABLE user_role (
    user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id smallint NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    assigned_by uuid REFERENCES app_user(id),
    -- composite primary key
    PRIMARY KEY (user_id, role_id)
);

-- -------------------------
-- Addresses (FK to app_user)
-- -------------------------
CREATE TABLE address (
    id bigserial PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    line1 varchar(200) NOT NULL,
    line2 varchar(200),
    city varchar(100) NOT NULL,
    state varchar(100),
    postal_code varchar(20),
    country char(2) NOT NULL CHECK (country ~ '^[A-Z]{2}$'), -- ISO country code (2 chars)
    created_at timestamptz NOT NULL DEFAULT now()
);

-- -------------------------
-- Products
-- -------------------------
CREATE TABLE product (
    id bigserial PRIMARY KEY,
    sku varchar(30) NOT NULL UNIQUE,
    name varchar(200) NOT NULL,
    description text,
    price NUMERIC(19,2) NOT NULL CHECK (price >= 0), -- NUMERIC(19,2)
    attributes jsonb DEFAULT '{}'::jsonb,            -- JSONB for flexible product attributes
    created_at timestamp WITHOUT time zone NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'), -- TIMESTAMP WITHOUT TZ
    stock integer NOT NULL DEFAULT 0
);

-- -------------------------
-- Categories
-- -------------------------
CREATE TABLE category (
    id smallserial PRIMARY KEY,
    slug varchar(80) NOT NULL UNIQUE,
    title varchar(120) NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb
);

-- -------------------------
-- Many-to-Many: product <-> category
-- (demonstrates composite PK + FKs)
-- -------------------------
CREATE TABLE product_category (
    product_id bigint NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    category_id smallint NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (product_id, category_id)
);

-- -------------------------
-- Orders
-- -------------------------
CREATE TYPE order_status AS ENUM ('pending','paid','shipped','cancelled','refunded');

CREATE TABLE customer_order (
    id bigserial PRIMARY KEY,
    user_id uuid REFERENCES app_user(id),
    order_number varchar(30) NOT NULL UNIQUE,
    order_date timestamptz NOT NULL DEFAULT now(),
    ship_date timestamp WITHOUT time zone,             -- nullable until shipped
    shipping_address_id bigint REFERENCES address(id),
    status order_status NOT NULL DEFAULT 'pending',
    total NUMERIC(19,2) NOT NULL CHECK (total >= 0),
    metadata jsonb DEFAULT '{}'::jsonb
);

-- -------------------------
-- Order items (many-to-many-like: order <-> product with extra attributes)
-- -------------------------
CREATE TABLE order_item (
    id bigserial PRIMARY KEY,
    order_id bigint NOT NULL REFERENCES customer_order(id) ON DELETE CASCADE,
    product_id bigint NOT NULL REFERENCES product(id),
    product_snapshot jsonb NOT NULL,          -- store product details at time-of-order (json)
    unit_price NUMERIC(19,2) NOT NULL CHECK (unit_price >= 0),
    quantity integer NOT NULL CHECK (quantity > 0),
    line_total NUMERIC(19,2) NOT NULL CHECK (line_total >= 0),
    CONSTRAINT uq_order_product UNIQUE (order_id, product_id)  -- optional: one line per product per order
);

-- -------------------------
-- Payments (FK to order)
-- -------------------------
CREATE TABLE payment (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id bigint NOT NULL REFERENCES customer_order(id) ON DELETE CASCADE,
    paid_at timestamptz NOT NULL DEFAULT now(),
    amount NUMERIC(19,2) NOT NULL CHECK (amount >= 0),
    method varchar(50) NOT NULL,
    provider_transaction_id varchar(255),
    raw_response jsonb,
    CONSTRAINT chk_payment_nonzero CHECK (amount > 0)
);

-- -------------------------
-- Example audit / log table showing additional data types
-- -------------------------
CREATE TABLE audit_log (
    id bigserial PRIMARY KEY,
    entity_type varchar(100) NOT NULL,
    entity_id text NOT NULL,
    action varchar(50) NOT NULL,
    performed_by uuid REFERENCES app_user(id),
    performed_at timestamptz NOT NULL DEFAULT now(),
    details jsonb
);


-- -------------------
-- table without a primary key
-- ------------------
CREATE TABLE tag_log (
    tag_id varchar(50) NOT NULL,
    logged_at timestamptz NOT NULL DEFAULT now()
);

-- -------------------------
-- Helpful indexes for common lookups
-- -------------------------
CREATE INDEX idx_product_price ON product(price);
CREATE INDEX idx_order_user ON customer_order(user_id);
CREATE INDEX idx_address_user ON address(user_id);

-- -------------------------
-- Product status (lookup table for SET DEFAULT referential action testing)
-- -------------------------
CREATE TABLE product_status (
    id smallint PRIMARY KEY,
    code varchar(50) NOT NULL UNIQUE
);

-- Row 0 is the default target for ON DELETE SET DEFAULT / ON UPDATE SET DEFAULT constraints below
INSERT INTO product_status VALUES (0, 'unlisted');

-- -------------------------
-- Product listing
-- Demonstrates RESTRICT, SET NULL, and SET DEFAULT referential actions,
-- and covers ON UPDATE CASCADE, SET NULL, and SET DEFAULT.
-- -------------------------
CREATE TABLE product_listing (
    id             bigserial PRIMARY KEY,
    listed_by      uuid      REFERENCES app_user(id)      ON DELETE RESTRICT     ON UPDATE RESTRICT,
    product_id     bigint    REFERENCES product(id)        ON DELETE SET NULL     ON UPDATE SET NULL,
    status_id      smallint  NOT NULL DEFAULT 0
                             REFERENCES product_status(id) ON DELETE SET DEFAULT  ON UPDATE SET DEFAULT,
    title          varchar(200) NOT NULL,
    listed_at      timestamptz NOT NULL DEFAULT now()
);
