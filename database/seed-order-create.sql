-- Seed data for OrderService#createOrder flow.
-- Target: PostgreSQL
--
-- What this script ensures:
-- - A CUSTOMER user exists (users -> customers)
-- - The customer has ACTIVE cart_items (customers -> cart_items)
-- - Referenced shoe_variants exist and have enough stock (shoes -> shoe_variants)
-- - Category + Brand are present for shoes
--
-- Notes about schema drift:
-- - Your entities use BaseEntity column name "ID" (uppercase). In Postgres this is case-sensitive if created quoted.
-- - Older schema exports may include extra NOT NULL columns (e.g. shoes.quantity). This script sets them when present.
--
-- Run:
--   psql -d <db> -f database/seed-order-create.sql

BEGIN;

-- ---------------------------------------------------------------------------
-- Schema compatibility helpers
-- This script adapts to common drift between:
-- - your JPA entities (id column named "ID" in annotations)
-- - actual PostgreSQL tables (usually unquoted lowercase id)
-- - older exported schemas (may miss version, sku, is_active; may have shoes.quantity NOT NULL)
-- ---------------------------------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'categories' AND column_name = 'id'
  )
  AND EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'categories' AND column_name = 'ID'
  ) THEN
    RAISE NOTICE 'Detected quoted "ID" columns. This seed assumes lowercase id exists; consider regenerating schema or adjust table definitions.';
  END IF;
END$$;

-- Optional cleanup (uncomment if you want a clean slate)
-- TRUNCATE TABLE
--   cart_items,
--   carts,
--   shoe_variants,
--   shoes,
--   brands,
--   categories,
--   customers,
--   users
-- RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------------------
-- Fixed UUIDs (easy to reference when testing)
-- ---------------------------------------------------------------------------
-- USER/CUSTOMER
-- user_id & customer_id (trùng nhau để bạn có thể dùng sẵn user có trong DB):
--   55565d71-611a-462c-9e0f-289d40272e96
-- keycloak_id demo (nếu cần): aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa   (put this in JWT "sub" if your code maps it)
--
-- CATALOG
-- category_running_id: 44444444-4444-4444-4444-444444444444
-- category_lifestyle_id:55555555-5555-5555-5555-555555555555
-- brand_nike_id:        66666666-6666-6666-6666-666666666666
-- brand_adidas_id:      77777777-7777-7777-7777-777777777777
-- shoe_pegasus_id:      88888888-8888-8888-8888-888888888888
-- shoe_samba_id:        99999999-9999-9999-9999-999999999999
-- shoe_forum_id:        12121212-1212-1212-1212-121212121212
--
-- VARIANTS
-- pegasus_42_black:     13131313-1313-1313-1313-131313131313
-- pegasus_43_black:     14141414-1414-1414-1414-141414141414
-- samba_42_white:       15151515-1515-1515-1515-151515151515
-- forum_41_blue:        16161616-1616-1616-1616-161616161616
--
-- CART ITEMS
-- cart_item_1:          17171717-1717-1717-1717-171717171717
-- cart_item_2:          18181818-1818-1818-1818-181818181818

-- ---------------------------------------------------------------------------
-- Categories
-- ---------------------------------------------------------------------------
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='categories' AND column_name='version') THEN
    EXECUTE $q$
      INSERT INTO categories (id, version, name, description, slug)
      VALUES
        ('44444444-4444-4444-4444-444444444444', 0, 'Running', 'Giày chạy bộ êm ái, bền bỉ, tối ưu sải chân.', 'running'),
        ('55555555-5555-5555-5555-555555555555', 0, 'Lifestyle', 'Giày thời trang đi hằng ngày, phối đồ dễ.', 'lifestyle')
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSE
    EXECUTE $q$
      INSERT INTO categories (id, name, description, slug)
      VALUES
        ('44444444-4444-4444-4444-444444444444', 'Running', 'Giày chạy bộ êm ái, bền bỉ, tối ưu sải chân.', 'running'),
        ('55555555-5555-5555-5555-555555555555', 'Lifestyle', 'Giày thời trang đi hằng ngày, phối đồ dễ.', 'lifestyle')
      ON CONFLICT (id) DO NOTHING
    $q$;
  END IF;
END$$;

-- ---------------------------------------------------------------------------
-- Brands
-- ---------------------------------------------------------------------------
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='brands' AND column_name='version') THEN
    EXECUTE $q$
      INSERT INTO brands (id, version, name, slug, description, logo_url, country)
      VALUES
        ('66666666-6666-6666-6666-666666666666', 0, 'Nike', 'nike', 'Just do it — classic & performance.', 'https://cdn.example.com/brands/nike.png', 'USA'),
        ('77777777-7777-7777-7777-777777777777', 0, 'Adidas', 'adidas', 'Iconic three stripes — street & sport.', 'https://cdn.example.com/brands/adidas.png', 'Germany')
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSE
    EXECUTE $q$
      INSERT INTO brands (id, name, slug, description, logo_url, country)
      VALUES
        ('66666666-6666-6666-6666-666666666666', 'Nike', 'nike', 'Just do it — classic & performance.', 'https://cdn.example.com/brands/nike.png', 'USA'),
        ('77777777-7777-7777-7777-777777777777', 'Adidas', 'adidas', 'Iconic three stripes — street & sport.', 'https://cdn.example.com/brands/adidas.png', 'Germany')
      ON CONFLICT (id) DO NOTHING
    $q$;
  END IF;
END$$;

-- ---------------------------------------------------------------------------
-- Shoes
-- Entity does not map shoes.quantity, but older schema may still require it (NOT NULL).
-- We insert quantity=1000 just in case.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
  has_version boolean;
  has_quantity boolean;
BEGIN
  has_version := EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='shoes' AND column_name='version');
  has_quantity := EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='shoes' AND column_name='quantity');

  IF has_version AND has_quantity THEN
    EXECUTE $q$
      INSERT INTO shoes (id, version, gender, status, quantity, material, slug, description, name, category_id, brand_id, price)
      VALUES
        ('88888888-8888-8888-8888-888888888888', 0, 'MEN', 'ACTIVE', 1000, 'Engineered mesh', 'nike-air-zoom-pegasus-40-black', 'Nike Air Zoom Pegasus 40: đệm êm, phản hồi tốt, hợp chạy hằng ngày.', 'Nike Air Zoom Pegasus 40', '44444444-4444-4444-4444-444444444444', '66666666-6666-6666-6666-666666666666', 2899000),
        ('99999999-9999-9999-9999-999999999999', 0, 'UNISEX', 'ACTIVE', 1000, 'Leather & suede', 'adidas-samba-og-white', 'Adidas Samba OG: huyền thoại sân cỏ, nay là biểu tượng streetwear.', 'Adidas Samba OG', '55555555-5555-5555-5555-555555555555', '77777777-7777-7777-7777-777777777777', 2490000),
        ('12121212-1212-1212-1212-121212121212', 0, 'UNISEX', 'ACTIVE', 1000, 'Synthetic leather', 'adidas-forum-low-blue', 'Adidas Forum Low: form retro basketball, lên chân chắc chắn.', 'Adidas Forum Low', '55555555-5555-5555-5555-555555555555', '77777777-7777-7777-7777-777777777777', 2690000)
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSIF has_version AND NOT has_quantity THEN
    EXECUTE $q$
      INSERT INTO shoes (id, version, gender, status, material, slug, description, name, category_id, brand_id, price)
      VALUES
        ('88888888-8888-8888-8888-888888888888', 0, 'MEN', 'ACTIVE', 'Engineered mesh', 'nike-air-zoom-pegasus-40-black', 'Nike Air Zoom Pegasus 40: đệm êm, phản hồi tốt, hợp chạy hằng ngày.', 'Nike Air Zoom Pegasus 40', '44444444-4444-4444-4444-444444444444', '66666666-6666-6666-6666-666666666666', 2899000),
        ('99999999-9999-9999-9999-999999999999', 0, 'UNISEX', 'ACTIVE', 'Leather & suede', 'adidas-samba-og-white', 'Adidas Samba OG: huyền thoại sân cỏ, nay là biểu tượng streetwear.', 'Adidas Samba OG', '55555555-5555-5555-5555-555555555555', '77777777-7777-7777-7777-777777777777', 2490000),
        ('12121212-1212-1212-1212-121212121212', 0, 'UNISEX', 'ACTIVE', 'Synthetic leather', 'adidas-forum-low-blue', 'Adidas Forum Low: form retro basketball, lên chân chắc chắn.', 'Adidas Forum Low', '55555555-5555-5555-5555-555555555555', '77777777-7777-7777-7777-777777777777', 2690000)
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSIF NOT has_version AND has_quantity THEN
    EXECUTE $q$
      INSERT INTO shoes (id, gender, status, quantity, material, slug, description, name, category_id, brand_id, price)
      VALUES
        ('88888888-8888-8888-8888-888888888888', 'MEN', 'ACTIVE', 1000, 'Engineered mesh', 'nike-air-zoom-pegasus-40-black', 'Nike Air Zoom Pegasus 40: đệm êm, phản hồi tốt, hợp chạy hằng ngày.', 'Nike Air Zoom Pegasus 40', '44444444-4444-4444-4444-444444444444', '66666666-6666-6666-6666-666666666666', 2899000),
        ('99999999-9999-9999-9999-999999999999', 'UNISEX', 'ACTIVE', 1000, 'Leather & suede', 'adidas-samba-og-white', 'Adidas Samba OG: huyền thoại sân cỏ, nay là biểu tượng streetwear.', 'Adidas Samba OG', '55555555-5555-5555-5555-555555555555', '77777777-7777-7777-7777-777777777777', 2490000),
        ('12121212-1212-1212-1212-121212121212', 'UNISEX', 'ACTIVE', 1000, 'Synthetic leather', 'adidas-forum-low-blue', 'Adidas Forum Low: form retro basketball, lên chân chắc chắn.', 'Adidas Forum Low', '55555555-5555-5555-5555-555555555555', '77777777-7777-7777-7777-777777777777', 2690000)
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSE
    EXECUTE $q$
      INSERT INTO shoes (id, gender, status, material, slug, description, name, category_id, brand_id, price)
      VALUES
        ('88888888-8888-8888-8888-888888888888', 'MEN', 'ACTIVE', 'Engineered mesh', 'nike-air-zoom-pegasus-40-black', 'Nike Air Zoom Pegasus 40: đệm êm, phản hồi tốt, hợp chạy hằng ngày.', 'Nike Air Zoom Pegasus 40', '44444444-4444-4444-4444-444444444444', '66666666-6666-6666-6666-666666666666', 2899000),
        ('99999999-9999-9999-9999-999999999999', 'UNISEX', 'ACTIVE', 'Leather & suede', 'adidas-samba-og-white', 'Adidas Samba OG: huyền thoại sân cỏ, nay là biểu tượng streetwear.', 'Adidas Samba OG', '55555555-5555-5555-5555-555555555555', '77777777-7777-7777-7777-777777777777', 2490000),
        ('12121212-1212-1212-1212-121212121212', 'UNISEX', 'ACTIVE', 'Synthetic leather', 'adidas-forum-low-blue', 'Adidas Forum Low: form retro basketball, lên chân chắc chắn.', 'Adidas Forum Low', '55555555-5555-5555-5555-555555555555', '77777777-7777-7777-7777-777777777777', 2690000)
      ON CONFLICT (id) DO NOTHING
    $q$;
  END IF;
END$$;

-- ---------------------------------------------------------------------------
-- Shoe Variants
-- Older schema export had no sku; your entity requires sku (nullable=false).
-- ---------------------------------------------------------------------------
DO $$
DECLARE
  has_version boolean;
  has_sku boolean;
BEGIN
  has_version := EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='shoe_variants' AND column_name='version');
  has_sku := EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='shoe_variants' AND column_name='sku');

  IF has_version AND has_sku THEN
    EXECUTE $q$
      INSERT INTO shoe_variants (id, version, shoe_id, size, color, quantity, sku)
      VALUES
        ('13131313-1313-1313-1313-131313131313', 0, '88888888-8888-8888-8888-888888888888', '42', 'Black', 50, 'NK-PEG40-BLK-42'),
        ('14141414-1414-1414-1414-141414141414', 0, '88888888-8888-8888-8888-888888888888', '43', 'Black', 40, 'NK-PEG40-BLK-43'),
        ('15151515-1515-1515-1515-151515151515', 0, '99999999-9999-9999-9999-999999999999', '42', 'Cloud White', 30, 'AD-SAMBA-WHT-42'),
        ('16161616-1616-1616-1616-161616161616', 0, '12121212-1212-1212-1212-121212121212', '41', 'Blue', 25, 'AD-FORUM-BLU-41')
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSIF has_version AND NOT has_sku THEN
    EXECUTE $q$
      INSERT INTO shoe_variants (id, version, shoe_id, size, color, quantity)
      VALUES
        ('13131313-1313-1313-1313-131313131313', 0, '88888888-8888-8888-8888-888888888888', '42', 'Black', 50),
        ('14141414-1414-1414-1414-141414141414', 0, '88888888-8888-8888-8888-888888888888', '43', 'Black', 40),
        ('15151515-1515-1515-1515-151515151515', 0, '99999999-9999-9999-9999-999999999999', '42', 'Cloud White', 30),
        ('16161616-1616-1616-1616-161616161616', 0, '12121212-1212-1212-1212-121212121212', '41', 'Blue', 25)
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSIF NOT has_version AND has_sku THEN
    EXECUTE $q$
      INSERT INTO shoe_variants (id, shoe_id, size, color, quantity, sku)
      VALUES
        ('13131313-1313-1313-1313-131313131313', '88888888-8888-8888-8888-888888888888', '42', 'Black', 50, 'NK-PEG40-BLK-42'),
        ('14141414-1414-1414-1414-141414141414', '88888888-8888-8888-8888-888888888888', '43', 'Black', 40, 'NK-PEG40-BLK-43'),
        ('15151515-1515-1515-1515-151515151515', '99999999-9999-9999-9999-999999999999', '42', 'Cloud White', 30, 'AD-SAMBA-WHT-42'),
        ('16161616-1616-1616-1616-161616161616', '12121212-1212-1212-1212-121212121212', '41', 'Blue', 25, 'AD-FORUM-BLU-41')
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSE
    EXECUTE $q$
      INSERT INTO shoe_variants (id, shoe_id, size, color, quantity)
      VALUES
        ('13131313-1313-1313-1313-131313131313', '88888888-8888-8888-8888-888888888888', '42', 'Black', 50),
        ('14141414-1414-1414-1414-141414141414', '88888888-8888-8888-8888-888888888888', '43', 'Black', 40),
        ('15151515-1515-1515-1515-151515151515', '99999999-9999-9999-9999-999999999999', '42', 'Cloud White', 30),
        ('16161616-1616-1616-1616-161616161616', '12121212-1212-1212-1212-121212121212', '41', 'Blue', 25)
      ON CONFLICT (id) DO NOTHING
    $q$;
  END IF;
END$$;

-- ---------------------------------------------------------------------------
-- User + Customer
-- If your auth integrates with Keycloak, keep keycloak_id consistent with token subject mapping.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
  has_version boolean;
  has_last_seen boolean;
BEGIN
  has_version := EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='users' AND column_name='version');
  has_last_seen := EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='users' AND column_name='LAST_SEEN_AT')
                  OR EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='users' AND lower(column_name)='last_seen_at');

  IF has_version AND has_last_seen THEN
    EXECUTE $q$
      INSERT INTO users (id, version, keycloak_id, role, username, first_name, last_name, phone_number, email, date_of_birth, avatar_url, status, last_seen_at)
      VALUES
        ('55565d71-611a-462c-9e0f-289d40272e96', 0, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ROLE_CUSTOMER', 'nhat.customer', 'Nhat', 'Dinh', '0909000000', 'nhat.customer@ssos.local', '2002-10-10', 'https://cdn.example.com/avatars/nhat.png', 'ACTIVE', NULL)
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSIF has_version AND NOT has_last_seen THEN
    EXECUTE $q$
      INSERT INTO users (id, version, keycloak_id, role, username, first_name, last_name, phone_number, email, date_of_birth, avatar_url, status)
      VALUES
        ('55565d71-611a-462c-9e0f-289d40272e96', 0, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ROLE_CUSTOMER', 'nhat.customer', 'Nhat', 'Dinh', '0909000000', 'nhat.customer@ssos.local', '2002-10-10', 'https://cdn.example.com/avatars/nhat.png', 'ACTIVE')
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSIF NOT has_version AND has_last_seen THEN
    EXECUTE $q$
      INSERT INTO users (id, keycloak_id, role, username, first_name, last_name, phone_number, email, date_of_birth, avatar_url, status, last_seen_at)
      VALUES
        ('55565d71-611a-462c-9e0f-289d40272e96', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ROLE_CUSTOMER', 'nhat.customer', 'Nhat', 'Dinh', '0909000000', 'nhat.customer@ssos.local', '2002-10-10', 'https://cdn.example.com/avatars/nhat.png', 'ACTIVE', NULL)
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSE
    EXECUTE $q$
      INSERT INTO users (id, keycloak_id, role, username, first_name, last_name, phone_number, email, date_of_birth, avatar_url, status)
      VALUES
        ('55565d71-611a-462c-9e0f-289d40272e96', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ROLE_CUSTOMER', 'nhat.customer', 'Nhat', 'Dinh', '0909000000', 'nhat.customer@ssos.local', '2002-10-10', 'https://cdn.example.com/avatars/nhat.png', 'ACTIVE')
      ON CONFLICT (id) DO NOTHING
    $q$;
  END IF;
END$$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='customers' AND column_name='version') THEN
    EXECUTE $q$
      INSERT INTO customers (id, version, user_id, loyalty_points)
      VALUES
        ('55565d71-611a-462c-9e0f-289d40272e96', 0, '55565d71-611a-462c-9e0f-289d40272e96', 1200)
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSE
    EXECUTE $q$
      INSERT INTO customers (id, user_id, loyalty_points)
      VALUES
        ('55565d71-611a-462c-9e0f-289d40272e96', '55565d71-611a-462c-9e0f-289d40272e96', 1200)
      ON CONFLICT (id) DO NOTHING
    $q$;
  END IF;
END$$;

-- ---------------------------------------------------------------------------
-- Cart Items (gắn trực tiếp với Customer)
-- OrderService.validateItems requires:
-- - customer có cart_items
-- - cart_items size matches request size
-- - each cart_item isActive = true
-- ---------------------------------------------------------------------------
DO $$
DECLARE
  has_version boolean;
  has_is_active boolean;
BEGIN
  has_version := EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='cart_items' AND column_name='version');
  has_is_active := EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='cart_items' AND column_name='is_active')
                   OR EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='cart_items' AND column_name='isActive')
                   OR EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='cart_items' AND column_name='is_active');

  IF has_version AND has_is_active THEN
    EXECUTE $q$
      INSERT INTO cart_items (id, version, customer_id, shoe_variant_id, quantity, is_active)
      VALUES
        ('17171717-1717-1717-1717-171717171717', 0, '55565d71-611a-462c-9e0f-289d40272e96', '13131313-1313-1313-1313-131313131313', 2, TRUE),
        ('18181818-1818-1818-1818-181818181818', 0, '55565d71-611a-462c-9e0f-289d40272e96', '15151515-1515-1515-1515-151515151515', 1, TRUE)
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSIF has_version AND NOT has_is_active THEN
    EXECUTE $q$
      INSERT INTO cart_items (id, version, customer_id, shoe_variant_id, quantity)
      VALUES
        ('17171717-1717-1717-1717-171717171717', 0, '55565d71-611a-462c-9e0f-289d40272e96', '13131313-1313-1313-1313-131313131313', 2),
        ('18181818-1818-1818-1818-181818181818', 0, '55565d71-611a-462c-9e0f-289d40272e96', '15151515-1515-1515-1515-151515151515', 1)
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSIF NOT has_version AND has_is_active THEN
    EXECUTE $q$
      INSERT INTO cart_items (id, customer_id, shoe_variant_id, quantity, is_active)
      VALUES
        ('17171717-1717-1717-1717-171717171717', '55565d71-611a-462c-9e0f-289d40272e96', '13131313-1313-1313-1313-131313131313', 2, TRUE),
        ('18181818-1818-1818-1818-181818181818', '55565d71-611a-462c-9e0f-289d40272e96', '15151515-1515-1515-1515-151515151515', 1, TRUE)
      ON CONFLICT (id) DO NOTHING
    $q$;
  ELSE
    EXECUTE $q$
      INSERT INTO cart_items (id, customer_id, shoe_variant_id, quantity)
      VALUES
        ('17171717-1717-1717-1717-171717171717', '55565d71-611a-462c-9e0f-289d40272e96', '13131313-1313-1313-1313-131313131313', 2),
        ('18181818-1818-1818-1818-181818181818', '55565d71-611a-462c-9e0f-289d40272e96', '15151515-1515-1515-1515-151515151515', 1)
      ON CONFLICT (id) DO NOTHING
    $q$;
  END IF;
END$$;

COMMIT;

