-- V4: richer demo data so every screen (board, dashboard, technician view,
-- customer portal) has something meaningful to show.
-- Additive only: nothing from V1-V3 is changed. All seeded users share the
-- password Password123!

-- ---------- more customers, sites ----------
INSERT INTO customers (name, contact_email) VALUES
    ('Lakeside Medical Center', 'facilities@lakeside.example');

INSERT INTO sites (customer_id, name, address) VALUES
    (1, 'Meridian North Campus', '200 North Ave, Springfield'),
    (1, 'Meridian Data Hall', '8 Server Ln, Springfield'),
    (2, 'Harborview Annex', '14 Harbor St, Bayport'),
    (2, 'Harborview Parking Structure', '20 Dock Rd, Bayport'),
    ((SELECT id FROM customers WHERE name = 'Lakeside Medical Center'), 'Lakeside Main Building', '1 Lakeside Blvd, Lakeview'),
    ((SELECT id FROM customers WHERE name = 'Lakeside Medical Center'), 'Lakeside Outpatient Clinic', '45 Clinic Way, Lakeview');

-- ---------- more users ----------
INSERT INTO users (name, email, password_hash, role, customer_id) VALUES
    ('Omar Technician', 'technician3@keystone.demo', '$2b$10$lViQKqk.KLaQneNTUE4.cOj6fe400JtJi47IkipIR8q3d28D4YCF.', 'TECHNICIAN', NULL),
    ('Priya Dispatcher', 'dispatcher2@keystone.demo', '$2b$10$lViQKqk.KLaQneNTUE4.cOj6fe400JtJi47IkipIR8q3d28D4YCF.', 'DISPATCHER', NULL),
    ('Harborview Contact', 'customer2@keystone.demo', '$2b$10$lViQKqk.KLaQneNTUE4.cOj6fe400JtJi47IkipIR8q3d28D4YCF.', 'CUSTOMER', 2),
    ('Lakeside Contact', 'customer3@keystone.demo', '$2b$10$lViQKqk.KLaQneNTUE4.cOj6fe400JtJi47IkipIR8q3d28D4YCF.', 'CUSTOMER',
        (SELECT id FROM customers WHERE name = 'Lakeside Medical Center'));

-- ---------- more parts ----------
INSERT INTO parts (name, sku, unit_cost, stock_qty) VALUES
    ('HVAC Run Capacitor 45uF', 'HVAC-CAP-45', 18.90, 30),
    ('Pressure Relief Valve', 'PLB-VALVE-PR', 42.00, 12),
    ('Copper Wire 12AWG (per meter)', 'ELE-WIRE-12', 1.85, 500),
    ('Digital Thermostat', 'ELE-THERM-01', 64.50, 15),
    ('Fuse 10A', 'ELE-FUSE-10', 2.40, 200);

-- ---------- 20 more work orders (WO-2026-0003 .. WO-2026-0022) ----------
-- Codes are contiguous so the app's next generated code (count + 1) never collides.
INSERT INTO work_orders
    (code, title, description, priority, status, sla_due_at, sla_breached, customer_id, site_id, assigned_to, created_at, updated_at)
SELECT v.code, v.title, v.descr, v.priority, v.status,
       now() + v.sla_offset::interval, v.breached,
       s.customer_id, s.id, u.id,
       now() - v.age::interval, now() - v.upd::interval
FROM (VALUES
    ('WO-2026-0003', 'Rooftop HVAC unit short-cycling', 'Unit turns on and off every few minutes; tenants on floor 9 report uneven cooling.', 'HIGH', 'IN_PROGRESS', '-2 hours', true, 'Meridian HQ Tower', 'technician@keystone.demo', '30 hours', '1 hour'),
    ('WO-2026-0004', 'Server room cooling alarm', 'Temperature alarm triggered in the data hall; cooling loop pressure is low.', 'CRITICAL', 'NEW', '-30 minutes', true, 'Meridian Data Hall', NULL, '5 hours', '5 hours'),
    ('WO-2026-0005', 'Lobby lighting flicker', 'Several ceiling fixtures flicker intermittently near the reception desk.', 'LOW', 'NEW', '120 hours', false, 'Meridian HQ Tower', NULL, '6 hours', '6 hours'),
    ('WO-2026-0006', 'Emergency generator test failure', 'Monthly load test failed to start on the first attempt.', 'CRITICAL', 'ASSIGNED', '2 hours', false, 'Meridian Distribution Center', 'technician2@keystone.demo', '4 hours', '3 hours'),
    ('WO-2026-0007', 'Restroom water pressure low', 'Low pressure across all restrooms on the ground floor.', 'MEDIUM', 'ASSIGNED', '40 hours', false, 'Harborview Plaza', 'technician3@keystone.demo', '10 hours', '9 hours'),
    ('WO-2026-0008', 'Elevator machine room ventilation', 'Machine room running hot; exhaust fan appears to be underperforming.', 'HIGH', 'ASSIGNED', '14 hours', false, 'Meridian North Campus', 'technician@keystone.demo', '8 hours', '7 hours'),
    ('WO-2026-0009', 'Boiler pressure valve replacement', 'Relief valve weeping under load; replacement approved.', 'HIGH', 'IN_PROGRESS', '10 hours', false, 'Meridian Distribution Center', 'technician2@keystone.demo', '20 hours', '2 hours'),
    ('WO-2026-0010', 'Electrical panel upgrade - 3rd floor', 'Replace two aging breakers and re-label the panel schedule.', 'MEDIUM', 'IN_PROGRESS', '30 hours', false, 'Meridian North Campus', 'technician3@keystone.demo', '26 hours', '4 hours'),
    ('WO-2026-0011', 'Drain blockage in loading dock', 'Standing water at the dock door after rain.', 'MEDIUM', 'IN_PROGRESS', '-5 hours', true, 'Meridian Distribution Center', 'technician2@keystone.demo', '80 hours', '3 hours'),
    ('WO-2026-0012', 'Chiller compressor noise', 'Grinding noise from the compressor housing during peak load.', 'HIGH', 'IN_PROGRESS', '6 hours', false, 'Harborview Annex', 'technician3@keystone.demo', '18 hours', '1 hour'),
    ('WO-2026-0013', 'Exhaust fan replacement', 'Waiting on delivery of the replacement fan assembly.', 'LOW', 'ON_HOLD', '100 hours', false, 'Harborview Parking Structure', 'technician@keystone.demo', '50 hours', '20 hours'),
    ('WO-2026-0014', 'Water heater replacement', 'On hold until the client confirms access to the plant room.', 'MEDIUM', 'ON_HOLD', '20 hours', false, 'Lakeside Main Building', 'technician2@keystone.demo', '40 hours', '12 hours'),
    ('WO-2026-0015', 'Thermostat calibration - west wing', 'Recalibrate zone thermostats; awaiting manager sign-off.', 'LOW', 'COMPLETED', '90 hours', false, 'Meridian HQ Tower', 'technician3@keystone.demo', '60 hours', '6 hours'),
    ('WO-2026-0016', 'Sump pump inspection', 'Inspection complete, float switch replaced; awaiting sign-off.', 'MEDIUM', 'COMPLETED', '30 hours', false, 'Lakeside Outpatient Clinic', 'technician@keystone.demo', '45 hours', '5 hours'),
    ('WO-2026-0017', 'Replace failed circuit breaker', 'Breaker tripped repeatedly; replaced and load-tested.', 'HIGH', 'CLOSED', '-20 hours', false, 'Meridian HQ Tower', 'technician@keystone.demo', '100 hours', '30 hours'),
    ('WO-2026-0018', 'Quarterly HVAC filter change', 'Scheduled preventive maintenance across all air handlers.', 'LOW', 'CLOSED', '-60 hours', false, 'Harborview Plaza', 'technician2@keystone.demo', '200 hours', '100 hours'),
    ('WO-2026-0019', 'Burst pipe repair - stairwell', 'Emergency repair; water shut off and section replaced.', 'CRITICAL', 'CLOSED', '-50 hours', false, 'Meridian North Campus', 'technician3@keystone.demo', '90 hours', '10 hours'),
    ('WO-2026-0020', 'Duplicate request - AC noise', 'Cancelled: duplicate of an existing request.', 'LOW', 'CANCELLED', '60 hours', false, 'Harborview Annex', NULL, '30 hours', '28 hours'),
    ('WO-2026-0021', 'Annual fire panel inspection', 'Yearly inspection completed with no findings.', 'LOW', 'CLOSED', '-30 hours', false, 'Lakeside Main Building', 'technician@keystone.demo', '150 hours', '60 hours'),
    ('WO-2026-0022', 'Door access controller fault', 'Controller on the east entrance was resetting; firmware and PSU replaced.', 'MEDIUM', 'CLOSED', '-10 hours', false, 'Meridian Data Hall', 'technician3@keystone.demo', '70 hours', '40 hours')
) AS v(code, title, descr, priority, status, sla_offset, breached, site_name, tech_email, age, upd)
JOIN sites s ON s.name = v.site_name
LEFT JOIN users u ON u.email = v.tech_email;

-- ---------- status history: one row per step along each order's path ----------
WITH paths(status, path) AS (VALUES
    ('NEW',         ARRAY['NEW']),
    ('ASSIGNED',    ARRAY['NEW','ASSIGNED']),
    ('IN_PROGRESS', ARRAY['NEW','ASSIGNED','IN_PROGRESS']),
    ('ON_HOLD',     ARRAY['NEW','ASSIGNED','IN_PROGRESS','ON_HOLD']),
    ('COMPLETED',   ARRAY['NEW','ASSIGNED','IN_PROGRESS','COMPLETED']),
    ('CLOSED',      ARRAY['NEW','ASSIGNED','IN_PROGRESS','COMPLETED','CLOSED']),
    ('CANCELLED',   ARRAY['NEW','CANCELLED'])
)
INSERT INTO work_order_status_history (work_order_id, from_status, to_status, changed_by, changed_at, note)
SELECT w.id,
       CASE WHEN g.i = 1 THEN NULL ELSE p.path[g.i - 1] END,
       p.path[g.i],
       CASE p.path[g.i]
            WHEN 'NEW' THEN d.id
            WHEN 'ASSIGNED' THEN d.id
            WHEN 'CANCELLED' THEN d.id
            WHEN 'CLOSED' THEN m.id
            ELSE COALESCE(w.assigned_to, d.id)
       END,
       w.created_at + (g.i - 1) * interval '20 minutes',
       CASE p.path[g.i]
            WHEN 'NEW' THEN 'Created'
            WHEN 'ASSIGNED' THEN 'Assigned to technician'
            WHEN 'IN_PROGRESS' THEN 'Work started'
            WHEN 'ON_HOLD' THEN 'Waiting on parts or access'
            WHEN 'COMPLETED' THEN 'Work completed'
            WHEN 'CLOSED' THEN 'Signed off'
            WHEN 'CANCELLED' THEN 'Cancelled by dispatcher'
       END
FROM work_orders w
JOIN paths p ON p.status = w.status
CROSS JOIN LATERAL generate_series(1, array_length(p.path, 1)) AS g(i)
CROSS JOIN (SELECT id FROM users WHERE email = 'dispatcher@keystone.demo') d
CROSS JOIN (SELECT id FROM users WHERE email = 'manager@keystone.demo') m
WHERE w.code BETWEEN 'WO-2026-0003' AND 'WO-2026-0022';

-- ---------- time logs on every job that has been worked ----------
INSERT INTO time_logs (work_order_id, technician_id, minutes, note, logged_at)
SELECT w.id, w.assigned_to,
       ((30 + (w.id % 5) * 15) * g.i)::int,
       CASE g.i WHEN 1 THEN 'Initial diagnosis and site check' ELSE 'Repair work and testing' END,
       w.created_at + g.i * interval '30 minutes'
FROM work_orders w
CROSS JOIN generate_series(1, 2) AS g(i)
WHERE w.code BETWEEN 'WO-2026-0003' AND 'WO-2026-0022'
  AND w.status IN ('IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CLOSED')
  AND w.assigned_to IS NOT NULL;

-- ---------- parts used, with matching stock decrement ----------
INSERT INTO part_usage (work_order_id, part_id, qty_used, logged_at)
SELECT w.id, p.id, q.qty, w.created_at + interval '1 hour'
FROM (VALUES
    ('WO-2026-0003', 'HVAC-FLT-2020', 2),
    ('WO-2026-0003', 'HVAC-CAP-45', 1),
    ('WO-2026-0009', 'PLB-VALVE-PR', 1),
    ('WO-2026-0010', 'ELE-CB-20A', 3),
    ('WO-2026-0010', 'ELE-WIRE-12', 4),
    ('WO-2026-0011', 'PLB-CU-1IN', 6),
    ('WO-2026-0012', 'HVAC-FLT-2020', 4),
    ('WO-2026-0015', 'ELE-THERM-01', 1),
    ('WO-2026-0017', 'ELE-CB-20A', 2),
    ('WO-2026-0018', 'HVAC-FLT-2020', 8),
    ('WO-2026-0019', 'PLB-CU-1IN', 10),
    ('WO-2026-0021', 'ELE-FUSE-10', 5)
) AS q(code, sku, qty)
JOIN work_orders w ON w.code = q.code
JOIN parts p ON p.sku = q.sku;

UPDATE parts
SET stock_qty = parts.stock_qty - used.total
FROM (SELECT part_id, SUM(qty_used)::int AS total FROM part_usage GROUP BY part_id) AS used
WHERE parts.id = used.part_id;
