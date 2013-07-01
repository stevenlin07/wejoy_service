package com.weibo.wejoy.wesync;

import java.util.HashSet;
import java.util.List;

public interface WesyncInApi {
	
	/** 向单个人发消息 */
	boolean sendToUser(String fromuid, String touid, String content);

	/** 向群组所有成员发消息 */
	boolean sendToGroup(String fromuid, String gid, String content);

	/** 创建一个群,返回gid */
	String createGroup(String operator, List<String> members);

	/** 向群中增加好友操作 */
	boolean addMember(String operator,String gid,String affectedUsers);

	/** 从群中删除好友操作 */
	boolean removeMember(String operator,String gid,String affectedUsers);

	/** 从群中退出操作 */
	boolean quitGroup(String fromuid, String gid);

	/** 删除群 */
	boolean delGroup(String gid);
	
	/** 获取群成员 */
	HashSet<String> groupMembers(String gid);
}
