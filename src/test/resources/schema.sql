-- =====================================================
-- H2 Test Schema - PostgreSQL Compatibility Layer
-- Place this file in: src/test/resources/
--
-- This script runs BEFORE Hibernate's auto-DDL
-- to create PostgreSQL-specific enum types that H2
-- doesn't natively understand.
--
-- STRATEGY: Use CREATE DOMAIN ... AS VARCHAR(255) instead
-- of CREATE TYPE ... AS ENUM (...) so we DON'T need to
-- know the exact enum values.
--
-- When you add a new entity with @JdbcTypeCode(SqlTypes.NAMED_ENUM)
-- and @Column(columnDefinition = "your_type_name"), add a
-- corresponding CREATE DOMAIN statement here.
-- =====================================================

-- ==================== USER ENTITY ====================
CREATE DOMAIN IF NOT EXISTS users_authentication_type AS VARCHAR(255);

-- ==================== ORDER TRACK ENTITY ====================
CREATE DOMAIN IF NOT EXISTS order_track_status AS VARCHAR(255);

-- ==================== ORDER ENTITY ====================
CREATE DOMAIN IF NOT EXISTS orders_payment_method AS VARCHAR(255);
CREATE DOMAIN IF NOT EXISTS orders_status AS VARCHAR(255);

-- ==================== SETTING ENTITY ====================
CREATE DOMAIN IF NOT EXISTS settings_category AS VARCHAR(255);

-- =====================================================
-- ADD MORE DOMAINS BELOW as you discover new errors:
--
-- Pattern: CREATE DOMAIN IF NOT EXISTS <type_name> AS VARCHAR(255);
--
-- The <type_name> must match exactly the string in
-- @Column(columnDefinition = "<type_name>")
-- =====================================================
