-- Seed data for local dev / demo / grading.
-- All seeded users share the password: Password123!
-- (BCrypt hash below is for that password.)

INSERT INTO customers (name, contact_email) VALUES
    ('Meridian Facilities Management', 'ops@meridianfm.example'),
    ('Harborview Offices Ltd', 'facilities@harborview.example');

INSERT INTO sites (customer_id, name, address) VALUES
    (1, 'Meridian HQ Tower', '100 Meridian Way, Springfield'),
    (1, 'Meridian Distribution Center', '55 Industrial Rd, Springfield'),
    (2, 'Harborview Plaza', '12 Harbor St, Bayport');

-- Password for ALL seed users: Password123!
INSERT INTO users (name, email, password_hash, role, customer_id) VALUES
    ('Ava Dispatcher',  'dispatcher@keystone.demo', '$2b$10$lViQKqk.KLaQneNTUE4.cOj6fe400JtJi47IkipIR8q3d28D4YCF.', 'DISPATCHER', NULL),
    ('Sam Manager',     'manager@keystone.demo',    '$2b$10$lViQKqk.KLaQneNTUE4.cOj6fe400JtJi47IkipIR8q3d28D4YCF.', 'MANAGER', NULL),
    ('Theo Technician', 'technician@keystone.demo', '$2b$10$lViQKqk.KLaQneNTUE4.cOj6fe400JtJi47IkipIR8q3d28D4YCF.', 'TECHNICIAN', NULL),
    ('Nina Technician',  'technician2@keystone.demo','$2b$10$lViQKqk.KLaQneNTUE4.cOj6fe400JtJi47IkipIR8q3d28D4YCF.', 'TECHNICIAN', NULL),
    ('Cara Customer',   'customer@keystone.demo',   '$2b$10$lViQKqk.KLaQneNTUE4.cOj6fe400JtJi47IkipIR8q3d28D4YCF.', 'CUSTOMER', 1);

INSERT INTO parts (name, sku, unit_cost, stock_qty) VALUES
    ('HVAC Filter 20x20', 'HVAC-FLT-2020', 12.50, 40),
    ('Copper Pipe 1in (per meter)', 'PLB-CU-1IN', 8.75, 100),
    ('Circuit Breaker 20A', 'ELE-CB-20A', 15.00, 25);

-- A couple of seed work orders so the board / dashboard aren't empty on first login.
INSERT INTO work_orders (code, title, description, priority, status, sla_due_at, customer_id, site_id, assigned_to, created_at, updated_at)
VALUES
    ('WO-2026-0001', 'AC unit not cooling - 4th floor', 'Tenant reports AC blowing warm air.', 'HIGH', 'ASSIGNED',
        now() + interval '24 hours', 1, 1, 3, now(), now()),
    ('WO-2026-0002', 'Leaking pipe in basement', 'Water pooling near utility room.', 'CRITICAL', 'NEW',
        now() + interval '4 hours', 1, 2, NULL, now(), now());

INSERT INTO work_order_status_history (work_order_id, from_status, to_status, changed_by, note)
VALUES
    (1, NULL, 'NEW', 1, 'Created'),
    (1, 'NEW', 'ASSIGNED', 1, 'Assigned to Theo Technician'),
    (2, NULL, 'NEW', 1, 'Created');
