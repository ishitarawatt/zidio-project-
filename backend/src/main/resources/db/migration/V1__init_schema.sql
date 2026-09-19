CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sites (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('DISPATCHER','TECHNICIAN','MANAGER','CUSTOMER')),
    customer_id BIGINT REFERENCES customers(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE work_orders (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEW','ASSIGNED','IN_PROGRESS','ON_HOLD','COMPLETED','CLOSED','CANCELLED')),
    sla_due_at TIMESTAMPTZ,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    site_id BIGINT NOT NULL REFERENCES sites(id),
    assigned_to BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE work_order_status_history (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_by BIGINT REFERENCES users(id),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    note VARCHAR(1000)
);

CREATE TABLE parts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    unit_cost NUMERIC(10,2) NOT NULL,
    stock_qty INT NOT NULL DEFAULT 0
);

CREATE TABLE part_usage (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    part_id BIGINT NOT NULL REFERENCES parts(id),
    qty_used INT NOT NULL,
    logged_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE time_logs (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    technician_id BIGINT NOT NULL REFERENCES users(id),
    minutes INT NOT NULL,
    note VARCHAR(1000),
    logged_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_work_orders_customer ON work_orders(customer_id);
CREATE INDEX idx_work_orders_site ON work_orders(site_id);
CREATE INDEX idx_work_orders_assigned_to ON work_orders(assigned_to);
CREATE INDEX idx_work_orders_status ON work_orders(status);
CREATE INDEX idx_history_work_order ON work_order_status_history(work_order_id);
