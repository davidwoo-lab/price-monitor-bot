CREATE DATABASE IF NOT EXISTS price_monitor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE price_monitor;

CREATE TABLE IF NOT EXISTS product (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    name                VARCHAR(255)    NOT NULL,
    url                 TEXT            NOT NULL,
    target_price        INT             NOT NULL,
    active              TINYINT(1)      NOT NULL DEFAULT 1,
    below_target        TINYINT(1)      NOT NULL DEFAULT 0,
    last_notified_at    DATETIME        NULL,
    created_at          DATETIME        NOT NULL,
    updated_at          DATETIME        NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS price_history (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    product_id  BIGINT      NOT NULL,
    price       INT         NOT NULL,
    created_at  DATETIME    NOT NULL,
    updated_at  DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_price_history_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Per-product notification channels (normalized).
-- A single channel per product is currently enforced at the application level,
-- but the schema allows multiple rows per product for future expansion.
CREATE TABLE IF NOT EXISTS product_notification (
    product_id  BIGINT       NOT NULL,
    channel     VARCHAR(20)  NOT NULL,
    PRIMARY KEY (product_id, channel),
    CONSTRAINT fk_product_notification_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ShedLock: distributed lock store to prevent concurrent scheduler runs across instances.
CREATE TABLE IF NOT EXISTS shedlock (
    name        VARCHAR(64)   NOT NULL,
    lock_until  TIMESTAMP(3)  NOT NULL,
    locked_at   TIMESTAMP(3)  NOT NULL,
    locked_by   VARCHAR(255)  NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Daily per-product price aggregation (for statistics).
-- Raw price_history is purged after aggregation; this table is retained long-term.
CREATE TABLE IF NOT EXISTS price_daily_summary (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    product_id    BIGINT      NOT NULL,
    summary_date  DATE        NOT NULL,
    min_price     INT         NOT NULL,
    max_price     INT         NOT NULL,
    avg_price     INT         NOT NULL,
    sample_count  INT         NOT NULL,
    created_at    DATETIME    NOT NULL,
    updated_at    DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_summary_product_date UNIQUE (product_id, summary_date),
    CONSTRAINT fk_summary_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample data
-- INSERT INTO product (name, url, target_price, active, created_at, updated_at)
-- VALUES ('Apple AirPods Pro 2nd Gen',
--         'https://search.shopping.naver.com/catalog/...',
--         280000, 1, NOW(), NOW());
