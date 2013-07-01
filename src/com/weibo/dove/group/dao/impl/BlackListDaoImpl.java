package com.weibo.dove.group.dao.impl;

import java.util.Set;

import org.apache.log4j.Logger;

import redis.clients.jedis.Tuple;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weibo.wejoy.data.storage.RedisStorage;

public class BlackListDaoImpl {
	private Logger log = Logger.getLogger("notify_service");
	public RedisStorage offlineNoticeBlackList;
	
	public static enum BlackConvType {
		GROUP("G");
		
		private String suffix;
		
		private BlackConvType(String suffix) {
			this.suffix = suffix;
		}
		
		public String getSuffix() {
			return "$" + suffix;
		}
	}
	
	public boolean isSenderIdInOfflineNoticeBlackList(String reciverId, String senderId, BlackConvType type){
		if(BlackConvType.GROUP.equals(type)){
			senderId = this.getGroupId4StorageValue(senderId);	//special operation for group id
		}
		
		String key = reciverId + type.getSuffix();
		
		Long ret = offlineNoticeBlackList.hget(key, reciverId);
		return (ret != null && ret != -1);
	}
	
	/*
	public boolean addUidToOfflineNoticeBlackList(String reciverId, String senderId, String type){
		if(Constants.OFFLINE_NOTICE_BLACK_LIST_GROUP_TYPE.equals(type)){
			senderId = this.getGroupId4StorageValue(senderId);	//special operation for group id
		}
		
		String key = reciverId + Constants.OFFLINE_NOTICE_BLACK_LIST_SUFFIX;
		
		Long ret = offlineNoticeBlackList.zadd(key, Constants.OFFLINE_NOTICE_BALCK_LIST_SCORE, senderId);
		return (ret != null && ret != -1 && ret != 0);	//return 0 -> element duplicate
	}
	
	public boolean delIdInOfflineNoticeBlackList(String reciverId, String senderId, String type){
		if(Constants.OFFLINE_NOTICE_BLACK_LIST_GROUP_TYPE.equals(type)){
			senderId = this.getGroupId4StorageValue(senderId);	//special operation for group id
		}
		
		String key = reciverId + Constants.OFFLINE_NOTICE_BLACK_LIST_SUFFIX;
		
		Long ret = offlineNoticeBlackList.zrem(key, senderId);
		return (ret != null && ret != -1 && ret != 0);	//return 0 -> non-existent element
	}
	
	public String getOfflineNoticeBlackList(String reciverId){
		String key = reciverId + Constants.OFFLINE_NOTICE_BLACK_LIST_SUFFIX;
		long count = offlineNoticeBlackList.zcard(key);
		
		StringBuilder sb = new StringBuilder();
		Set<Tuple> ret = offlineNoticeBlackList.zrangeWithScores(key, 0, (int)count);
		for (Tuple tuple : ret) {
			sb.append(tuple.getElement()).append(",");
		}
		
		return sb.toString();
	}
	*/
	private String getGroupId4StorageValue(String gid){
		return "g" + gid;
	}
	
	public static void main(String[] args) {
	}

}