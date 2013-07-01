package com.weibo.wejoy.firehose.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.sina.api.commons.util.ApiLogger;
import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper;
import cn.sina.api.data.storage.cache.MemCacheStorage;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.module.DbModule;
import com.weibo.wejoy.wesync.WesyncApiImpl;


@SuppressWarnings("unchecked")
public class CreateGroup {
	
	WesyncApiImpl wesyncInstance = WesyncApiImpl.getInstance();
	private static  MemCacheStorage<String> memCacheStorage;
	private static List2GroupDao list2GroupDao;
	private final Logger log = Logger.getLogger(CreateGroup.class);
	private final String SYS = "8899";
	
	static {
		Injector injector = Guice.createInjector(new DbModule());
		list2GroupDao = injector.getInstance(List2GroupDao.class);
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "firehose.xml" });
		memCacheStorage =  (MemCacheStorage<String>) ctx.getBean("memCacheStorage");		
	}

	//创建群
	public boolean createGroup(String fromuid, JsonWrapper json) {
		boolean isSuccess = true;
		ParseResult parseResult = ParseForFirehose.parseListStatusId(json);
		String gid = null;
		if(parseResult.flag){
			
			String listId = String.valueOf(parseResult.listId);
			String statusId = String.valueOf(parseResult.statusId);
			HashSet<String> groupMembers = getGroupMembers(fromuid ,parseResult.listId);
			List<String> gm = new LinkedList<String>();
			gm.addAll(groupMembers);
			if(groupMembers.size() != 0){
				try {
					gid = wesyncInstance.createGroup(fromuid, gm);
					if(null == gid){
						log.warn("[createGroup] firehose 处理定向微博，创建群聊时失败, gid 为null");
						return false;
					}
					
					boolean success = list2GroupDao.saveListId2GroupIdmMapping(parseResult.listId, parseResult.statusId, gid, fromuid);
					if(!success){
						isSuccess = false;
						log.warn("[createGroup] 创建映射时错误");
					}
					
					//存mc
					//getChatInfo接口需要从gid查询listId和statusId
					//定向微博评论需要从listId和statusId得到gid
					String listStatusId = listId +","+ statusId;
					memCacheStorage.set(gid, listStatusId);
					memCacheStorage.set(listStatusId, gid);
					//关系变更需要从fromuid得到它所对应的所有的群聊
					String mfromuid = "M" + fromuid;//式样： M1678339034
					String gids = memCacheStorage.get(mfromuid);
					if(null == gids){
						memCacheStorage.set(mfromuid, gid);
					}else{
						memCacheStorage.set(mfromuid, gids + "," + gid);		
					}
					
					//给群中每个成员发消息
					sendmMessage2GroupMem(fromuid, gid, String.valueOf(parseResult.statusId), groupMembers);
					
				} catch (Exception e) {
					isSuccess = false;
					log.error("[create group] wesync创建群聊时错误" , e);
				}
			}
		}
		
		return isSuccess;
	}
	
	
	//TODO 确认是否有存在意义
	//得到groupMembers
	public HashSet<String> getGroupMembers(String fromuid, long listId) {
		HashSet<String> groupChatMembers = new HashSet<String>();
		
		//得到原始微博可见的组的成员
		HashSet<String> membersId = ProxyService.getMembersId(fromuid, listId);
		if(membersId.size() == 0){
			ApiLogger.warn("[create group] 获取分组成员时失败");
		}else{	
			//得到原始微博发起人的双向联系人的列表
			HashSet<String> bilateralFriends = ProxyService.getBilateralIds(fromuid);
			if(bilateralFriends.size() == 0 ){
				ApiLogger.warn("[create group] 获取双向联系人时失败");
			}else{
				//取上述二者的交集
				groupChatMembers = getIntersection(membersId, bilateralFriends);
			}
		}
	
		return groupChatMembers;
	}
	
	public void sendmMessage2GroupMem(String fromuid, String gid, String statusId, HashSet<String>  groupMembers){
		int num = groupMembers.size();
		String message = String.format("%s 邀请您加入此聊天群", fromuid);
		JsonBuilder builder = new JsonBuilder();
		builder.append("key", "newGroup");
		builder.append("message", message);
		builder.append("gid", gid);
		builder.append("statusId", statusId);
		builder.append("num", num);
		String content = builder.flip().toString();
		for(String uid : groupMembers){		
			wesyncInstance.sendToUser(SYS, uid, content);
		}		
		//for 群主
		JsonBuilder builder2 = new JsonBuilder();
		builder2.append("key", "sendWeibo");
		builder2.append("gid", gid);
		builder2.append("statusId", statusId);
		builder2.append("num", num);
		content = builder2.flip().toString();
		wesyncInstance.sendToUser(SYS, fromuid, content);
	}

	
	//TODO 优化
	//求两个集合的交集
	private HashSet<String> getIntersection(HashSet<String> membersId, HashSet<String> bilateralFriends){
		HashSet<String> groupChatMembers = new HashSet<String>();
		Iterator<String> iterator1 =membersId.iterator();	
		Iterator<String> iterator2 =bilateralFriends.iterator();
		
		while(iterator1.hasNext()){
			String v1 = iterator1.next();
			iterator2 = bilateralFriends.iterator();
			while(iterator2.hasNext()){
				String v2 = iterator2.next();
				if(v1.equals(v2)){
					groupChatMembers.add(v1);
				}
			}
		}	
		return groupChatMembers;
	}
	 
}
