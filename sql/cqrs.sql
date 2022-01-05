CREATE TABLE `event_stream`
(
    `id`                       bigint(20) NOT NULL AUTO_INCREMENT,
    `aggregate_root_type_name` varchar(256) NOT NULL,
    `aggregate_root_id`        varchar(36)  NOT NULL,
    `version`                  int(11) NOT NULL,
    `command_id`               varchar(36)  NOT NULL,
    `gmt_create`               datetime     NOT NULL,
    `events`                   mediumtext   NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_aggregate_root_id_version` (`aggregate_root_id`,`version`),
    UNIQUE KEY `uk_aggregate_root_id_command_id` (`aggregate_root_id`,`command_id`)
) ENGINE=InnoDB AUTO_INCREMENT=308242 DEFAULT CHARSET=utf8mb4;


CREATE TABLE `event_offset`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT,
    `event_offset_id` bigint(20) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;