package com.weibo.wejoy.wesync;

import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.weibo.wejoy.wesync.listener.GroupChatService;
import com.weibo.wejoy.wesync.listener.model.GroupOperationType;

public class WesyncApiImpl extends SyncService implements WesyncInApi {

	private static Logger log = Logger.getLogger(WesyncApiImpl.class);
	private static WesyncApiImpl wesyncInstance = new WesyncApiImpl();
	
	public void initWesyncInstance(GroupChatService groupChatService) {
		log.warn("[WesyncInstance][INIT]");
		if(null == groupChatService){
			log.error("[WesyncInstance][INJECT][ERROR]");
		}
		wejoyListener = (GroupChatService) groupChatService;
	}
	
	public static WesyncApiImpl getInstance(){
		if(wesyncInstance == null) {
			synchronized(WesyncApiImpl.class) {
				if(wesyncInstance == null) {
					wesyncInstance = new WesyncApiImpl();
				}
			}
		}	
		return wesyncInstance;
	}
	
	@Override
	public boolean sendToUser(String fromuid, String touid, String content) {
		 return sendMeta(fromuid, touid, content,ConvType.SINGLE);
	}

	@Override
	public boolean sendToGroup(String fromuid, String gid, String content) {
		return sendMeta(fromuid, gid, content,ConvType.GROUP);
	}
	
	@Override
	public String createGroup(String operator, List<String> groupMembers) {
		return wejoyListener.createGroup(operator, (List<String>) groupMembers);
	}

	@Override
	public boolean addMember(String operator,String gid,String affectedUsers) {
		return sendGroupOper(GroupOperationType.addMember,gid,operator,affectedUsers);
	}

	@Override
	public boolean removeMember(String operator,String gid,String affectedUsers) {
		return sendGroupOper(GroupOperationType.removeMember,gid,operator,affectedUsers);
	}

	@Override
	public boolean quitGroup(String operator, String gid) {
		return sendGroupOper(GroupOperationType.quitGroup,gid,operator,operator);
	}

	@Override
	public HashSet<String> groupMembers(String gid) {
		return wejoyListener.getGroupMembers(gid);
	}

	@Override
	public boolean delGroup(String gid) {
		// TODO Auto-generated method stub
		return false;
	}

}
