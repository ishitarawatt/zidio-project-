ALTER TABLE work_orders ADD COLUMN sla_breached BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_work_orders_sla_breached ON work_orders(sla_breached) WHERE sla_breached = true;
