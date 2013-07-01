package com.weibo.wejoy.service;

import java.util.HashSet;

import org.apache.log4j.Logger;

import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper; 
import com.weibo.wejoy.wesync.WesyncApiImpl;

public class WejoyGroupService {

	
	private static WesyncApiImpl wesyncInstance = WesyncApiImpl.getInstance(); 
	private static Logger log = Logger.getLogger(WejoyService.class);
	private static final String SYS = "8899"; 
	
	public static String errorResult(String errorCause) {
		String result = "{\"error\":1001,\"cause\":\"" + errorCause +"!\"}";
		return result;
	}
	 
	public static String succResult(String key) {
		String result = "{\"code\":200,\"key\":\"" + key +"!\"}";
		return result;
	}
	
	/**
	 * 添加群组成员并向所有成员发送系统通知
	 * 请求串:{"key":"addGroupMember","gid":"G$32Q3$32","members":"uis1,uid2,uid3"}
	 * @param operator 操作者
	 */
	public static String addGroupMembers(String operator,JsonWrapper json) {
		String gid = json.get("gid");
		if ( null == gid ) return errorResult("[addGroupMembers][gid is null]");
		String membersStr = json.get("members");
		if ( null == membersStr || membersStr.isEmpty() ) return errorResult("[addGroupMembers][members is null]");
		String[] members = membersStr.split(",");
		boolean operFlag = false;
		for (String affectedUsers:members) { // 添加成员到群
			operFlag = wesyncInstance.addMember(operator, gid, affectedUsers);
			if (!operFlag) {
				log.error("[wejoy][addGroupMembers][add "+affectedUsers+" to "+gid+" failed!]");
			}
		}
		sendToMembers(gid,getGroupSysMsg(WeJoyServiceOpType.addGroupMember,gid,membersStr,operator)) ;  
		return succResult(WeJoyServiceOpType.addGroupMember.name());
	}
	
	/** 
	 * 删除群组成员并向所有成员发送系统通知
	 * 请求串:{"key":"delGroupMember","gid":"G$32Q3$32","members":"uis1,uid2,uid3"}
	 * @param operator 操作者
	 */
	public static String removeGroupMembers(String operator,JsonWrapper json) {
		String gid = json.get("gid");
		if ( null == gid ) return errorResult("[addGroupMembers][gid is null]");
		String membersStr = json.get("members");
		if ( null == membersStr || membersStr.isEmpty() ) return errorResult("[addGroupMembers][members is null]");
		String[] members = membersStr.split(",");
		
		sendToMembers(gid,getGroupSysMsg(WeJoyServiceOpType.delGroupMember,gid,membersStr,operator)) ; 
		for (String affectedUsers:members) { // 删除成员从群
			wesyncInstance.removeMember(operator, gid, affectedUsers);
		}	
		return succResult(WeJoyServiceOpType.delGroupMember.name());
	}
	
	/** 
	 * 解散分组成员并发送通知
	 * 请求串:{"key":"quitGroup","gid":"G$32Q3$32"}
	 * @param operator 操作者
	 */
	public static String quitGroup(String operator,JsonWrapper json) {
		String gid = json.get("gid");
		if ( null == gid ) return errorResult("[quitGroup][gid is null]");
		sendToMembers(gid,getGroupSysMsg(WeJoyServiceOpType.quitGroup,gid,null,operator)) ; // 给系统所有成员发送信息
		HashSet<String> groupMembers = wesyncInstance.groupMembers(gid);
		for (String affectedUsers:groupMembers) { // 删除成员从群
			wesyncInstance.removeMember(operator, gid, affectedUsers);
		}	
		return succResult(WeJoyServiceOpType.quitGroup.name());
	}
	
	/** 生成系统管理消息*/
	private static String getGroupSysMsg (WeJoyServiceOpType doveType,String gid,String membersStr,String operator) {
		JsonBuilder builder = new JsonBuilder();
		builder.append("key", doveType.name());
		builder.append("gid", gid);
		if (null != membersStr) {
			builder.append("members", membersStr);
		}
		builder.append("operator", operator);
		return builder.flip().toString();
	}
	
	/** 给所有的群成员发送系统消息 */
	private static void sendToMembers(String gid,String content) {
		HashSet<String> groupMembers = wesyncInstance.groupMembers(gid);
		boolean operFlag = false;
		for (String touid:groupMembers) { 
			operFlag = wesyncInstance.sendToUser(SYS, touid, content);
			if (!operFlag) {
				log.error("[wejoy][removeGroupMembers][send content "+" to "+ touid +" in "+gid+" failed!]");
			}
		}
	}
	
	/** 获取聊天成员返回uid用逗号分割*/
	public static String chatMembers(String fromuid, String gid) {
		HashSet<String> groupMembers = wesyncInstance.groupMembers(gid);
		StringBuilder uids = new StringBuilder();
		for (String touid:groupMembers) { 
			uids.append(touid).append(",");
		}
		String result = uids.toString();
		if (result.length() > 2) {
			result = result.substring(0, result.length() -1);
		}
		return result;
	}
}
