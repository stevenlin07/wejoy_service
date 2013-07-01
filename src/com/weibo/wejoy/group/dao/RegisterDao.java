package com.weibo.wejoy.group.dao;

public interface RegisterDao {
	
	/**
	 * 存使用了微聚用户的id
	 * @param uid
	 * @return
	 */
	public boolean saveRegisterUid(String uid);
	
	/**
	 * 获取使用微聚客户的id
	 * @param uid
	 * @return
	 */
	
	public String getUid(String uid);
	
	
	/**
	 * 判断是否是注册用户
	 * @param uid
	 * @return
	 */
	public  boolean isRegistered(String uid);

}
