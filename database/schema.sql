CREATE DATABASE IF NOT EXISTS saree_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE saree_db;

CREATE TABLE IF NOT EXISTS sarees (
  id BIGINT NOT NULL AUTO_INCREMENT,
  image_name VARCHAR(255) NOT NULL,
  image_path VARCHAR(500) NOT NULL,
  primary_color VARCHAR(32) NOT NULL,
  secondary_color VARCHAR(32),
  primary_color_percentage DECIMAL(5,2) NOT NULL,
  secondary_color_percentage DECIMAL(5,2),
  PRIMARY KEY (id),
  UNIQUE KEY uk_saree_image_name (image_name)
);

-- Optional example only. Normally POST /api/sarees/reindex adds every record automatically.
INSERT INTO sarees (image_name,image_path,primary_color,secondary_color,primary_color_percentage,secondary_color_percentage)
VALUES ('saree001.jpg','/saree-images/saree001.jpg','RED','GOLD',72.00,18.00)
ON DUPLICATE KEY UPDATE primary_color=VALUES(primary_color), secondary_color=VALUES(secondary_color);
