ALTER TABLE products
    ADD COLUMN IF NOT EXISTS color_hex_code VARCHAR(7);

UPDATE products
SET color_hex_code = CASE LOWER(name)
    WHEN 'alkaline water' THEN '#22C55E'
    WHEN 'ice' THEN '#38BDF8'
    WHEN 'mineral water' THEN '#0EA5E9'
    WHEN 'purified water' THEN '#2563EB'
    ELSE '#123456'
END
WHERE color_hex_code IS NULL OR BTRIM(color_hex_code) = '';

ALTER TABLE products
    ALTER COLUMN color_hex_code SET DEFAULT '#123456';

ALTER TABLE products
    ALTER COLUMN color_hex_code SET NOT NULL;
