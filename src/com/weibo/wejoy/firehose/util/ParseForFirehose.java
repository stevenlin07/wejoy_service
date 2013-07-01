package com.weibo.wejoy.firehose.util;

import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.module.DbModule;
import com.weibo.wejoy.wesync.WesyncApiImpl;
import com.weibo.wesync.data.GroupOperationType;

public class ParseForFirehose {
	
	private final static Logger log = Logger.getLogger(ParseForFirehose.class);
	private static WesyncApiImpl wesyncInstance = WesyncApiImpl.getInstance();
	private static List2GroupDao list2GroupDao;
	
	static{
		Injector injector = Guice.createInjector(new DbModule());
		list2GroupDao = injector.getInstance(List2GroupDao.class);
	}


//判断是不是关系变更
	public static boolean isRelationChange(JsonWrapper json){
		boolean result = false;
		String type = json.getNode("text").get("type");
		String event = json.getNode("text").get("event");
		if(type.equals("user") && event.equals("unfollow")){
			result = true;
		}

		return result;
	}
	
	//得到关系变更消息的fromuid
	public static String getFromuidFromRelationChange(JsonWrapper json){
		return json.getNode("text").getNode("source").get("idstr");
	}
	//得到关系变更的touid
	public static String getUidFromRelationChange(JsonWrapper json){
		return json.getNode("text").getNode("target").get("idstr");
	}
	
	//处理分组变更给群聊带来的影响(加入群和退出群)
	public static void processGroupChange(String line, GroupOperationType type, long listId) throws Exception {
		//从listId获得所有它所对应的群聊
		JsonWrapper json = new JsonWrapper(line);
		long[] arr = json.getNode("text").getNode("user_group").getLongArr("members");
		String fromuid = json.getNode("text").getNode("user_group").get("uid");
		List<String> gids = list2GroupDao.getGroupIdsByListId(listId, fromuid);
		HashSet<String> members = new HashSet<String>();
		for(long mem : arr){
			members.add(String.valueOf(mem));
		}
		for(String gid : gids){
		//得到members与每个群聊中成员的交集
			HashSet<String> changedMembers = getIntersection(members, gid);
			if(changedMembers.size()!=0){
				if(type == GroupOperationType.removeMember){
					for(String affectedUsers:changedMembers){
						boolean result = wesyncInstance.removeMember(fromuid, gid, affectedUsers);
						if(result){
							//给群成员发消息
							sendMessage(gid, fromuid, changedMembers, 2);
						}else{
							log.warn("[ParseForFirehose:processGroupChange] 删除群成员时出错   Wesync" + result);
						}
					}
				}else if(type == GroupOperationType.addMember){
					for(String affectedUsers:changedMembers){
						boolean result = wesyncInstance.addMember(fromuid, gid, affectedUsers);
						if(result){
							sendMessage(gid, fromuid, changedMembers, 3);
						}else{
							log.warn("[ParseForFirehose:processGroupChange] 添加群成员时出错   Wesync" + result);
						}
					}
					
				}else {	
					
				}
			}
		}	
	}
	
	//分组变更时给群发消息
	public static void sendMessage(String gid, String fromuid, HashSet<String> changedMembers, int oper){
		//根据gid获得此聊天群中的成员
		HashSet<String> groupMembers =  wesyncInstance.groupMembers(gid);
		//得到groupMembers中不含changedMembers的Set
		HashSet<String> restMembers = new HashSet<String>();
		for(String gm : groupMembers){
			boolean flag = false;
			for(String cm : changedMembers){
				if(gm.equals(cm)){
					flag = true;
					break;
				}
			}
			if(!flag){
				restMembers.add(gm);
			}
		}
		
		//下发通知
		int type = 9999;
		JsonBuilder json = new JsonBuilder();
		JsonBuilder json1 = new JsonBuilder();
		json1.append("fid", fromuid);
		json.append("type", type);
		json.append("gid",gid);
		json.append("oper", oper);	
		String message = null;
		for(String change : changedMembers){
			json1.append("touid", change);
			json.append("operinfo",json1.flip());
			message = String.format("%s 退出了这个群", change);
			json.append("message", message);
			for(String restMem : restMembers){
				String content = json.flip().toString();
				boolean flag = wesyncInstance.sendToUser("8899", restMem, content);
				if(!flag){
					log.info("[ParseForFirehose:sendMessage] 分组变更时给群发通知时错误, WesyncResult");
				}else{
					json.reset();
					json1.reset();
				}			
			}
		}
	
	}
	
	//处理退出群
	public static void processGroupChangeForQuit(String line,long listId){
		
		JsonWrapper json;
		try {
			json = new JsonWrapper(line);
			String fromuid = json.get("uid");
			//得到一个群聊中的成员列表
			List<String> groupIds = list2GroupDao.getGroupIdsByListId(listId, fromuid);
			HashSet<String> chatgroupMem = new HashSet<String>();
			for(String gid : groupIds){
				chatgroupMem = wesyncInstance.groupMembers(gid);
				for(String cgm : chatgroupMem){
					boolean result = wesyncInstance.quitGroup(fromuid, cgm);
					if(!result){
						log.warn("退出群失败      " + cgm);
					}
				}	
			}
		} catch (Exception e) {
			log.error("[ParseForFirehose] 处理退出群时错误");
		}
	
	}
	
	
	//处理关系变更给群聊带来的影响
	public static void processRelationChange(List<String> gids, String source, String target) {
		for(String gid : gids){
			if(null != gid){
				//得到一个群聊中的成员列表
				HashSet<String> groupMembers = wesyncInstance.groupMembers(gid);		
				//遍历此群聊的列表，判断uid是否属于其中
				for(String member : groupMembers){
					//如果uid属于此群聊，从此群中删除此成员
					if(target.equals(member)){
						HashSet<String> single = new HashSet<String>();
						single.add(target);
						for(String affectedUsers:single){
							boolean result = wesyncInstance.removeMember(source, gid, affectedUsers);
							if(!result){
								log.info("[processRelationChange]删除群成员失败  uid= " + target);
							}
						}
						
						
						//给群发通知消息
						int type = 9999;
						int oper = 2;
						JsonBuilder json = new JsonBuilder();
						JsonBuilder json1 = new JsonBuilder();
						json1.append("fid", source);
						json1.append("touid", target);
						json.append("type", type);
						json.append("gid",gid);
						json.append("oper", oper);
						json.append("operinfo",json1.flip());
						String message = String.format("%s 退出了这个群", target);
						json.append("message", message);
						for(String mem : groupMembers){
							if(!mem.equals(target)){
								String content = json.flip().toString();
								boolean sendMessage = wesyncInstance.sendToUser("8899", mem, content);
								if(!sendMessage){
									log.info("[ParseForFirehose] processRelationChange 处理关系变更时通知失败");
								}
							}
						}
					}
				}			
			}
		}
	}
	
	//得到members与每个群聊中成员的交集
	public static HashSet<String>  getIntersection(HashSet<String> members, String chatGroupId){
		//根据chatGroupId获得这个聊天群中的成员
		HashSet<String> chatMembers = new HashSet<String>();
		chatMembers =  wesyncInstance.groupMembers(chatGroupId);
		HashSet<String> finalMembers = new HashSet<String>();
	
		
		//求交集
		for(String fm : chatMembers ){
			for(String mem : members){
				if(fm.equals(mem)){
					finalMembers.add(fm);
				}
			}
		}	
		return finalMembers;
	}
			
	//判断是否是分组定向微博
	public static boolean isGrpOrientStatus(JsonWrapper json) {
		boolean result = false;
		String type = json.getNode("text").get("type");
		int visible_type = json.getNode("text").getNode("status").getNode("visible").getInt("type");
		if(visible_type == 3 && "grp_status".equals(type)){
			result = true;
		}	
		return result;
	}
	
	//判断是否是双向粉丝
	public static boolean isBilateralFans(String fromuid, String commentUid){
		boolean result = false;
		HashSet<String> bilaters = ProxyService.getBilateralIds(fromuid);
		for(String b : bilaters){
			if(commentUid.equals(b)){
				result = true;
			}
		}
		
		return result;
	}

	
	//从定向微博中得到原始微博发起人的uid
	public static String getUidFromStatus(JsonWrapper json) {
		return json.getNode("text").getNode("status").getNode("user").get("id");
	}
	
	
	//从定向微博的评论中得到原始微博发起人的uid
	public static String getUidFromComment(JsonWrapper json) {
		return json.getNode("text").getNode("comment").getNode("status").getNode("user").get("id");
	}
	
	//从分组定向微博得到分组id
	public static long getListId(JsonWrapper json) {
		return json.getNode("text").getNode("status").getNode("visible").getLong("list_id");
	}
	
	//从分组定向微博得到分组id
	public static long getListIdByComment(JsonWrapper json) {
		return json.getNode("text").getNode("comment").getNode("status").getNode("visible").getLong("list_id");
		 
	}
	
	
	//得到微博id
	public static long getGroupStatusId(JsonWrapper json){
		return json.getNode("text").getNode("status").getLong("id");	
		
	}
	
	
	//从评论中解析定向微博id
	public static long getGroupStatusIdByComment(JsonWrapper json){
		return json.getNode("text").getNode("comment").getNode("status").getLong("id");
	}
	
	//判断是否是分组定向微博评论
	public static boolean isOrientGroupComment(JsonWrapper json) { 
		boolean result = false;
		String text = json.getNode("text").get("type");	
			if(text.equals("grp_comment")){
				result = true;
			}	
		return result;
	}

	
	//得到这条评论的来源
	public static String getCommentSource(JsonWrapper json) {
		return json.getNode("text").getNode("comment").get("source");
		
	}
	
	//得到一条微博的来源
	public static String getStatusSource(JsonWrapper json) throws Exception{
		return json.getNode("text").getNode("status").get("source");
	}
	
	//得到评论内容
	public static String getComment(JsonWrapper json) {
		return json.getNode("text").getNode("comment").get("text");
		
	}
	
	//从定向微博评论中解析评论人uid
	public static String getCommentUid(JsonWrapper json) {
		return  json.getNode("text").getNode("comment").getNode("user").get("id");
	}
	
	/**
	 * 参数说明：
	 * fromuid：定向微博发起者的uid
	 * listId:定向微博所对应的分组的Id
	 * statusId:定向微博的id
	 * commentContent:分组定向微博评论的内容
	 */
	
	
	public static ParseResult parseComment(JsonWrapper json, String commentUid){
		ParseResult parseResult = new ParseResult();
		long listId = ParseForFirehose.getListIdByComment(json);	
		if(listId == 0){
			parseResult.flag = false;
			return parseResult;
		}
		parseResult.listId = listId;
		
		long statusId = ParseForFirehose.getGroupStatusIdByComment(json);
		if(statusId == 0 ){
			parseResult.flag = false;
			return parseResult;
		}
		parseResult.statusId = statusId;
		
		if(null == commentUid){
			parseResult.flag = false;
			return parseResult;
		}
		parseResult.commentUid = commentUid;
		
		String commentContent = ParseForFirehose.getComment(json);
		if(null == commentContent){
			parseResult.flag = false;
			return parseResult;
		}
		parseResult.commentContent = commentContent;
		
		return parseResult;
	}
	
	public static ParseResult parseListStatusId(JsonWrapper json){
		ParseResult parseResult = new ParseResult();
		long listId = ParseForFirehose.getListId(json);
		if(listId == 0){
			parseResult.flag = false;
			return parseResult;
		}
		parseResult.listId = listId;
		
		long statusId = ParseForFirehose.getGroupStatusId(json);
		if(statusId == 0){
			parseResult.flag = false;
			return parseResult;
		}
		parseResult.statusId = statusId;
		
		return parseResult;
	}

}
