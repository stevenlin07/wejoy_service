package com.weibo.wejoy.group.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import cn.sina.api.commons.util.ApiLogger;
import cn.sina.api.data.Constants;
import cn.sina.api.data.dao.util.JdbcTemplate;

import com.weibo.wejoy.group.constants.DoveConstants;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.dao.ShareStrategy;

public class OriList2GroupDaoImpl implements List2GroupDao{
	
	private static final int tableNum = 4; 
	private JdbcTemplate jdbc;
	{
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"firehose.xml"});
		jdbc = (JdbcTemplate) ctx.getBean("weijuJdbc");
	}
	
	/**
	 * 得到注册用户的uid
	 * @return
	 */
	@Override
	public List<String> getUid(){
		long t1 = System.currentTimeMillis();
		final List<String> uidList = new ArrayList<String>(); 
		String sql = GET_REGISTER_UID;
		jdbc.query(sql, new ResultSetExtractor<String>() {
			public String extractData(ResultSet rs) throws SQLException, DataAccessException {
				while(rs.next()) {
					uidList.add(rs.getString("uid"));
				}
				return null;
			}
		});
		
		long t2 = System.currentTimeMillis();
		if(t2 - t1 > 1000){//TODO优化找一个合适的时间
			long t = t2 - t1;
			ApiLogger.warn(new StringBuilder(128).append("RecommnApiImpl getUid too slow, t=").append(t));
		}
		
		return uidList;
	}
	
	
	/**
	 * 建立分组id和群聊id的映射
	 *  
	 * @param uid 分组所有者uid
	 * @param listId 定向分组id
	 * @param statusId 定向微博id
	 * @param groupId 群聊gid
	 * @return
	 */
	@Override
	public boolean saveListId2GroupIdmMapping(long listId, long statusId, String groupId, String uid){
		try {
			long t1 = System.currentTimeMillis();

			String sql = replaceSql(SAVE_LIST_TO_GROUP_MAPPING, Long.parseLong(uid), DoveConstants.LIST_TO_GROUP_MAPPING);

			int result = getJt(Long.parseLong(uid), DoveConstants.LIST_TO_GROUP_MAPPING).update(sql,
					new Object[] {listId, statusId, groupId, uid, groupId, uid});

			if (ApiLogger.isDebugEnabled()) {
				ApiLogger.debug("List2GroupDaoImpl saveListId2GroupIdmMapping :result=" + result + " ,listId=" + listId + " ,statusId=" + statusId +  " ,groupId=" + groupId + " ,uid=" + uid);
			}

			long t2 = System.currentTimeMillis();
			if (t2 - t1 > Constants.OP_DB_TIMEOUT) {
				long t = t2 - t1;
				ApiLogger.warn("List2GroupDaoImpl saveListId2GroupIdmMapping too slow, t=" + t + " ,listId=" + listId + " ,statusId=" + statusId +  " ,groupId=" + groupId + " ,uid=" + uid);
			}
			
			return true;
		} catch (RuntimeException e) {
			ApiLogger.error("List2GroupDaoImpl saveListId2GroupIdmMapping error: uid=" + uid + " ,listId=" + listId + " ,statusId=" + statusId +  " ,groupId=" + groupId + " ,uid=" + uid);
			System.out.println(e);
			throw e;
		}
	}
	
	

	/**
	 * 根据分组id和定向微博id得到群聊id
	 * 
	 * @param listId 定向分组id
	 * @param statusId 定向微博id
	 * @return
	 */
	@Override
	public String getGroupId(long listId, long statusId, String uid){
		long t1 = System.currentTimeMillis();
		
		String sql = replaceSql(GET_GROUPID_BY_LISTANDSTATUS, Long.parseLong(uid), DoveConstants.LIST_TO_GROUP_MAPPING);
		
		@SuppressWarnings("rawtypes")
		String groupId = (String) getJt(Long.parseLong(uid), DoveConstants.LIST_TO_GROUP_MAPPING).query(sql, new Object[] { listId, statusId }, new ResultSetExtractor() {
			public String extractData(ResultSet rs) throws SQLException, DataAccessException {
				if (rs.next()) {
					return rs.getString("gid");
				}
				return null;
			}
		});
		
		long t2 = System.currentTimeMillis();
		if(t2 - t1 > Constants.OP_DB_TIMEOUT){
			long t = t2 - t1;
			ApiLogger.warn(new StringBuilder(128).append("List2GroupDaoImpl getGroupId too slow, t=").append(t).append(", listId=").append(listId).append(", statudId=").append(statusId).toString());
		}
		
		return groupId;
	}

	/**
	 * 根据分组id获得对应群聊id列表
	 * 
	 * @param listId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<String> getGroupIdsByListId(long listId, String uid){
		long t1 = System.currentTimeMillis();
		
		final List<String> list = new ArrayList<String>();
		
		String sql = replaceSql(GET_GROUPIDS_BY_LISTID, Long.parseLong(uid), DoveConstants.LIST_TO_GROUP_MAPPING);
		
		getJt(listId, DoveConstants.LIST_TO_GROUP_MAPPING).query(sql, new Object[] { listId }, new RowMapper() {
			@Override
			public Object mapRow(ResultSet rs, int arg1) throws SQLException {
				String groupId = rs.getString("gid");

				list.add(groupId);
				return null;
			}
		});
		
		long t2 = System.currentTimeMillis();
		if(t2 - t1 > Constants.OP_DB_TIMEOUT){
			long t = t2 - t1;
			ApiLogger.warn(new StringBuilder(128).append("List2GroupDaoImpl getGroupIdsByListId too slow, t=").append(t).append(", listId=").append(listId).toString());
		}
		
		return list;
	}

	/**
	 * 获取uid所在群聊id列表
	 * 
	 * @param uid
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<String> getGroupIdsByUid(String uid){
		long t1 = System.currentTimeMillis();
		
		final List<String> list = new ArrayList<String>();
		
		String sql = replaceSql(GET_GROUPIDS_BY_UID, Long.valueOf(uid), DoveConstants.LIST_TO_GROUP_MAPPING);
		
		getJt(Long.valueOf(uid), DoveConstants.LIST_TO_GROUP_MAPPING).query(sql, new Object[] { uid }, new RowMapper() {
			@Override
			public Object mapRow(ResultSet rs, int arg1) throws SQLException {
				String groupId = rs.getString("gid");

				list.add(groupId);
				return null;
			}
		});
		
		long t2 = System.currentTimeMillis();
		if(t2 - t1 > Constants.OP_DB_TIMEOUT){
			long t = t2 - t1;
			ApiLogger.warn(new StringBuilder(128).append("List2GroupDaoImpl getGroupIdsByUid too slow, t=").append(t).append(", uid=").append(uid).toString());
		}
		
		return list;
	}
	
	@Override
	public long getStatusIdByGroupId(String groupId, String uid) {
		String sql = replaceSql(GET_STATUSID_BY_GID, Long.valueOf(uid), DoveConstants.LIST_TO_GROUP_MAPPING);
		long status_id = (Long) getJt(Long.valueOf(uid), DoveConstants.LIST_TO_GROUP_MAPPING).query(sql, new Object[]{groupId}, new ResultSetExtractor<Long>(){

			@Override
			public Long extractData(ResultSet rs) throws SQLException,
					DataAccessException {
				if(rs.next()){
					return rs.getLong("status_id"); 
				}
				return 0l;
			}
			
		});
		return status_id;
	}
	
	@Override
	public long getListIdByGroupId(String groupId, String uid) {
		String sql = replaceSql(GET_LISTID_BY_GID, Long.valueOf(uid), DoveConstants.LIST_TO_GROUP_MAPPING);
		long list_id = (Long) getJt(Long.valueOf(uid), DoveConstants.LIST_TO_GROUP_MAPPING).query(sql, new Object[]{groupId}, new ResultSetExtractor<Long>(){

			@Override
			public Long extractData(ResultSet rs) throws SQLException,
					DataAccessException {
				if(rs.next()){
					return rs.getLong("list_id"); 
				}
				return 0l;
			}
			
		});
		return list_id;
	}
	
	@Override
	public boolean updataCommentTime(String gid, String uid) {
		try{
			long t1 = System.currentTimeMillis();
			String sql = replaceSql(UPDATE_COMMENT_TIME, Long.valueOf(uid), DoveConstants.LIST_TO_GROUP_MAPPING);
			int result = getJt(Long.valueOf(uid), DoveConstants.LIST_TO_GROUP_MAPPING).update(sql,new Object[] {gid});
			long t2 = System.currentTimeMillis();
			if (ApiLogger.isDebugEnabled()) {
				ApiLogger.debug("List2GroupDaoImpl updataCommentTime :result=" + result + " ,gid=" + gid );
			}
			
			if (t2 - t1 > Constants.OP_DB_TIMEOUT) {
				long t = t2 - t1;
				ApiLogger.warn("List2GroupDaoImpl updataCommentTime too slow, t=" + t + " ,gid=" + gid );
			}
			
			return result > 0;
			
		} catch (RuntimeException e) {
			ApiLogger.error("List2GroupDaoImpl updataCommentTime error: gid=" + gid);
			throw e;
		}
	}
	
	@Override
	public boolean deleteNotActiveGidsInDb(String gid, String uid) {
		try{
			long t1 = System.currentTimeMillis();
			String sql = replaceSql(DELETE_NOTACTIVE_GIDS, Long.valueOf(uid), DoveConstants.LIST_TO_GROUP_MAPPING);
			int result = getJt(Long.valueOf(uid), DoveConstants.LIST_TO_GROUP_MAPPING).update(sql,new Object[] {gid});
			long t2 = System.currentTimeMillis();
			if (ApiLogger.isDebugEnabled()) {
				ApiLogger.debug("List2GroupDaoImpl deleteNotActiveGidsInDb：result=" + result + " ,gid=" + gid );
			}
			
			if (t2 - t1 > Constants.OP_DB_TIMEOUT) {
				long t = t2 - t1;
				ApiLogger.warn("List2GroupDaoImpl deleteNotActiveGidsInDb  too slow, t=" + t + " ,gid=" + gid );
			}
			
			return result > 0;
			
		} catch (RuntimeException e) {
			ApiLogger.error("List2GroupDaoImpl deleteNotActiveGidsInDb error: gid=" + gid);
			throw e;
		}
	}
		
	@Override
	public List<String> getInvalidGids() {
		
		final List<String> invalidGids = new ArrayList<String>(); 
		String sql = GET_INVALID_GIDS;
		for(int i = 1; i<tableNum;i++){
			sql = replaceSql(sql, i);
			jdbc.query(sql, new ResultSetExtractor<String>() {
				public String extractData(ResultSet rs) throws SQLException, DataAccessException {
					while(rs.next()) {
						invalidGids.add(rs.getString("gid"));
					}
					return null;
				}
			});
		}
		return invalidGids;
	}
	
	
	public JdbcTemplate getJt(long id, String type) {
		ShareStrategy strategy = shareStrategys.get(type);
		
		if (strategy == null) return null;
		
		return strategy.getIdxJdbcTemplate(id);
	}
	
	public String replaceSql(String sql, long id, String type) {
		String dbName = getDBName(id, type);
		String tableSuffix = getTableSuffix(id, type);
		return replaceSql(sql,dbName,tableSuffix);
	}
	
	public String replaceSql(String sql, String dbName, String tableSuffix) {
		sql = sql.replace("$db$", dbName);
		sql = sql.replace("$suffix$", tableSuffix);
		
		return sql;
	}
	
	public String replaceSql(String sql, int tableIndex){
		sql = sql.replace("$suffix$", String.valueOf(tableIndex));
		
		return null;
	} 
	
	public String getDBName(long id, String type) {
		ShareStrategy strategy = shareStrategys.get(type);
		
		if (strategy == null) return null;
		
		return strategy.getDBName(id);
	}
	
	public String getTableSuffix(long id, String type) {
		ShareStrategy strategy = shareStrategys.get(type);
		
		if (strategy == null) return null;
		
		return strategy.getTableSuffix(id);
	}
	
	private Map<String, ShareStrategy> shareStrategys;
	
	public void setShareStrategys(Map<String, ShareStrategy> shareStrategys) {
		this.shareStrategys = shareStrategys;
	}
	
	/** dove sql start */
	// 保存定向分组和群聊映射关系
	private final String SAVE_LIST_TO_GROUP_MAPPING = "insert into $db$.list_to_group_$suffix$(list_id,status_id,gid,creator,create_time, lastComment_time) values (?,?,?,?,now(),now()) on duplicate key update gid=?, creator=?, lastComment_time=now()";
	// 根据定向分组id和定向微博id获取群聊id
	private final String GET_GROUPID_BY_LISTANDSTATUS = "select gid from $db$.list_to_group_$suffix$  where list_id=? and status_id=?";
	// 根据定向分组id获取群聊id
	private final String GET_GROUPIDS_BY_LISTID = "select gid from $db$.list_to_group_$suffix$  where list_id=?";
	
	//从群聊id获得所对应的微博id
	private final String GET_STATUSID_BY_GID = "SELECT `status_id` FROM $db$.list_to_group_$suffix$ WHERE `gid`=?";
	//从uid获得这个人所在的所有群聊列表
	private final String GET_GROUPIDS_BY_UID = "select gid from $db$.list_to_group_$suffix$  where creator=?";
	
	//从群聊id获得所对应的微博id
	private final String GET_LISTID_BY_GID = "SELECT `list_id` FROM $db$.list_to_group_$suffix$ WHERE `gid`=?";
	
	//更新最后评论时间
	private final String UPDATE_COMMENT_TIME = "UPDATE  $db$.list_to_group_$suffix$ SET `lastComment_time` = now() WHERE `gid` = ?";
	
	//从db中挑选出不活跃的群id（>30天未聊天）
	private static String  GET_INVALID_GIDS = "SELECT `gid` FROM weiju_1.list_to_group_$suffix$ WHERE TIMESTAMPDIFF(DAY, create_time, lastComment_time) > 30";
	
	//从db中删除不活跃的群id（>30天未聊天）
	private static String DELETE_NOTACTIVE_GIDS = "DELETE FROM $db$.list_to_group_$suffix$ WHERE `gid` = ?";
	
	//得到注册用户uids
	private final String GET_REGISTER_UID = "select uid from register_weiju.register_weiju_1";

	private final String GET_CREATER_BY_GID  = "select uid from FROM $db$.list_to_group_$suffix$ WHERE `gid`=?";
		
	/** dove sql end */

	
}

