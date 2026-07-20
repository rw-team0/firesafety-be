-- V1__create_initial_tables.sql
-- firesafety 초기 스키마

CREATE TABLE `user` (
        user_id BIGINT NOT NULL AUTO_INCREMENT,
        email VARCHAR(100) NOT NULL,
        password VARCHAR(255) NOT NULL,
        name VARCHAR(50) NOT NULL,
        phone VARCHAR(20) NULL,
        role ENUM('SUPER_ADMIN', 'ADMIN', 'GENERAL') NOT NULL DEFAULT 'GENERAL',
        created_by BIGINT NULL,
        fcm_token VARCHAR(255) NULL,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NULL,
        PRIMARY KEY (user_id),
        CONSTRAINT uk_user_email UNIQUE (email),
        CONSTRAINT fk_user_created_by FOREIGN KEY (created_by)
            REFERENCES `user` (user_id)
            ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE site (
      site_id BIGINT NOT NULL AUTO_INCREMENT,
      name VARCHAR(100) NOT NULL,
      address VARCHAR(200) NULL,
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (site_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_site (
       mapping_id BIGINT NOT NULL AUTO_INCREMENT,
       user_id BIGINT NOT NULL,
       site_id BIGINT NOT NULL,
       assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
       PRIMARY KEY (mapping_id),
       CONSTRAINT uk_user_site UNIQUE (user_id, site_id),
       CONSTRAINT fk_user_site_user FOREIGN KEY (user_id)
           REFERENCES `user` (user_id)
           ON UPDATE RESTRICT ON DELETE CASCADE,
       CONSTRAINT fk_user_site_site FOREIGN KEY (site_id)
           REFERENCES site (site_id)
           ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE panel (
       panel_id BIGINT NOT NULL AUTO_INCREMENT,
       site_id BIGINT NOT NULL,
       device_serial VARCHAR(50) NOT NULL,
       m_no VARCHAR(5) NOT NULL,
       status ENUM('NORMAL', 'CAUTION', 'RISK') NOT NULL DEFAULT 'NORMAL',
       is_online TINYINT(1) NOT NULL DEFAULT 0,
       last_communicated_at DATETIME NULL,
       PRIMARY KEY (panel_id),
       CONSTRAINT uk_panel_device_serial UNIQUE (device_serial),
       CONSTRAINT fk_panel_site FOREIGN KEY (site_id)
           REFERENCES site (site_id)
           ON UPDATE RESTRICT ON DELETE RESTRICT,
       CONSTRAINT chk_panel_is_online CHECK (is_online IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE circuit (
     circuit_id BIGINT NOT NULL AUTO_INCREMENT,
     panel_id BIGINT NOT NULL,
     channel_no INT NOT NULL,
     load_type VARCHAR(50) NULL,
     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (circuit_id),
     CONSTRAINT uk_circuit_panel_channel UNIQUE (panel_id, channel_no),
     CONSTRAINT fk_circuit_panel FOREIGN KEY (panel_id)
         REFERENCES panel (panel_id)
         ON UPDATE RESTRICT ON DELETE RESTRICT,
     CONSTRAINT chk_circuit_channel_no CHECK (channel_no BETWEEN 1 AND 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sensor_frame (
      frame_id BIGINT NOT NULL AUTO_INCREMENT,
      panel_id BIGINT NOT NULL,
      received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      mode TINYINT NULL,
      volt_v DECIMAL(4,1) NULL,
      leak_ma DECIMAL(4,1) NULL,
      temperature DECIMAL(4,1) NULL,
      humidity DECIMAL(4,1) NULL,
      fire_raw INT NULL,
      gas_raw INT NULL,
      error_bits VARCHAR(20) NULL,
      door_status TINYINT(1) NULL,
      total_current DECIMAL(4,1) NULL,
      total_power INT NULL,
      PRIMARY KEY (frame_id),
      CONSTRAINT fk_sensor_frame_panel FOREIGN KEY (panel_id)
          REFERENCES panel (panel_id)
          ON UPDATE RESTRICT ON DELETE RESTRICT,
      CONSTRAINT chk_sensor_frame_door_status CHECK (door_status IS NULL OR door_status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sensor_frame_circuit (
      reading_id BIGINT NOT NULL AUTO_INCREMENT,
      frame_id BIGINT NOT NULL,
      circuit_id BIGINT NOT NULL,
      current_a DECIMAL(6,3) NOT NULL,
      arc_counter INT NOT NULL DEFAULT 0,
      device_arc_flag TINYINT(1) NOT NULL DEFAULT 0,
      predicted_next_current DECIMAL(6,3) NULL,
      PRIMARY KEY (reading_id),
      CONSTRAINT uk_sensor_frame_circuit UNIQUE (frame_id, circuit_id),
      CONSTRAINT fk_sensor_frame_circuit_frame FOREIGN KEY (frame_id)
          REFERENCES sensor_frame (frame_id)
          ON UPDATE RESTRICT ON DELETE CASCADE,
      CONSTRAINT fk_sensor_frame_circuit_circuit FOREIGN KEY (circuit_id)
          REFERENCES circuit (circuit_id)
          ON UPDATE RESTRICT ON DELETE RESTRICT,
      CONSTRAINT chk_sensor_frame_circuit_arc_counter CHECK (arc_counter >= 0),
      CONSTRAINT chk_sensor_frame_circuit_device_arc_flag CHECK (device_arc_flag IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_diagnosis_result (
         result_id BIGINT NOT NULL AUTO_INCREMENT,
         circuit_id BIGINT NOT NULL,
         frame_id BIGINT NULL,
         verdict ENUM('NORMAL', 'ARC') NOT NULL,
         confidence FLOAT NULL,
         diagnosed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
         PRIMARY KEY (result_id),
         CONSTRAINT fk_ai_diagnosis_result_circuit FOREIGN KEY (circuit_id)
             REFERENCES circuit (circuit_id)
             ON UPDATE RESTRICT ON DELETE RESTRICT,
         CONSTRAINT fk_ai_diagnosis_result_frame FOREIGN KEY (frame_id)
             REFERENCES sensor_frame (frame_id)
             ON UPDATE RESTRICT ON DELETE SET NULL,
         CONSTRAINT chk_ai_diagnosis_result_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE alert (
       alert_id BIGINT NOT NULL AUTO_INCREMENT,
       circuit_id BIGINT NULL,
       panel_id BIGINT NULL,
       source ENUM('DEVICE', 'AI', 'SYSTEM') NOT NULL,
       type ENUM('ARC', 'OVERHEAT', 'LEAKAGE', 'OVERCURRENT', 'COMM_LOST') NOT NULL,
       result_id BIGINT NULL,
       status ENUM('UNCONFIRMED', 'CONFIRMED', 'RESOLVED') NOT NULL DEFAULT 'UNCONFIRMED',
       triggered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
       confirmed_by BIGINT NULL,
       confirmed_at DATETIME NULL,
       resolved_at DATETIME NULL,
       PRIMARY KEY (alert_id),
       CONSTRAINT fk_alert_circuit FOREIGN KEY (circuit_id)
           REFERENCES circuit (circuit_id)
           ON UPDATE RESTRICT ON DELETE RESTRICT,
       CONSTRAINT fk_alert_panel FOREIGN KEY (panel_id)
           REFERENCES panel (panel_id)
           ON UPDATE RESTRICT ON DELETE RESTRICT,
       CONSTRAINT fk_alert_result FOREIGN KEY (result_id)
           REFERENCES ai_diagnosis_result (result_id)
           ON UPDATE RESTRICT ON DELETE SET NULL,
       CONSTRAINT fk_alert_confirmed_by FOREIGN KEY (confirmed_by)
           REFERENCES `user` (user_id)
           ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report (
        report_id BIGINT NOT NULL AUTO_INCREMENT,
        type VARCHAR(50) NOT NULL,
        period_start DATE NULL,
        period_end DATE NULL,
        file_path VARCHAR(255) NULL,
        generated_by BIGINT NOT NULL,
        generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (report_id),
        CONSTRAINT fk_report_generated_by FOREIGN KEY (generated_by)
            REFERENCES `user` (user_id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
        CONSTRAINT chk_report_period CHECK (
            period_start IS NULL OR period_end IS NULL OR period_start <= period_end
            )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주요 조회 경로 인덱스
CREATE INDEX idx_user_site_site_id ON user_site (site_id);
CREATE INDEX idx_panel_site_status ON panel (site_id, status);
CREATE INDEX idx_panel_last_communicated_at ON panel (last_communicated_at);
CREATE INDEX idx_circuit_panel_id ON circuit (panel_id);
CREATE INDEX idx_sensor_frame_panel_received ON sensor_frame (panel_id, received_at);
CREATE INDEX idx_sensor_frame_circuit_circuit_id ON sensor_frame_circuit (circuit_id);
CREATE INDEX idx_ai_diagnosis_circuit_diagnosed ON ai_diagnosis_result (circuit_id, diagnosed_at);
CREATE INDEX idx_alert_status_triggered ON alert (status, triggered_at);
CREATE INDEX idx_alert_type_triggered ON alert (type, triggered_at);
CREATE INDEX idx_alert_panel_triggered ON alert (panel_id, triggered_at);
CREATE INDEX idx_alert_circuit_triggered ON alert (circuit_id, triggered_at);
CREATE INDEX idx_report_generated_at ON report (generated_at);