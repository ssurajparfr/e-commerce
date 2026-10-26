INSERT INTO products (sku, name, description, category, price_cents, currency, active)
VALUES ('SKU-001', 'Widget', 'A synthetic product', 'General', 1000, 'EUR', TRUE);

INSERT INTO inventory (product_id, available_quantity, reserved_quantity, version)
SELECT id, 10, 0, 0 FROM products WHERE sku = 'SKU-001';
