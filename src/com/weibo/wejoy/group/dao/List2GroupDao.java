package com.weibo.wejoy.group.dao;

import java.util.List;

public interface List2GroupDao {
	/**
	 * 建立分组id和群聊id的映射
	 *  
	 * @param uid 分组所有者uid
	 * @param listId 定向分组id
	 * @param statusId 定向微博id
	 * @param groupId 群聊gid
	 * @return
	 */
	boolean saveListId2GroupIdmMapping(long listId, long statusId, String groupId, String uid); 

	/**
	 * 根据分组id和定向微博id得到群聊id
	 * 
	 * @param listId 定向分组id
	 * @param statusId 定向微博id
	 * @return
	 */
	String getGroupId(long listId, long statusId, String uid); 

	/**
	 * 根据分组id获得对应群聊id列表
	 * 
	 * @param listId
	 * @return
	 */
	List<String> getGroupIdsByListId(long listId, String uid); 

	/**
	 * 获取uid所在群聊id列表
	 * 
	 * @param uid
	 * @return
	 */
	List<String> getGroupIdsByUid(String uid);
	
	/**
	 * 获取群聊id所对应的定向微博id
	 * @param groupId
	 * @return
	 */
	long getStatusIdByGroupId(String groupId, String uid);
	
	/**
	 * 获取群聊id所对应的分组id
	 * @param groupId
	 * @return
	 */
	long getListIdByGroupId(String groupId, String uid);
	
	
	/**
	 * 更新数据库中的评论时间
	 * @param commentTime
	 * @return
	 */
	boolean updataCommentTime(String gid, String uid);
	
	/**
	 * 从db中删除不活跃的群id
	 * @param gid
	 */
	boolean deleteNotActiveGidsInDb(String gid, String uid);
	
	/**
	 * 从db中获取不活跃的群聊ids(>30天未聊天)
	 * @return
	 */
	List<String> getInvalidGids();//TODO
	
	/**
	 * 得到注册用户的uid
	 * @return
	 */
	List<String> getUid();
 
}

