package com.weibo.wejoy.group.dao.impl;

import java.util.Map;

import com.weibo.wejoy.group.dao.ShareStrategy;

import cn.sina.api.data.dao.util.JdbcTemplate;

public abstract class AbstractShareStrategy implements ShareStrategy{

	/**
	 * 获取分库的JdbcTemplate
	 * @param id	ID Hash策略
	 * @return	JdbcTemplate
	 */
	@Override
	public JdbcTemplate getIdxJdbcTemplate(long id){
		String dbname = getDBName(id);
		JdbcTemplate jt = jts.get(dbname);
		
		if(jt != null){
			return jt;
		}
		throw new IllegalArgumentException("Bad dbhost in ClusterDatabases.getIdxJdbcTemplate, dbname=" + dbname);
	}
	
	
	public JdbcTemplate getIdxJdbcTemplateByTable(long id) {
		throw new RuntimeException("don't support method getIdxJdbcTemplateByTable");
	}
	
	public void setShareTableCount(int shareTableCount) {
		this.shareTableCount = shareTableCount;
	}

	public void setShareDBPrefix(String shareDBPrefix) {
		this.shareDBPrefix = shareDBPrefix;
	}
	
	public void setShareDBCount(int shareDBCount) {
		this.shareDBCount = shareDBCount;
	}
	
	public void setJts(Map<String, JdbcTemplate> jts) {
		this.jts =jts;
	}

	protected String shareDBPrefix;
	protected int shareDBCount;
	protected int shareTableCount;
	protected Map<String, JdbcTemplate> jts;

}
