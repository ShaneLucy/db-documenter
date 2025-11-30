-- =====================================================
-- RDBMS-Agnostic Multi-Schema Test Database
-- =====================================================
-- This SQL file demonstrates:
-- - Multiple schemas
-- - Wide range of data types
-- - Composite primary keys
-- - Various relationship types (1:1, 1:N, N:M)
-- - Self-referential foreign keys
-- - Cross-table constraints
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
    CONSTRAINT chk_country_code CHECK (country_code ~ '^[A-Z]{2}$')
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
    CONSTRAINT chk_currency_code CHECK (currency_code ~ '^[A-Z]{3}$')
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
-- End of Multi-Schema Test Database
-- =====================================================
