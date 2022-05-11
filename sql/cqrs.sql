CREATE TABLE `event_offset`
(
    `id`               bigint                                                        NOT NULL AUTO_INCREMENT,
    `event_offset_id`  bigint                                                        NOT NULL,
    `data_source_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    `table_name`       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE `event_stream_0`
(
    `id`                       bigint                                                NOT NULL AUTO_INCREMENT,
    `aggregate_root_type_name` varchar(256) COLLATE utf8mb4_bin                      NOT NULL,
    `aggregate_root_id`        varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `version`                  int                                                   NOT NULL,
    `command_id`               varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `gmt_create`               datetime                                              NOT NULL,
    `events`                   mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_aggregate_id_command_id` (`aggregate_root_id`,`command_id`) USING BTREE,
    UNIQUE KEY `uk_aggregate_id_version` (`aggregate_root_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE `event_stream_1`
(
    `id`                       bigint                                                NOT NULL AUTO_INCREMENT,
    `aggregate_root_type_name` varchar(256) COLLATE utf8mb4_bin                      NOT NULL,
    `aggregate_root_id`        varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `version`                  int                                                   NOT NULL,
    `command_id`               varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `gmt_create`               datetime                                              NOT NULL,
    `events`                   mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_aggregate_id_command_id` (`aggregate_root_id`,`command_id`) USING BTREE,
    UNIQUE KEY `uk_aggregate_id_version` (`aggregate_root_id`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;


INSERT INTO `event_offset`(`id`, `event_offset_id`, `data_source_name`, `table_name`)
VALUES (1, 0, 'ds0', 'event_stream_0');
INSERT INTO `event_offset`(`id`, `event_offset_id`, `data_source_name`, `table_name`)
VALUES (2, 0, 'ds0', 'event_stream_1');