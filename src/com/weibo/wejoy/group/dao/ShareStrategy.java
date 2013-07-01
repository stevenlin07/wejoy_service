package com.weibo.wejoy.group.dao;

import cn.sina.api.data.dao.util.JdbcTemplate;

public interface ShareStrategy {	
	
	String getDBName(long id);
	
	String getTableSuffix(long id);
	
	JdbcTemplate getIdxJdbcTemplate(long id);
}
