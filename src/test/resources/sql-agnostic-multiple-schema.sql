-- =====================================================
-- RDBMS-Agnostic Multi-Schema Test Database
-- =====================================================
-- This SQL file demonstrates:
-- - Multiple schemas
-- - Wide range of SQL-standard data types
-- - Composite primary keys
-- - Various relationship types (1:1, 1:N, N:M)
-- - Self-referential foreign keys
-- - Cross-schema constraints
-- - Views (simple and complex)
-- - Sequences
-- - Indexes (single, composite, unique)
-- - Referential actions (CASCADE, SET NULL, SET DEFAULT, RESTRICT)
-- - IDENTITY columns (SQL:2003)
-- - Generated/computed columns (SQL:2003)
-- - Deferrable constraints
-- - Edge cases for comprehensive testing
-- =====================================================

-- =====================================================
-- SCHEMA 1: E-COMMERCE
-- =====================================================
CREATE SCHEMA ecommerce;

-- Users table with various data types
CREATE TABLE ecommerce.users (
    user_id BIGINT NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    date_of_birth DATE,
    phone_number VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    last_login_at TIMESTAMP,
    credit_balance DECIMAL(19,2) DEFAULT 0.00,
    loyalty_points INTEGER DEFAULT 0,
    bio TEXT,
    CONSTRAINT chk_email_format CHECK (email LIKE '%@%'),
    CONSTRAINT chk_credit_balance CHECK (credit_balance >= 0)
);

-- Self-referential foreign key (user referrals)
CREATE TABLE ecommerce.user_referrals (
    referrer_user_id BIGINT NOT NULL,
    referred_user_id BIGINT NOT NULL,
    referral_date TIMESTAMP NOT NULL,
    referral_bonus DECIMAL(10,2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    PRIMARY KEY (referrer_user_id, referred_user_id),
    FOREIGN KEY (referrer_user_id) REFERENCES ecommerce.users(user_id),
    FOREIGN KEY (referred_user_id) REFERENCES ecommerce.users(user_id),
    CONSTRAINT chk_no_self_referral CHECK (referrer_user_id <> referred_user_id),
    CONSTRAINT chk_referral_status CHECK (status IN ('pending', 'completed', 'expired'))
);

-- Addresses (1:N relationship with users)
CREATE TABLE ecommerce.addresses (
    address_id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address_type VARCHAR(20) NOT NULL,
    street_line1 VARCHAR(200) NOT NULL,
    street_line2 VARCHAR(200),
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country_code CHAR(2) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES ecommerce.users(user_id),
    CONSTRAINT chk_address_type CHECK (address_type IN ('shipping', 'billing', 'both')),
    CONSTRAINT chk_country_code CHECK (LENGTH(country_code) = 2 AND country_code = UPPER(country_code))
);

-- Product categories with hierarchical relationship
CREATE TABLE ecommerce.categories (
    category_id INTEGER NOT NULL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    parent_category_id INTEGER,
    description TEXT,
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (parent_category_id) REFERENCES ecommerce.categories(category_id)
);

-- Products
CREATE TABLE ecommerce.products (
    product_id BIGINT NOT NULL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    product_name VARCHAR(200) NOT NULL,
    description TEXT,
    category_id INTEGER,
    unit_price DECIMAL(19,4) NOT NULL,
    cost_price DECIMAL(19,4),
    weight_kg DECIMAL(10,3),
    dimensions_cm VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    is_featured BOOLEAN DEFAULT FALSE,
    stock_quantity INTEGER DEFAULT 0,
    reorder_level INTEGER DEFAULT 10,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES ecommerce.categories(category_id),
    CONSTRAINT chk_unit_price CHECK (unit_price > 0),
    CONSTRAINT chk_stock_quantity CHECK (stock_quantity >= 0)
);

-- Product images with composite PK
CREATE TABLE ecommerce.product_images (
    product_id BIGINT NOT NULL,
    image_sequence INTEGER NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(200),
    is_primary BOOLEAN DEFAULT FALSE,
    uploaded_at TIMESTAMP NOT NULL,
    PRIMARY KEY (product_id, image_sequence),
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id)
);

-- Product reviews with composite PK
CREATE TABLE ecommerce.product_reviews (
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating SMALLINT NOT NULL,
    review_title VARCHAR(200),
    review_text TEXT,
    is_verified_purchase BOOLEAN DEFAULT FALSE,
    helpful_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    PRIMARY KEY (product_id, user_id),
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id),
    FOREIGN KEY (user_id) REFERENCES ecommerce.users(user_id),
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
);

-- Orders
CREATE TABLE ecommerce.orders (
    order_id BIGINT NOT NULL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    order_date TIMESTAMP NOT NULL,
    shipped_date TIMESTAMP,
    delivered_date TIMESTAMP,
    shipping_address_id BIGINT,
    billing_address_id BIGINT,
    subtotal DECIMAL(19,2) NOT NULL,
    tax_amount DECIMAL(19,2) DEFAULT 0.00,
    shipping_cost DECIMAL(19,2) DEFAULT 0.00,
    discount_amount DECIMAL(19,2) DEFAULT 0.00,
    total_amount DECIMAL(19,2) NOT NULL,
    notes TEXT,
    FOREIGN KEY (user_id) REFERENCES ecommerce.users(user_id),
    FOREIGN KEY (shipping_address_id) REFERENCES ecommerce.addresses(address_id),
    FOREIGN KEY (billing_address_id) REFERENCES ecommerce.addresses(address_id),
    CONSTRAINT chk_order_status CHECK (order_status IN ('pending', 'confirmed', 'processing', 'shipped', 'delivered', 'cancelled', 'refunded')),
    CONSTRAINT chk_total_amount CHECK (total_amount >= 0)
);

-- Order items with composite PK
CREATE TABLE ecommerce.order_items (
    order_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    discount_percent DECIMAL(5,2) DEFAULT 0.00,
    line_total DECIMAL(19,2) NOT NULL,
    PRIMARY KEY (order_id, line_number),
    FOREIGN KEY (order_id) REFERENCES ecommerce.orders(order_id),
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id),
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_discount_percent CHECK (discount_percent BETWEEN 0 AND 100)
);

-- Payments
CREATE TABLE ecommerce.payments (
    payment_id BIGINT NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    amount DECIMAL(19,2) NOT NULL,
    currency_code CHAR(3) DEFAULT 'USD',
    transaction_id VARCHAR(100),
    payment_date TIMESTAMP,
    confirmed_date TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (order_id) REFERENCES ecommerce.orders(order_id),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('pending', 'completed', 'failed', 'refunded')),
    CONSTRAINT chk_amount CHECK (amount > 0),
    CONSTRAINT chk_currency_code CHECK (LENGTH(currency_code) = 3 AND currency_code = UPPER(currency_code))
);

-- Coupons
CREATE TABLE ecommerce.coupons (
    coupon_id INTEGER NOT NULL PRIMARY KEY,
    coupon_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    min_purchase_amount DECIMAL(19,2),
    max_discount_amount DECIMAL(19,2),
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    usage_limit INTEGER,
    times_used INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT chk_discount_type CHECK (discount_type IN ('percentage', 'fixed')),
    CONSTRAINT chk_discount_value CHECK (discount_value > 0)
);

-- Coupon usage tracking with composite PK
CREATE TABLE ecommerce.coupon_usage (
    coupon_id INTEGER NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    discount_applied DECIMAL(19,2) NOT NULL,
    used_at TIMESTAMP NOT NULL,
    PRIMARY KEY (coupon_id, order_id),
    FOREIGN KEY (coupon_id) REFERENCES ecommerce.coupons(coupon_id),
    FOREIGN KEY (order_id) REFERENCES ecommerce.orders(order_id),
    FOREIGN KEY (user_id) REFERENCES ecommerce.users(user_id)
);

-- =====================================================
-- SCHEMA 2: INVENTORY
-- =====================================================
CREATE SCHEMA inventory;

-- Warehouses
CREATE TABLE inventory.warehouses (
    warehouse_id INTEGER NOT NULL PRIMARY KEY,
    warehouse_code VARCHAR(20) NOT NULL UNIQUE,
    warehouse_name VARCHAR(100) NOT NULL,
    manager_name VARCHAR(100),
    address_line1 VARCHAR(200) NOT NULL,
    address_line2 VARCHAR(200),
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country_code CHAR(2) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(100),
    capacity_sqm DECIMAL(10,2),
    is_active BOOLEAN DEFAULT TRUE,
    opened_date DATE,
    CONSTRAINT chk_capacity CHECK (capacity_sqm > 0)
);

-- Suppliers
CREATE TABLE inventory.suppliers (
    supplier_id INTEGER NOT NULL PRIMARY KEY,
    supplier_code VARCHAR(20) NOT NULL UNIQUE,
    supplier_name VARCHAR(200) NOT NULL,
    contact_name VARCHAR(100),
    contact_email VARCHAR(100),
    contact_phone VARCHAR(20),
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100),
    country_code CHAR(2),
    payment_terms VARCHAR(50),
    credit_limit DECIMAL(19,2),
    is_active BOOLEAN DEFAULT TRUE,
    rating SMALLINT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
);

-- Product suppliers (many-to-many) with composite PK
CREATE TABLE inventory.product_suppliers (
    product_id BIGINT NOT NULL,
    supplier_id INTEGER NOT NULL,
    supplier_sku VARCHAR(50),
    unit_cost DECIMAL(19,4) NOT NULL,
    lead_time_days INTEGER,
    min_order_quantity INTEGER DEFAULT 1,
    is_preferred BOOLEAN DEFAULT FALSE,
    effective_from DATE NOT NULL,
    effective_until DATE,
    PRIMARY KEY (product_id, supplier_id),
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id),
    FOREIGN KEY (supplier_id) REFERENCES inventory.suppliers(supplier_id),
    CONSTRAINT chk_unit_cost CHECK (unit_cost > 0),
    CONSTRAINT chk_lead_time CHECK (lead_time_days >= 0)
);

-- Stock levels with composite PK (product + warehouse)
CREATE TABLE inventory.stock_levels (
    product_id BIGINT NOT NULL,
    warehouse_id INTEGER NOT NULL,
    quantity_on_hand INTEGER NOT NULL DEFAULT 0,
    quantity_reserved INTEGER NOT NULL DEFAULT 0,
    quantity_available INTEGER NOT NULL DEFAULT 0,
    reorder_point INTEGER DEFAULT 10,
    max_stock_level INTEGER,
    last_stock_check TIMESTAMP,
    last_restocked_at TIMESTAMP,
    PRIMARY KEY (product_id, warehouse_id),
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id),
    FOREIGN KEY (warehouse_id) REFERENCES inventory.warehouses(warehouse_id),
    CONSTRAINT chk_quantity_on_hand CHECK (quantity_on_hand >= 0),
    CONSTRAINT chk_quantity_reserved CHECK (quantity_reserved >= 0)
);

-- Purchase orders
CREATE TABLE inventory.purchase_orders (
    po_id BIGINT NOT NULL PRIMARY KEY,
    po_number VARCHAR(50) NOT NULL UNIQUE,
    supplier_id INTEGER NOT NULL,
    warehouse_id INTEGER NOT NULL,
    order_date DATE NOT NULL,
    expected_delivery_date DATE,
    actual_delivery_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    subtotal DECIMAL(19,2) NOT NULL,
    tax_amount DECIMAL(19,2) DEFAULT 0.00,
    shipping_cost DECIMAL(19,2) DEFAULT 0.00,
    total_amount DECIMAL(19,2) NOT NULL,
    notes TEXT,
    created_by VARCHAR(100),
    FOREIGN KEY (supplier_id) REFERENCES inventory.suppliers(supplier_id),
    FOREIGN KEY (warehouse_id) REFERENCES inventory.warehouses(warehouse_id),
    CONSTRAINT chk_po_status CHECK (status IN ('draft', 'submitted', 'approved', 'received', 'cancelled')),
    CONSTRAINT chk_po_total CHECK (total_amount >= 0)
);

-- Purchase order items with composite PK
CREATE TABLE inventory.purchase_order_items (
    po_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    product_id BIGINT NOT NULL,
    quantity_ordered INTEGER NOT NULL,
    quantity_received INTEGER DEFAULT 0,
    unit_cost DECIMAL(19,4) NOT NULL,
    line_total DECIMAL(19,2) NOT NULL,
    received_date DATE,
    PRIMARY KEY (po_id, line_number),
    FOREIGN KEY (po_id) REFERENCES inventory.purchase_orders(po_id),
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id),
    CONSTRAINT chk_po_quantity_ordered CHECK (quantity_ordered > 0),
    CONSTRAINT chk_po_quantity_received CHECK (quantity_received >= 0)
);

-- Stock movements/transactions
CREATE TABLE inventory.stock_movements (
    movement_id BIGINT NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id INTEGER NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    movement_date TIMESTAMP NOT NULL,
    performed_by VARCHAR(100),
    notes TEXT,
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id),
    FOREIGN KEY (warehouse_id) REFERENCES inventory.warehouses(warehouse_id),
    CONSTRAINT chk_movement_type CHECK (movement_type IN ('receipt', 'shipment', 'adjustment', 'transfer', 'return'))
);

-- Warehouse transfers with composite PK
CREATE TABLE inventory.warehouse_transfers (
    transfer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    from_warehouse_id INTEGER NOT NULL,
    to_warehouse_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    transfer_date TIMESTAMP NOT NULL,
    received_date TIMESTAMP,
    status VARCHAR(20) DEFAULT 'pending',
    initiated_by VARCHAR(100),
    notes TEXT,
    PRIMARY KEY (transfer_id, product_id),
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id),
    FOREIGN KEY (from_warehouse_id) REFERENCES inventory.warehouses(warehouse_id),
    FOREIGN KEY (to_warehouse_id) REFERENCES inventory.warehouses(warehouse_id),
    CONSTRAINT chk_different_warehouses CHECK (from_warehouse_id <> to_warehouse_id),
    CONSTRAINT chk_transfer_status CHECK (status IN ('pending', 'in_transit', 'received', 'cancelled')),
    CONSTRAINT chk_transfer_quantity CHECK (quantity > 0)
);

-- =====================================================
-- SCHEMA 3: ANALYTICS
-- =====================================================
CREATE SCHEMA analytics;

-- Daily sales summary
CREATE TABLE analytics.daily_sales (
    report_date DATE NOT NULL PRIMARY KEY,
    total_orders INTEGER DEFAULT 0,
    total_revenue DECIMAL(19,2) DEFAULT 0.00,
    total_items_sold INTEGER DEFAULT 0,
    unique_customers INTEGER DEFAULT 0,
    average_order_value DECIMAL(19,2) DEFAULT 0.00,
    new_customers INTEGER DEFAULT 0,
    returning_customers INTEGER DEFAULT 0,
    cancelled_orders INTEGER DEFAULT 0,
    refunded_amount DECIMAL(19,2) DEFAULT 0.00,
    generated_at TIMESTAMP NOT NULL
);

-- Product performance metrics with composite PK (date + product)
CREATE TABLE analytics.product_performance (
    report_date DATE NOT NULL,
    product_id BIGINT NOT NULL,
    units_sold INTEGER DEFAULT 0,
    revenue DECIMAL(19,2) DEFAULT 0.00,
    cost DECIMAL(19,2) DEFAULT 0.00,
    profit DECIMAL(19,2) DEFAULT 0.00,
    profit_margin DECIMAL(5,2) DEFAULT 0.00,
    page_views INTEGER DEFAULT 0,
    conversion_rate DECIMAL(5,2) DEFAULT 0.00,
    average_rating DECIMAL(3,2),
    review_count INTEGER DEFAULT 0,
    PRIMARY KEY (report_date, product_id),
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id)
);

-- Customer segments
CREATE TABLE analytics.customer_segments (
    segment_id INTEGER NOT NULL PRIMARY KEY,
    segment_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    min_lifetime_value DECIMAL(19,2),
    max_lifetime_value DECIMAL(19,2),
    min_order_count INTEGER,
    max_order_count INTEGER,
    is_active BOOLEAN DEFAULT TRUE
);

-- Customer segment assignments with composite PK
CREATE TABLE analytics.customer_segment_history (
    user_id BIGINT NOT NULL,
    segment_id INTEGER NOT NULL,
    assigned_date DATE NOT NULL,
    lifetime_value DECIMAL(19,2),
    order_count INTEGER,
    last_order_date DATE,
    PRIMARY KEY (user_id, segment_id, assigned_date),
    FOREIGN KEY (user_id) REFERENCES ecommerce.users(user_id),
    FOREIGN KEY (segment_id) REFERENCES analytics.customer_segments(segment_id)
);

-- Inventory turnover metrics with composite PK
CREATE TABLE analytics.inventory_turnover (
    report_month DATE NOT NULL,
    product_id BIGINT NOT NULL,
    warehouse_id INTEGER NOT NULL,
    beginning_inventory INTEGER,
    ending_inventory INTEGER,
    units_sold INTEGER,
    units_purchased INTEGER,
    average_inventory DECIMAL(10,2),
    turnover_ratio DECIMAL(10,4),
    days_of_inventory DECIMAL(10,2),
    PRIMARY KEY (report_month, product_id, warehouse_id),
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id),
    FOREIGN KEY (warehouse_id) REFERENCES inventory.warehouses(warehouse_id)
);

-- Supplier performance metrics
CREATE TABLE analytics.supplier_performance (
    report_quarter DATE NOT NULL,
    supplier_id INTEGER NOT NULL,
    total_orders INTEGER DEFAULT 0,
    on_time_deliveries INTEGER DEFAULT 0,
    late_deliveries INTEGER DEFAULT 0,
    total_amount DECIMAL(19,2) DEFAULT 0.00,
    average_lead_time_days DECIMAL(10,2),
    quality_score DECIMAL(5,2),
    PRIMARY KEY (report_quarter, supplier_id),
    FOREIGN KEY (supplier_id) REFERENCES inventory.suppliers(supplier_id),
    CONSTRAINT chk_quality_score CHECK (quality_score BETWEEN 0 AND 100)
);

-- Marketing campaign tracking
CREATE TABLE analytics.campaigns (
    campaign_id INTEGER NOT NULL PRIMARY KEY,
    campaign_name VARCHAR(100) NOT NULL,
    campaign_type VARCHAR(50),
    start_date DATE NOT NULL,
    end_date DATE,
    budget DECIMAL(19,2),
    actual_spend DECIMAL(19,2) DEFAULT 0.00,
    target_audience VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE
);

-- Campaign performance with composite PK
CREATE TABLE analytics.campaign_performance (
    campaign_id INTEGER NOT NULL,
    report_date DATE NOT NULL,
    impressions INTEGER DEFAULT 0,
    clicks INTEGER DEFAULT 0,
    conversions INTEGER DEFAULT 0,
    revenue DECIMAL(19,2) DEFAULT 0.00,
    cost DECIMAL(19,2) DEFAULT 0.00,
    click_through_rate DECIMAL(5,4),
    conversion_rate DECIMAL(5,4),
    return_on_ad_spend DECIMAL(10,2),
    PRIMARY KEY (campaign_id, report_date),
    FOREIGN KEY (campaign_id) REFERENCES analytics.campaigns(campaign_id)
);

-- =====================================================
-- COMPREHENSIVE SQL-STANDARD FEATURE ADDITIONS
-- =====================================================
-- The following sections add comprehensive SQL-standard
-- features for thorough database documentation testing
-- =====================================================

-- =====================================================
-- TABLES WITH REFERENTIAL ACTIONS
-- =====================================================

-- User preferences with ON DELETE CASCADE and ON UPDATE CASCADE
CREATE TABLE ecommerce.user_preferences (
    preference_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES ecommerce.users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    UNIQUE (user_id, preference_key)
);

-- Product promotions with ON DELETE SET NULL
CREATE TABLE ecommerce.product_promotions (
    promotion_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT,
    promotion_name VARCHAR(100) NOT NULL,
    discount_percent DECIMAL(5, 2),
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id)
        ON DELETE SET NULL
        ON UPDATE NO ACTION
);

-- Protected categories with ON DELETE RESTRICT
CREATE TABLE ecommerce.protected_categories (
    category_id INTEGER NOT NULL PRIMARY KEY,
    protection_reason VARCHAR(200) NOT NULL,
    protected_since DATE NOT NULL DEFAULT CURRENT_DATE,
    protected_by VARCHAR(100) DEFAULT CURRENT_USER,
    FOREIGN KEY (category_id) REFERENCES ecommerce.categories(category_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

-- =====================================================
-- DATA TYPE SHOWCASE TABLE
-- =====================================================
-- Demonstrates comprehensive SQL-standard data types

CREATE TABLE ecommerce.data_type_showcase (
    -- Identity column (SQL:2003 standard auto-increment)
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- Exact numeric types
    small_int_col SMALLINT,
    integer_col INTEGER,
    bigint_col BIGINT,
    numeric_col NUMERIC(20, 6),
    decimal_col DECIMAL(15, 4),

    -- Approximate numeric types
    real_col REAL,
    double_col DOUBLE PRECISION,
    float_col FLOAT(24),

    -- Character string types
    char_fixed CHAR(10),
    varchar_variable VARCHAR(255),
    text_unlimited TEXT,

    -- Date and time types
    date_only DATE,
    time_only TIME,
    time_with_tz TIME WITH TIME ZONE,
    timestamp_col TIMESTAMP,
    timestamp_with_tz TIMESTAMP WITH TIME ZONE,

    -- Boolean type
    boolean_col BOOLEAN,


    -- Columns demonstrating SQL-standard default functions
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_date DATE DEFAULT CURRENT_DATE,
    created_user VARCHAR(100) DEFAULT CURRENT_USER,

    -- Generated/computed column (SQL:2003)
    computed_full_description VARCHAR(300) GENERATED ALWAYS AS (
        COALESCE(CAST(id AS VARCHAR(10)), 'N/A') || ': ' || COALESCE(varchar_variable, 'No description')
    ) STORED,

    -- Constraints demonstrating various validations
    CONSTRAINT chk_small_int_range CHECK (small_int_col IS NULL OR small_int_col BETWEEN -1000 AND 1000),
    CONSTRAINT chk_real_positive CHECK (real_col IS NULL OR real_col >= 0),
    CONSTRAINT chk_date_not_future CHECK (date_only IS NULL OR date_only <= CURRENT_DATE)
);

-- =====================================================
-- IDENTITY COLUMN VARIATIONS
-- =====================================================

-- Basic IDENTITY column
CREATE TABLE ecommerce.config_settings (
    setting_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(500),
    setting_type VARCHAR(20) DEFAULT 'string',
    is_encrypted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_setting_type CHECK (setting_type IN ('string', 'integer', 'boolean', 'json'))
);

-- IDENTITY with custom start and increment
CREATE TABLE inventory.batch_tracking (
    batch_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000 INCREMENT BY 5) PRIMARY KEY,
    batch_number VARCHAR(50) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    warehouse_id INTEGER NOT NULL,
    manufactured_date DATE NOT NULL,
    expiry_date DATE,
    quantity INTEGER NOT NULL,
    notes TEXT,
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (warehouse_id) REFERENCES inventory.warehouses(warehouse_id) ON DELETE RESTRICT,
    CONSTRAINT chk_batch_quantity CHECK (quantity > 0),
    CONSTRAINT chk_batch_dates CHECK (expiry_date IS NULL OR expiry_date > manufactured_date)
);

-- =====================================================
-- GENERATED/COMPUTED COLUMNS
-- =====================================================

-- Invoice table with computed columns
CREATE TABLE ecommerce.invoices (
    invoice_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    invoice_date DATE NOT NULL DEFAULT CURRENT_DATE,
    subtotal DECIMAL(19,2) NOT NULL,
    tax_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0825,

    -- Computed columns
    tax_amount DECIMAL(19,2) GENERATED ALWAYS AS (subtotal * tax_rate) STORED,
    total_amount DECIMAL(19,2) GENERATED ALWAYS AS (subtotal + (subtotal * tax_rate)) STORED,

    due_date DATE,
    paid_date DATE,
    status VARCHAR(20) DEFAULT 'pending',

    FOREIGN KEY (order_id) REFERENCES ecommerce.orders(order_id) ON DELETE CASCADE,
    CONSTRAINT chk_invoice_status CHECK (status IN ('pending', 'paid', 'overdue', 'cancelled')),
    CONSTRAINT chk_subtotal_positive CHECK (subtotal > 0)
);

-- Person table with full name computed
CREATE TABLE ecommerce.people (
    person_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,

    -- Computed full name
    full_name VARCHAR(302) GENERATED ALWAYS AS (
        first_name ||
        CASE WHEN middle_name IS NOT NULL THEN ' ' || middle_name ELSE '' END ||
        ' ' || last_name
    ) STORED,

    birth_date DATE,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- INTERVAL DATA TYPE DEMONSTRATION
-- =====================================================

-- Subscription plans using INTERVAL
CREATE TABLE ecommerce.subscription_plans (
    plan_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_name VARCHAR(100) NOT NULL UNIQUE,
    plan_code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    billing_period INTERVAL NOT NULL,
    trial_period INTERVAL,
    grace_period INTERVAL DEFAULT INTERVAL '7' DAY,
    monthly_price DECIMAL(10, 2) NOT NULL,
    annual_price DECIMAL(10, 2),
    setup_fee DECIMAL(10, 2) DEFAULT 0.00,
    max_users INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_monthly_price CHECK (monthly_price >= 0),
    CONSTRAINT chk_max_users CHECK (max_users > 0)
);

-- User subscriptions
CREATE TABLE ecommerce.user_subscriptions (
    subscription_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id INTEGER NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trial_ends_at TIMESTAMP,
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    FOREIGN KEY (user_id) REFERENCES ecommerce.users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES ecommerce.subscription_plans(plan_id) ON DELETE RESTRICT,
    CONSTRAINT chk_subscription_status CHECK (status IN ('active', 'trial', 'cancelled', 'expired', 'paused'))
);

-- =====================================================
-- DEFERRABLE CONSTRAINTS (CIRCULAR DEPENDENCIES)
-- =====================================================

-- Departments table (part of circular FK relationship)
CREATE TABLE ecommerce.departments (
    department_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    department_code VARCHAR(20) NOT NULL UNIQUE,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    department_head_id BIGINT,  -- FK added after employees table
    parent_department_id INTEGER,
    budget DECIMAL(19, 2),
    location VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_department_id) REFERENCES ecommerce.departments(department_id)
        DEFERRABLE INITIALLY DEFERRED
);

-- Employees table (completes circular FK)
CREATE TABLE ecommerce.employees (
    employee_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_number VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    department_id INTEGER,
    manager_id BIGINT,  -- Self-referential FK
    job_title VARCHAR(100),
    hire_date DATE NOT NULL,
    termination_date DATE,
    salary DECIMAL(19, 2),
    commission_rate DECIMAL(5, 4) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (department_id) REFERENCES ecommerce.departments(department_id)
        DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY (manager_id) REFERENCES ecommerce.employees(employee_id)
        ON DELETE SET NULL
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT chk_hire_before_termination CHECK (termination_date IS NULL OR termination_date >= hire_date),
    CONSTRAINT chk_salary_positive CHECK (salary IS NULL OR salary > 0),
    CONSTRAINT chk_commission_rate CHECK (commission_rate >= 0 AND commission_rate <= 1)
);

-- Complete the circular FK relationship
ALTER TABLE ecommerce.departments
    ADD CONSTRAINT fk_department_head
    FOREIGN KEY (department_head_id) REFERENCES ecommerce.employees(employee_id)
    ON DELETE SET NULL
    DEFERRABLE INITIALLY DEFERRED;

-- =====================================================
-- EDGE CASE TABLES
-- =====================================================

-- Table with SQL reserved keyword column names (tests identifier quoting)
CREATE TABLE analytics."order" (
    "select" INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    "from" VARCHAR(100),
    "where" TEXT,
    "group" INTEGER,
    "table" VARCHAR(50),
    "index" INTEGER,
    "constraint" BOOLEAN DEFAULT FALSE,
    "primary" DATE,
    "foreign" TIMESTAMP,
    "null" VARCHAR(10),
    "default" DECIMAL(10,2)
);

-- Table with very long identifiers (testing 63-character limit)
CREATE TABLE analytics.very_long_table_name_testing_identifier_length_limits_max (
    very_long_column_name_testing_identifier_limits_primary INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    another_extremely_long_column_name_for_comprehensive_tests VARCHAR(100),
    yet_another_very_long_column_name_to_test_edge_cases_fully TEXT,
    short_col INTEGER
);

-- Table with ALL nullable columns (no NOT NULL except implied by PK)
CREATE TABLE analytics.all_nullable_columns (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100),
    description TEXT,
    amount DECIMAL(10, 2),
    quantity INTEGER,
    is_active BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Table with ALL NOT NULL columns
CREATE TABLE analytics.all_required_columns (
    id INTEGER NOT NULL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Table with multiple UNIQUE constraints
CREATE TABLE ecommerce.multi_unique_constraints (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    natural_key_1 VARCHAR(50) NOT NULL,
    natural_key_2 VARCHAR(50) NOT NULL,
    alternate_key VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,  -- Nullable unique
    employee_number VARCHAR(20) NOT NULL UNIQUE,
    CONSTRAINT uq_natural_key UNIQUE (natural_key_1, natural_key_2),
    CONSTRAINT uq_email_phone UNIQUE (email, phone)
);

-- Table WITHOUT a primary key (tests PK detection/handling)
CREATE TABLE analytics.audit_log_no_primary_key (
    log_timestamp TIMESTAMP NOT NULL,
    table_schema VARCHAR(100) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    row_id VARCHAR(100),
    old_values TEXT,
    new_values TEXT,
    performed_by VARCHAR(100),
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(100)
);

-- Empty reference table (for testing empty table handling)
CREATE TABLE ecommerce.future_payment_methods (
    method_id INTEGER NOT NULL PRIMARY KEY,
    method_code VARCHAR(20) NOT NULL UNIQUE,
    method_name VARCHAR(100) NOT NULL,
    processor VARCHAR(100),
    is_active BOOLEAN DEFAULT FALSE,
    available_from DATE,
    configuration TEXT,
    CONSTRAINT chk_method_code_format CHECK (LENGTH(method_code) >= 3)
);

-- Table with all default values
CREATE TABLE ecommerce.all_defaults (
    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL DEFAULT 'Unknown',
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    priority INTEGER NOT NULL DEFAULT 0,
    count INTEGER NOT NULL DEFAULT 0,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT CURRENT_USER,
    notes TEXT DEFAULT NULL,
    CONSTRAINT chk_priority_range CHECK (priority BETWEEN 0 AND 100)
);

-- Table demonstrating multiple check constraints
CREATE TABLE ecommerce.constraint_showcase (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    percentage DECIMAL(5,2),
    rating INTEGER,
    status VARCHAR(20),
    start_date DATE,
    end_date DATE,
    email VARCHAR(255),
    price DECIMAL(19,2),
    quantity INTEGER,
    discount_code VARCHAR(20),
    CONSTRAINT chk_percentage_range CHECK (percentage IS NULL OR (percentage >= 0 AND percentage <= 100)),
    CONSTRAINT chk_rating_values CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    CONSTRAINT chk_status_enum CHECK (status IN ('draft', 'pending', 'active', 'archived', 'deleted')),
    CONSTRAINT chk_date_order CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_email_format CHECK (email IS NULL OR email LIKE '%@%.%'),
    CONSTRAINT chk_price_positive CHECK (price IS NULL OR price > 0),
    CONSTRAINT chk_quantity_nonnegative CHECK (quantity IS NULL OR quantity >= 0),
    CONSTRAINT chk_discount_uppercase CHECK (discount_code IS NULL OR discount_code = UPPER(discount_code))
);

-- =====================================================
-- MULTI-COLUMN FOREIGN KEY DEMONSTRATION
-- =====================================================

-- Warehouse locations with composite primary key
CREATE TABLE inventory.warehouse_locations (
    warehouse_code VARCHAR(10) NOT NULL,
    location_code VARCHAR(10) NOT NULL,
    aisle VARCHAR(10),
    shelf VARCHAR(10),
    bin VARCHAR(10),
    description VARCHAR(200),
    capacity_cubic_meters DECIMAL(10,2),
    is_active BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (warehouse_code, location_code)
);

-- Inventory bins referencing composite PK
CREATE TABLE inventory.inventory_bins (
    bin_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_code VARCHAR(10) NOT NULL,
    location_code VARCHAR(10) NOT NULL,
    bin_number VARCHAR(20) NOT NULL,
    product_id BIGINT,
    max_weight_kg DECIMAL(10,2),
    current_weight_kg DECIMAL(10,2) DEFAULT 0.00,
    is_refrigerated BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (warehouse_code, location_code)
        REFERENCES inventory.warehouse_locations(warehouse_code, location_code)
        ON DELETE RESTRICT,
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id)
        ON DELETE SET NULL,
    CONSTRAINT uq_bin_number UNIQUE (warehouse_code, location_code, bin_number),
    CONSTRAINT chk_weight_capacity CHECK (current_weight_kg IS NULL OR max_weight_kg IS NULL OR current_weight_kg <= max_weight_kg)
);

-- =====================================================
-- CROSS-SCHEMA COMPREHENSIVE METRICS TABLE
-- =====================================================

-- Analytics table referencing all three schemas
CREATE TABLE analytics.comprehensive_business_metrics (
    metric_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    metric_date DATE NOT NULL,
    user_id BIGINT,
    product_id BIGINT,
    warehouse_id INTEGER,
    supplier_id INTEGER,
    department_id INTEGER,
    order_count INTEGER DEFAULT 0,
    revenue DECIMAL(19, 2) DEFAULT 0.00,
    cost DECIMAL(19, 2) DEFAULT 0.00,
    profit DECIMAL(19, 2) GENERATED ALWAYS AS (revenue - cost) STORED,
    inventory_level INTEGER,
    supplier_lead_time_days INTEGER,
    employee_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES ecommerce.users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (product_id) REFERENCES ecommerce.products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (warehouse_id) REFERENCES inventory.warehouses(warehouse_id) ON DELETE SET NULL,
    FOREIGN KEY (supplier_id) REFERENCES inventory.suppliers(supplier_id) ON DELETE SET NULL,
    FOREIGN KEY (department_id) REFERENCES ecommerce.departments(department_id) ON DELETE SET NULL,
    CONSTRAINT uq_metric_date_product UNIQUE (metric_date, product_id),
    CONSTRAINT chk_order_count CHECK (order_count >= 0),
    CONSTRAINT chk_revenue CHECK (revenue >= 0)
);

-- =====================================================
-- SEQUENCES (SQL:2003 STANDARD)
-- =====================================================

-- Sequence for user IDs
CREATE SEQUENCE ecommerce.user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    NO CYCLE;

-- Sequence for order numbers
CREATE SEQUENCE ecommerce.order_number_seq
    START WITH 100000
    INCREMENT BY 1
    MINVALUE 100000
    MAXVALUE 999999999
    NO CYCLE;

-- Sequence for invoice numbers
CREATE SEQUENCE ecommerce.invoice_number_seq
    START WITH 10000
    INCREMENT BY 1
    NO CYCLE;

-- Sequence for product IDs
CREATE SEQUENCE ecommerce.product_id_seq
    START WITH 1000
    INCREMENT BY 1
    NO CYCLE;

-- Sequence with larger increment
CREATE SEQUENCE inventory.movement_id_seq
    START WITH 1
    INCREMENT BY 10
    NO CYCLE;

-- Sequence for analytics report IDs
CREATE SEQUENCE analytics.report_id_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- Sequence with specific range
CREATE SEQUENCE inventory.batch_code_seq
    START WITH 1000
    INCREMENT BY 100
    MINVALUE 1000
    MAXVALUE 999999
    NO CYCLE;

-- =====================================================
-- VIEWS (SQL:1992 STANDARD)
-- =====================================================

-- Simple view: Active users
CREATE VIEW ecommerce.active_users AS
SELECT
    user_id,
    username,
    email,
    first_name,
    last_name,
    created_at,
    last_login_at,
    loyalty_points
FROM ecommerce.users
WHERE is_active = TRUE AND is_verified = TRUE;

-- View with aggregation: Order summary
CREATE VIEW ecommerce.order_summary AS
SELECT
    o.order_id,
    o.order_number,
    o.user_id,
    u.username,
    u.email AS customer_email,
    o.order_status,
    o.order_date,
    o.total_amount,
    COUNT(oi.line_number) AS item_count,
    SUM(oi.quantity) AS total_quantity
FROM ecommerce.orders o
JOIN ecommerce.users u ON o.user_id = u.user_id
LEFT JOIN ecommerce.order_items oi ON o.order_id = oi.order_id
GROUP BY o.order_id, o.order_number, o.user_id, u.username,
         u.email, o.order_status, o.order_date, o.total_amount;

-- Cross-schema view: Product inventory summary
CREATE VIEW inventory.product_inventory_summary AS
SELECT
    p.product_id,
    p.sku,
    p.product_name,
    p.unit_price,
    c.category_name,
    w.warehouse_code,
    w.warehouse_name,
    sl.quantity_on_hand,
    sl.quantity_reserved,
    sl.quantity_available,
    sl.reorder_point,
    CASE
        WHEN sl.quantity_available <= sl.reorder_point THEN 'LOW'
        WHEN sl.quantity_available <= (sl.reorder_point * 2) THEN 'MEDIUM'
        ELSE 'SUFFICIENT'
    END AS stock_status
FROM ecommerce.products p
LEFT JOIN ecommerce.categories c ON p.category_id = c.category_id
LEFT JOIN inventory.stock_levels sl ON p.product_id = sl.product_id
LEFT JOIN inventory.warehouses w ON sl.warehouse_id = w.warehouse_id
WHERE p.is_active = TRUE;

-- Complex view with multiple joins and subquery
CREATE VIEW analytics.customer_lifetime_value AS
SELECT
    u.user_id,
    u.username,
    u.email,
    u.created_at AS customer_since,
    COUNT(DISTINCT o.order_id) AS total_orders,
    COALESCE(SUM(o.total_amount), 0) AS lifetime_revenue,
    COALESCE(AVG(o.total_amount), 0) AS average_order_value,
    MAX(o.order_date) AS last_order_date,
    CASE
        WHEN COUNT(o.order_id) = 0 THEN 'INACTIVE'
        WHEN COUNT(o.order_id) = 1 THEN 'ONE_TIME'
        WHEN COUNT(o.order_id) BETWEEN 2 AND 5 THEN 'OCCASIONAL'
        WHEN COUNT(o.order_id) BETWEEN 6 AND 10 THEN 'REGULAR'
        ELSE 'VIP'
    END AS customer_segment
FROM ecommerce.users u
LEFT JOIN ecommerce.orders o ON u.user_id = o.user_id
GROUP BY u.user_id, u.username, u.email, u.created_at;

-- View with cross-schema joins for comprehensive reporting
CREATE VIEW analytics.order_fulfillment_details AS
SELECT
    o.order_id,
    o.order_number,
    o.order_date,
    u.username,
    p.product_name,
    oi.quantity AS ordered_quantity,
    sl.quantity_available,
    w.warehouse_name,
    w.city AS warehouse_city,
    CASE
        WHEN sl.quantity_available >= oi.quantity THEN 'IN_STOCK'
        WHEN sl.quantity_available > 0 THEN 'PARTIAL'
        ELSE 'OUT_OF_STOCK'
    END AS availability_status
FROM ecommerce.orders o
JOIN ecommerce.users u ON o.user_id = u.user_id
JOIN ecommerce.order_items oi ON o.order_id = oi.order_id
JOIN ecommerce.products p ON oi.product_id = p.product_id
LEFT JOIN inventory.stock_levels sl ON p.product_id = sl.product_id
LEFT JOIN inventory.warehouses w ON sl.warehouse_id = w.warehouse_id;

-- View demonstrating column aliases
CREATE VIEW ecommerce.product_catalog (
    product_identifier,
    stock_keeping_unit,
    product_title,
    category,
    retail_price,
    wholesale_price,
    profit_margin_percent,
    in_stock,
    featured_item
) AS
SELECT
    p.product_id,
    p.sku,
    p.product_name,
    c.category_name,
    p.unit_price,
    p.cost_price,
    CASE
        WHEN p.cost_price > 0 THEN ((p.unit_price - p.cost_price) / p.cost_price * 100)
        ELSE NULL
    END,
    CASE WHEN p.stock_quantity > 0 THEN TRUE ELSE FALSE END,
    p.is_featured
FROM ecommerce.products p
LEFT JOIN ecommerce.categories c ON p.category_id = c.category_id;

-- View for supplier performance metrics
CREATE VIEW analytics.supplier_reliability_metrics AS
SELECT
    s.supplier_id,
    s.supplier_name,
    s.supplier_code,
    COUNT(DISTINCT po.po_id) AS total_purchase_orders,
    COUNT(DISTINCT CASE WHEN po.status = 'received' THEN po.po_id END) AS completed_orders,
    AVG(CASE
        WHEN po.actual_delivery_date IS NOT NULL AND po.expected_delivery_date IS NOT NULL
        THEN EXTRACT(DAY FROM (po.actual_delivery_date - po.expected_delivery_date))
        ELSE NULL
    END) AS avg_delivery_delay_days,
    SUM(po.total_amount) AS total_purchase_value,
    s.rating AS supplier_rating
FROM inventory.suppliers s
LEFT JOIN inventory.purchase_orders po ON s.supplier_id = po.supplier_id
GROUP BY s.supplier_id, s.supplier_name, s.supplier_code, s.rating;

-- =====================================================
-- INDEXES (SQL:2003 STANDARD)
-- =====================================================

-- Single column indexes
CREATE INDEX idx_users_email ON ecommerce.users(email);
CREATE INDEX idx_users_username ON ecommerce.users(username);
CREATE INDEX idx_products_sku ON ecommerce.products(sku);
CREATE INDEX idx_products_name ON ecommerce.products(product_name);
CREATE INDEX idx_orders_order_number ON ecommerce.orders(order_number);
CREATE INDEX idx_orders_order_date ON ecommerce.orders(order_date);

-- Composite indexes (multi-column)
CREATE INDEX idx_orders_user_date ON ecommerce.orders(user_id, order_date);
CREATE INDEX idx_orders_status_date ON ecommerce.orders(order_status, order_date);
CREATE INDEX idx_order_items_product ON ecommerce.order_items(product_id, order_id);
CREATE INDEX idx_stock_product_warehouse ON inventory.stock_levels(product_id, warehouse_id);
CREATE INDEX idx_product_suppliers_supplier ON inventory.product_suppliers(supplier_id, product_id);

-- Indexes on foreign keys (performance optimization)
CREATE INDEX idx_addresses_user ON ecommerce.addresses(user_id);
CREATE INDEX idx_products_category ON ecommerce.products(category_id);
CREATE INDEX idx_order_items_order ON ecommerce.order_items(order_id);
CREATE INDEX idx_payments_order ON ecommerce.payments(order_id);
CREATE INDEX idx_product_reviews_product ON ecommerce.product_reviews(product_id);
CREATE INDEX idx_product_reviews_user ON ecommerce.product_reviews(user_id);

-- Unique indexes (in addition to unique constraints)
CREATE UNIQUE INDEX idx_unique_employee_number ON ecommerce.employees(employee_number);
CREATE UNIQUE INDEX idx_unique_department_code ON ecommerce.departments(department_code);
CREATE UNIQUE INDEX idx_unique_coupon_code ON ecommerce.coupons(coupon_code);

-- Descending indexes (for ORDER BY DESC queries)
CREATE INDEX idx_orders_date_desc ON ecommerce.orders(order_date DESC);
CREATE INDEX idx_products_price_desc ON ecommerce.products(unit_price DESC);
CREATE INDEX idx_users_created_desc ON ecommerce.users(created_at DESC);

-- Partial/filtered indexes (where supported - tests conditional indexes)
CREATE INDEX idx_active_products ON ecommerce.products(product_id) WHERE is_active = TRUE;
CREATE INDEX idx_verified_users ON ecommerce.users(user_id) WHERE is_verified = TRUE;
CREATE INDEX idx_pending_orders ON ecommerce.orders(order_id) WHERE order_status = 'pending';

-- Indexes for analytics queries
CREATE INDEX idx_product_performance_date ON analytics.product_performance(report_date);
CREATE INDEX idx_product_performance_product ON analytics.product_performance(product_id);
CREATE INDEX idx_daily_sales_date ON analytics.daily_sales(report_date DESC);
CREATE INDEX idx_inventory_turnover_month ON analytics.inventory_turnover(report_month DESC);

-- Cross-schema index optimization
CREATE INDEX idx_purchase_order_items_product ON inventory.purchase_order_items(product_id);
CREATE INDEX idx_stock_movements_product ON inventory.stock_movements(product_id);
CREATE INDEX idx_warehouse_transfers_product ON inventory.warehouse_transfers(product_id);

-- Composite index with mixed sort order
CREATE INDEX idx_employees_dept_name ON ecommerce.employees(department_id ASC, last_name ASC, first_name ASC);

-- =====================================================
-- End of Multi-Schema Test Database
-- =====================================================
