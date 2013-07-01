package com.weibo.wejoy;

import java.util.HashSet;

import cn.sina.api.commons.util.ApiLogger;
import cn.sina.api.commons.util.JsonWrapper;

import com.weibo.wejoy.firehose.client.FirehoseClient;
import com.weibo.wejoy.service.WeJoyServiceOpType;
import com.weibo.wejoy.service.WejoyGroupService;
import com.weibo.wejoy.service.WejoyService;
import com.weibo.wejoy.service.scheduled.RecommnService;
import com.weibo.wejoy.service.scheduled.UpdateMc;  
import com.weibo.wejoy.wesync.WesyncApiImpl;
import com.weibo.wejoy.wesync.listener.GroupChatService;


public enum WeijuInstance {
	
	INSTANCE; 
	
    public static WeijuInstance getInstance(GroupChatService groupChatService){  
    	WesyncApiImpl.getInstance().initWesyncInstance(groupChatService);
    	FirehoseClient.start();//firehose 模块业务
    	RecommnService.INSTANCE.start();// 定向推荐模块业务
    	UpdateMc.INSTANCE.start(); // 针对注册用户业务 ，DB重写mc
    	//GroupManagerService.INSTANCE.start();//群管理模块业务
    	return INSTANCE;
    }
    
	/**
	 * 
	 * getChatInfo接口
	 * 需要的json串 {key:"chatInfo", gid:"G$4342$43"}
	 * gid:群聊的id，调用这个接口会得到展示这个聊天群所需要的信息
	 * 作用：查询相关群的信息
	 * 
	 * sendWeibo接口
	 * 需要的json串{key:"sendWeibo", text:"小行星撞地球", uids:"1432040901,1659481900",filePath: ”http://xxdxxx”}
	 * text:定向微博的文本内容
	 * listId:分组id,如果此项为空,则用传过来的uids去创建分组
	 * uids:所建的群聊中的成员
	 * metaType：有两种值：pic或null,pic的话需要网关去解析filePath，得到上传图片的二进制流，null的话发表的定向微博只有文本内容
	 * 作用：处理来自微聚客户端的群聊请求
	 * 
	 * register接口   
	 * 需要的json串{key:"register"}
	 * 作用：记录使用微聚客户端的用户的uid
	 */
    
	public String process(String fromuid,String remoteIp,JsonWrapper json,byte[] attach){
		ApiLogger.warn("plugin request :"+json.toString());
		String result = null;
		String key = json.get("key");
		WeJoyServiceOpType doveType = WeJoyServiceOpType.valueOfStr(key);
		if (WeJoyServiceOpType.UNKNOWN == doveType) return paramsEmpty("[key = "+key+"]",null); 
		// 业务处理
		switch (doveType) {
			case chatInfo : {
				String gid = json.get("gid");
				if( null == gid) {
					result = paramsEmpty("[chatInfo][gid]","null");
				} else {
					result = WejoyService.getChatInfo(gid);
				}
				break;
			}
			case createGroupChat : {
				String uids = json.get("uids");
				if(null == uids) return paramsEmpty("[SendWeibo][uids]","null");
				String[] arr = uids.split(",");
				HashSet<String> uidsSet = new HashSet<String>();
				for(String uid:arr) uidsSet.add(uid);
				if(0 == uidsSet.size()) return paramsEmpty("[SendWeibo][uids]","empty");
				
				result = WejoyService.createGroupChat(uidsSet, fromuid);
				break;
			}
			case register :
				
				result = WejoyService.register(fromuid); break;
			case groups: 
				result = WejoyService.groups(fromuid);break;
			case bi_users: {
				String listId = json.get("listId");
				if(null == listId) return paramsEmpty("bi_uses listId", "null");
				result = WejoyService.biUsersInGroup(listId, fromuid);
				break;
			}	
			case groupMembers: {
				String listId = json.get("listId");
				if(null == listId) return paramsEmpty("bi_uses listId", "null");
				result = WejoyService.getGroupMembers(listId, fromuid);
				break;
			} 
			case userInfo: {
				String uid = json.get("uid");
				if(null == uid) return paramsEmpty("[userInfo][uid]", "null");
				result = WejoyService.userInfo( uid, fromuid);
				break;
			}
			case statusInfo: {
				String statusId = json.get("statusId");
				if(null == statusId) return paramsEmpty("statusInfo statusId", "null");
				result = WejoyService.statusInfo(statusId, fromuid);
				break;
			}
			case addGroupMember:{
				result = WejoyGroupService.addGroupMembers(fromuid, json);
				break;
			}
			case delGroupMember:{
				result = WejoyGroupService.removeGroupMembers(fromuid, json);
				break;
			}
			case quitGroup: {
				result = WejoyGroupService.quitGroup(fromuid, json);
				break;
			}
			case chatMembers: {
				String gid = json.get("gid");
				result = WejoyGroupService.chatMembers(fromuid, gid);
				break;
			}
			default:
		}

		ApiLogger.warn("plugin response :"+ result);
		return result;
	}
	
	public static String paramsEmpty(String paramName,String status) {
		String result = "{\"code\":1001,\"text\":\"" + paramName + "is "+status+"!\"}";
		return result;
	}
}

