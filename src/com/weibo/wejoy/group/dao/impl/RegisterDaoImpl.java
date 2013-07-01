package com.weibo.wejoy.group.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.weibo.wejoy.group.dao.RegisterDao;

import cn.sina.api.commons.util.ApiLogger;
import cn.sina.api.data.Constants;
import cn.sina.api.data.dao.util.JdbcTemplate;
import cn.sina.api.data.storage.cache.MemCacheStorage;

@SuppressWarnings("unchecked")
class RegisterDaoImpl implements RegisterDao{
	
	private static Logger log = Logger.getLogger(RegisterDaoImpl.class);
	private static  MemCacheStorage<String> memCacheStorage;
	
	 static {
			ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"firehose.xml"});
			memCacheStorage =  (MemCacheStorage<String>) ctx.getBean("memCacheStorage");
		}
	
	@Override
	public boolean saveRegisterUid(String uid){
		try {
			long t1 = System.currentTimeMillis();
			
			String sql = SAVE_REGISTER_UID;
			int result = tempJdbc.update(sql,new Object[] {uid});

			if (ApiLogger.isDebugEnabled()) {
				log.debug("RegisterDaoImpl saveRegisterUid :uid=" + uid);
			}

			long t2 = System.currentTimeMillis();
			if (t2 - t1 > Constants.OP_DB_TIMEOUT) {
				long t = t2 - t1;
				log.warn("RegisterDaoImpl saveRegisterUid too slow : t=" + t + " ,uid= " + uid);
			}
			
			return result > 0;
		} catch (RuntimeException e) {
			log.error("RegisterDaoImpl saveRegisterUid error: uid=" + uid);
			throw e;
		}
	}
	
	@Override
	public String getUid(String uid) {
		String sql = GET_REGISTER_UID;
		String registerUid = (String) tempJdbc.query(sql, new Object[]{uid}, new ResultSetExtractor<String>(){

			@Override
			public String extractData(ResultSet rs) throws SQLException,
					DataAccessException {
				if(rs.next()){
					return rs.getString("uid"); 
				}
				return null;
			}
			
		});
		
		return registerUid;
	}
	
	
	//TODO优化
	@Override
	public  boolean isRegistered(String uid){
		String result = memCacheStorage.get(uid);
		if(null == result){
			return false;
		}else{
			return true;
		}
	}
	
	@Resource(name="weijuJdbc")
	private JdbcTemplate tempJdbc;
	
	public void setTempJdbc (JdbcTemplate tempJdbc){
		this.tempJdbc = tempJdbc;
	}
	
	// 保存定向分组和群聊映射关系
		private final String SAVE_REGISTER_UID = "insert ignore into register_weiju.register_weiju_1(uid,time) values (?,now())";
		// 根据定向分组id和定向微博id获取群聊id
		private final String GET_REGISTER_UID = "select uid from register_weiju.register_weiju_1  where uid=?";

}
