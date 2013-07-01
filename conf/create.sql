CREATE TABLE `list_to_group_1` (
  `list_id` bigint(20) NOT NULL DEFAULT '0',
  `status_id` bigint(20) NOT NULL DEFAULT '0',
  `gid` varchar(48) DEFAULT NULL,
  `creator` varchar(24) DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `lastComment_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`list_id`,`status_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1