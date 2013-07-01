package com.weibo.wejoy.service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper;
import cn.sina.api.data.storage.cache.MemCacheStorage;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.dao.RegisterDao;
import com.weibo.wejoy.group.module.DbModule;
import com.weibo.wejoy.service.ProxyService;
import com.weibo.wejoy.service.util.GetIntersection;
import com.weibo.wejoy.service.util.ParseListIds;
import com.weibo.wejoy.service.util.PluginResult;
import com.weibo.wejoy.wesync.WesyncApiImpl;

@SuppressWarnings("unchecked")
public class WejoyService {

	private static WesyncApiImpl wesyncInstance = WesyncApiImpl.getInstance();
	private static List2GroupDao list2GroupDao;
	private static  MemCacheStorage<String> memCacheStorage;
	private static RegisterDao registerDao;
	private static Logger log = Logger.getLogger(WejoyService.class);
	private static final String SYS = "8899";
	private static final int retryTime = 3;
	

	static {
		Injector injector = Guice.createInjector(new DbModule());
		list2GroupDao = injector.getInstance(List2GroupDao.class);
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "firehose.xml" });
		registerDao = (RegisterDao)ctx.getBean("registerDao");
		memCacheStorage =  (MemCacheStorage<String>) ctx.getBean("memCacheStorage");	
	}

	/** 发送评论到微博*/
	public static String sendCommentToWeibo(String gid,String comment,String fromuid) {
		String[] split = gid.split("\\$");
		String createId = split[1];
		if (createId == null ){
			return null;
		}
		long statusId = list2GroupDao.getStatusIdByGroupId(gid, createId);
		ProxyService.createComment(comment, statusId, fromuid);
		return null;
	}
	
	/**
	 * 获取群聊信息 得到这个群聊组所对应的原始微博 
	 */
	public static String getChatInfo(String gid) {
		long statusId = 0;
		long listId = 0;
		int num = 0;
		String fromuid = getFromuid(gid);//从群聊id中获得群主id，为后续查db做准备，分库分表是哈希uid到不同的表中
		String listStatusId = memCacheStorage.get(gid);
		if(null != listStatusId){	
			int index = listStatusId.indexOf(",");
			listId = Long.parseLong(listStatusId.substring(0, index));
			statusId = Long.parseLong(listStatusId.substring(index+1, listStatusId.length()));
		}else{
			try {
				statusId = list2GroupDao.getStatusIdByGroupId(gid, fromuid);
				listId = list2GroupDao.getListIdByGroupId(gid, fromuid);
				} catch (Exception e) {		
					log.error("[getChatInfo] 从db中获取statusId 或 listId 有错误" , e);
				}
		}
	
		if(0 == statusId || 0 == listId){
			return "{\"code\":1001,\"text\":\"[chatInfo] conver gid to statusId or listId failed!\"}";
		}
		
		HashSet<String> members = wesyncInstance.groupMembers(gid);
		if (null == members || 0 == members.size()) {
			log.warn("[getChatInfo] gid:" + gid + "members size:" + members.size());
		} else {
			num = members.size();
		}
		JsonBuilder builder = new JsonBuilder();
		builder.append("key", "chatInfo");
		builder.append("StatusId", statusId);
		builder.append("listId", listId);
		builder.append("num", num);
		return builder.flip().toString();
	}

	/**
	 * 只创建群聊
	 * 
	 * @param pluginResult
	 * @param fromuid
	 * @param attach
	 * @return apprequest failed 1002 wesync failed 1003
	 */
	public static String createGroupChat(HashSet<String> uidsSet, String fromuid) {
		LinkedList<String> uids = new LinkedList<String>();
		uids.addAll(uidsSet);
		//创建群聊
		String gid = wesyncInstance.createGroup(fromuid, uids);
		
		if (null == gid) {
			return "{\"code\":1005,\"text\":\"[sendWeibo] create group failure\"}";
		}

		//给非群主成员发sys-conv-uid消息
		sendMessage2GroupMembers(uidsSet, fromuid, null, gid);
			
		//给群主返信息
		JsonBuilder builder = new JsonBuilder();
		builder.reset();
		builder.append("key", "createGroupChat");
		builder.append("gid", gid);// G$4342$43
		builder.append("num", uidsSet.size()+1);
		return builder.flip().toString();
	}
	
	/**
	 * 发布定向微博并创建群聊
	 * 
	 * @param pluginResult
	 * @param fromuid
	 * @param attach
	 * @return apprequest failed 1002 wesync failed 1003
	 */
	public static String sendWeibo(String text, String listId, HashSet<String> uidsSet,String fromuid, byte[] attach) {
		String statusId = null; 
		if (null == listId) {
			log.warn("[sendWeibo][listId = null]");
			// 首先调用微博接口创建分组
			listId = ProxyService.createGroupWeibo(fromuid);

			if (null == listId) {
				return "{\"code\":1002,\"text\":\"[sendWeibo] createGroupWeibo failure\"}";
			}
			
			boolean result = ProxyService.addMembers2Group(fromuid,listId,uidsSet);
			if(!result){
				return "{\"code\":1003,\"text\":\"[sendWeibo] addMembers2Group failure\"}";
			}
		}
		
		//发布定向微博
		statusId = ProxyService.sendOrientWeibo(fromuid, text,listId,attach);
		if (null == statusId) {
			return "{\"code\":1004,\"text\":\"[sendWeibo] sendOrientStatus failure\"}";
		}
		
		LinkedList<String> uids = new LinkedList<String>();
		uids.addAll(uidsSet);
		//创建群聊
		String gid = wesyncInstance.createGroup(fromuid,uids);
		if (null == gid) {
			return "{\"code\":1005,\"text\":\"[sendWeibo] create group failure\"}";
		}

		// 建立分组id、微博id和群聊id的映射
		boolean mapping = list2GroupDao.saveListId2GroupIdmMapping(Long.valueOf(listId), Long.valueOf(statusId), gid, fromuid);
		if (!mapping) {
			return "{\"code\":1006,\"text\":\"[sendWeibo] mapping gid  and statusId to chatId failure\"}";
		}
		
		//存mc 
		//1.key:gid , value:listId&statusId组合  说明：getChatInfo接口需要从gid查询listId和statusId
		//2.key：listId&statusId组合 , value:gid  说明：定向微博评论需要从listId和statusId得到gid
		//3.key:Mfromuid ,value:fromuid所拥有的群   说明：关系变更需要从fromuid得到它所对应的所有的群聊
		String listStatusId = listId + "," + statusId;
		memCacheStorage.set(gid, listStatusId);
		memCacheStorage.set(listStatusId, gid);
		String mfromuid ="M"+ fromuid;//式样： M1678339034
		String gids = memCacheStorage.get(mfromuid);
		if(null == gids){
			memCacheStorage.set(mfromuid, gid);
		}else{
			memCacheStorage.set(mfromuid, gids +","+gid);		
		}
		
		//给非群主成员发sys-conv-uid消息
		sendMessage2GroupMembers(uidsSet, fromuid, statusId, gid);
			
		//给群主返信息
		JsonBuilder builder = new JsonBuilder();
		builder.reset();
		builder.append("key", "sendWeibo");
		builder.append("gid", gid);// G$4342$43
		builder.append("num", uidsSet.size()+1);
		builder.append("statusId", statusId);
		builder.append("listId", listId);
		return builder.flip().toString();
	}
	
	/**
	 * 记录使用微聚客户端的用户的uid
	 * @param uid
	 * @return
	 * 先写mc retry time为3, 之后写db,retry time为2,写db失败后清除mc中数据，返回'注册失败'信息
	 */
	public static String register(String uid){
		boolean success = false;
		int time = 0;
		//判断是否已经注册过
		String hasRegistered = memCacheStorage.get(uid);
		JsonBuilder builder = new JsonBuilder();
		if(null != hasRegistered ){
			builder.append("key", "register");
			builder.append("message", "registered");//此时林洋忽略此消息，继续下面的业务
			return builder.flip().toString();
		}else{
			//写mc
			while(time < retryTime){
				success = memCacheStorage.set(uid, uid);
				if(success){
					break;
				}
				time ++;
			}
			if(!success){
				log.warn("[PluginService:register] 写mc失败");
				builder.append("key", "register");
				builder.append("result", "failure");
			}else{
				//写db
				success = registerDao.saveRegisterUid(uid);
				if(!success){
					success = registerDao.saveRegisterUid(uid);
				}
			}
			
			if(!success){
				//清除mc中数据，返回注册失败信息
				success = memCacheStorage.delete(uid);
				if(!success){
					success = memCacheStorage.delete(uid);
					if(success){
						builder.append("key", "register");
						builder.append("result", "failure");
					}else{
						log.warn("[PluginService:register] 从mc中清除数据错误");//TODO 优化将此日志打到特殊的地方
					}
				}
				
			}else{
			builder.append("key", "register");
			builder.append("result", "success");
			}		
		}
		
		return builder.flip().toString();
	}
	
	/**
	 * 获得fromuid的所有的分组id和分组名称
	 * @param fromuid
	 * @return
	 */
	public static String groups(String fromuid){
		String result = "{\"code\":2001,\"text\":\"[PluginService] call groups interface failure\"}";
		String info = ProxyService.getGroupsInfo(fromuid);
		if(null != info){
			result = ParseListIds.parstListIds(info);	
		}
		return result;
	}
	
	/**
	 * 获取fromuid的某一分组中与fromuid是双向关注的用户的相关信息
	 * @param listId
	 * @param fromuid
	 * @return
	 */
	public static String biUsersInGroup(String listId, String fromuid){
		String result = "{\"code\":2002,\"text\":\"[biUsersInGroup] get biUsers Info failure\"}";
		HashSet<String> membersId = ProxyService.getMembersId(Long.valueOf(listId), fromuid);
		HashSet<String> bilateralMembers = ProxyService.getBilateralIds(fromuid);
		if(0 != membersId.size() && 0 != bilateralMembers.size()){
			HashSet<String> intersection = GetIntersection.getIntersectionOf2Set(membersId, bilateralMembers);
			if(0 != intersection.size()){
				String info = null;
				String name = null;
				String userId = null;
				String remark = null;
				List<JsonBuilder> list =  new LinkedList<JsonBuilder>();
				for(String uid : intersection){
					info = ProxyService.getUserInfo(fromuid, uid);
					if(null != info){
						try {
							JsonBuilder builder = new JsonBuilder();
							JsonWrapper json = new JsonWrapper(info);
							name = json.get("name");
							userId = json.get("idstr");
							remark = json.get("remark");
							builder.append("name", name);
							builder.append("user_id", userId);
							builder.append("remark", remark);
							builder.flip();
							list.add(builder);
						
						} catch (Exception e) {
							log.warn("[PluginService]  call biUsersInGroup error: " + e);
						}
					}
				}
				JsonBuilder jsonStr = new JsonBuilder();
				jsonStr.append("key", "bi_users");
				jsonStr.appendJsonArr("value", list);
				
				result = jsonStr.flip().toString();
			}
		}
		return result;
	}
	
	/**
	 * 获取fromuid的某一分组中与fromuid是双向关注的用户的相关信息
	 * @param listId
	 * @param fromuid
	 * @return
	 */
	public static String getGroupMembers(String listId, String fromuid){
		String result = "{\"code\":2002,\"text\":\"[biUsersInGroup] get biUsers Info failure\"}";
		
		List<Long> membersId = ProxyService.getMembersIdAsLong(Long.valueOf(listId), fromuid);
		JsonBuilder builder = new JsonBuilder();
		builder.appendLongList("group_member_ids", membersId);
		result = builder.flip().toString();
		
		return result;
	}
	
	/**
	 * 获取指定用户的信息
	 * @param uid
	 * @param fromuid
	 * @return
	 */
	public static String userInfo(String uid, String fromuid){
		String result = "{\"code\":2003,\"text\":\"[userInfo] get users Info failure\"}";
		String userInfo = ProxyService.getUserInfo(fromuid, uid);
		if(null != userInfo){
			JsonWrapper json;
			try {
				json = new JsonWrapper(userInfo);
				JsonBuilder builder1 = new JsonBuilder();
				builder1.append("user_id", json.get("idstr"));
				builder1.append("name", json.get("name"));
				builder1.append("remark", json.get("remark"));
				builder1.append("profile_image_url", json.get("profile_image_url"));
				builder1.append("description", json.get("description"));
				
				JsonBuilder builder2 = new JsonBuilder();
				builder2.append("key", "userInfo");
				builder2.append("value", builder1.flip());
				
				result = builder2.flip().toString();
						
			} catch (Exception e) {
				log.warn("[PluginService] call userInfo interface error: " + e);
			}
			
		}
		return result;
	}
	
	public static String statusInfo(String statusId, String fromuid){
		String result = "{\"code\":2004,\"text\":\"[statusInfo] get status Info failure\"}";
		String statusInfo = ProxyService.getStatusInfo(statusId, fromuid);
		if(null != statusInfo){
			try {
				JsonWrapper json = new JsonWrapper(statusInfo);
				JsonBuilder builder1 = new JsonBuilder();
				builder1.append("status_id", json.get("idstr"));
				builder1.append("text", json.get("text"));
				builder1.append("created_at", json.get("created_at"));
				builder1.append("creator_uid", json.getNode("user").get("idstr"));
				builder1.append("list_id", json.getNode("visible").get("list_id"));
				
				if(null != json.get("original_pic")){
					builder1.append("original_pic", json.get("original_pic"));
				}else{
					
				}
				
				JsonBuilder builder2 = new JsonBuilder();
				builder2.append("key", "statusInfo");
				builder2.append("value", builder1.flip());
				
				result = builder2.flip().toString();
				
			} catch (Exception e) {
				log.warn("[PluginService] call statusInfo interface error: " + e);
			}
			
		}
		return result;
	}

	/**
	 * 获取现在所有群聊的组,暂时不用
	 * 
	 * @param fromuid
	 * @return
	 */
	public static String getAllChatGroups(String fromuid) {
		List<String> chatGroups = list2GroupDao.getGroupIdsByUid(fromuid);
		Iterator<String> it = chatGroups.iterator();
		String ids = idsForJsonString(it);
		int total = chatGroups.size();
		JsonBuilder json = new JsonBuilder();
		json.append("ids", ids);
		json.append("total", total);
		return json.flip().toString();
	}
	
	public static String idsForJsonString(Iterator<String> it){
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		boolean first = true;
		while(it.hasNext()){
			if(first){
			builder.append("\'").append(it.next()).append("\'");
			first = false;
			}
			builder.append(",").append("\'").append(it.next()).append("\'");
		} 
		builder.append("]");
		return builder.toString();
	}
	
	//给非群主成员发消息
	public static void sendMessage2GroupMembers(HashSet<String> uidsSet, String fromuid, 
		String statusId, String gid)
	{
		JsonBuilder builder = new JsonBuilder();
		int num = uidsSet.size();
		String message = String.format("%s邀请您加入此聊天群", fromuid);
		builder.append("key", "newGroup");
		builder.append("message", message);
		builder.append("gid", gid);
		builder.append("statusId", statusId);
		builder.append("num", num);
		String content = builder.flip().toString();
		
		for(String uid : uidsSet){	
			wesyncInstance.sendToUser(SYS, uid, content);
		}
	}
	
	private static String getFromuid(String gid){
		int index1 = gid.indexOf("$");
		String back = gid.substring(index1 + 1, gid.length());
		int index2 = back.indexOf("$");
		String fromuid = back.substring(0, index2); 
		return fromuid;
	}
	
}
