CREATE TABLE cqrs.`event_offset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_offset_id` bigint NOT NULL,
  `data_source_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `table_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;


CREATE TABLE cqrs2.`event_offset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_offset_id` bigint NOT NULL,
  `data_source_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `table_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;



INSERT INTO `cqrs`.`event_offset`(`id`, `event_offset_id`, `data_source_name`, `table_name`) VALUES (1, 0, 'ds0', 'event_stream_0');
INSERT INTO `cqrs`.`event_offset`(`id`, `event_offset_id`, `data_source_name`, `table_name`) VALUES (2, 0, 'ds0', 'event_stream_1');


INSERT INTO `cqrs2`.`event_offset`(`id`, `event_offset_id`, `data_source_name`, `table_name`) VALUES (1, 0, 'ds1', 'event_stream_0');
INSERT INTO `cqrs2`.`event_offset`(`id`, `event_offset_id`, `data_source_name`, `table_name`) VALUES (2, 0, 'ds1', 'event_stream_1');


CREATE TABLE cqrs.`event_stream_0` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_root_type_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `aggregate_root_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `version` int NOT NULL,
  `command_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `gmt_create` datetime NOT NULL,
  `events` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aggregate_id_command_id` (`aggregate_root_id`,`command_id`) USING BTREE,
  UNIQUE KEY `uk_aggregate_id_version` (`aggregate_root_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;



CREATE TABLE cqrs.`event_stream_1` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_root_type_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `aggregate_root_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `version` int NOT NULL,
  `command_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `gmt_create` datetime NOT NULL,
  `events` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aggregate_id_command_id` (`aggregate_root_id`,`command_id`) USING BTREE,
  UNIQUE KEY `uk_aggregate_id_version` (`aggregate_root_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;


CREATE TABLE cqrs.`event_stream_2` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_root_type_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `aggregate_root_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `version` int NOT NULL,
  `command_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `gmt_create` datetime NOT NULL,
  `events` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aggregate_id_command_id` (`aggregate_root_id`,`command_id`) USING BTREE,
  UNIQUE KEY `uk_aggregate_id_version` (`aggregate_root_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;



CREATE TABLE cqrs.`event_stream_3` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_root_type_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `aggregate_root_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `version` int NOT NULL,
  `command_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `gmt_create` datetime NOT NULL,
  `events` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aggregate_id_command_id` (`aggregate_root_id`,`command_id`) USING BTREE,
  UNIQUE KEY `uk_aggregate_id_version` (`aggregate_root_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;





CREATE TABLE cqrs2.`event_stream_0` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_root_type_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `aggregate_root_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `version` int NOT NULL,
  `command_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `gmt_create` datetime NOT NULL,
  `events` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aggregate_id_command_id` (`aggregate_root_id`,`command_id`) USING BTREE,
  UNIQUE KEY `uk_aggregate_id_version` (`aggregate_root_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;



CREATE TABLE cqrs2.`event_stream_1` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_root_type_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `aggregate_root_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `version` int NOT NULL,
  `command_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `gmt_create` datetime NOT NULL,
  `events` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aggregate_id_command_id` (`aggregate_root_id`,`command_id`) USING BTREE,
  UNIQUE KEY `uk_aggregate_id_version` (`aggregate_root_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;


CREATE TABLE cqrs2.`event_stream_2` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_root_type_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `aggregate_root_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `version` int NOT NULL,
  `command_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `gmt_create` datetime NOT NULL,
  `events` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aggregate_id_command_id` (`aggregate_root_id`,`command_id`) USING BTREE,
  UNIQUE KEY `uk_aggregate_id_version` (`aggregate_root_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;



CREATE TABLE cqrs2.`event_stream_3` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_root_type_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `aggregate_root_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `version` int NOT NULL,
  `command_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `gmt_create` datetime NOT NULL,
  `events` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aggregate_id_command_id` (`aggregate_root_id`,`command_id`) USING BTREE,
  UNIQUE KEY `uk_aggregate_id_version` (`aggregate_root_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;