package com.weibo.wejoy.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import cn.sina.api.commons.util.ApiLogger;
import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper;

import com.weibo.wejoy.app.AppProxy;
import com.weibo.wejoy.service.util.DivisionUids;
import com.weibo.wejoy.service.util.GetIntersection;
import com.weibo.wejoy.service.util.GroupName; 
import com.weibo.wejoy.service.util.ProxyResult;

public class ProxyService {

	static String URL_SENDORIENTWEIBO_TEXT = "http://i2.api.weibo.com/2/statuses/update.json";//发布纯文本微博
	static String URL_SENDORIENTWEIBO_IMAGE = "http://i2.api.weibo.com/2/statuses/upload.json";//发布带有图片的微博
	static String URL_CREATEGROUP = "http://i2.api.weibo.com/2/friendships/groups/create.json";//创建分组
	static String URL_ADDMEMBERTOGROUP = "http://i2.api.weibo.com/2/friendships/groups/members/add.json";//批量添加分组成员
	static String URL_ADDMEMBERTOGROUP_BATCH = "http://i2.api.weibo.com/2/friendships/groups/members/add_batch.json";//批量获取分组成员
	static String URL_GET_GROUPMEMBERS = "http://i2.api.weibo.com/2/friendships/groups/members/ids.json";//获取某个分组中成员的ids
	static String URL_GET_BILATERALIDS = "http://i2.api.weibo.com/2/friendships/friends/bilateral/ids.json";//获取双向联系人ids
	static String URL_DELETE_LIST = "http://i2.api.weibo.com/2/lists/destroy.json";//删除分组
	static String URL_GET_LISTS = "http://i2.api.weibo.com/2/friendships/groups.json";//获取某人所有分组的id
	static String URL_GET_USER_INFO = "http://i2.api.weibo.com/2/users/show.json";//获取用户信息
	static String URL_GET_STATUS_INFO = "http://i2.api.weibo.com/2/statuses/show.json";//获取指定id的微博信息
	
	static String URL_CREATE_COMMENTS = "http://i2.api.weibo.com/2/comments/create.json";
	
	private static final String configfile = "source.properties";
	static String WEIJU_SOURCE;
	static String ORIGINAL_SOURCE;
	private static final int max = 20; 
	private static Logger log = Logger.getLogger(ProxyService.class);
	
	static{
		FileInputStream in = null;
		Properties prop = new Properties();
		URL url = ProxyService.class.getClassLoader().getResource(configfile);
		try {
			in = new FileInputStream(url.getFile());
			prop.load(in);
			WEIJU_SOURCE = (String) prop.get("weiju");
			ORIGINAL_SOURCE = prop.getProperty("original");
		} catch (FileNotFoundException e) {
			log.error("[ProxyService] configuration file source.properties does not find");
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static void createComment(String comment,long statusId,String fromuid) {
		String request = "source="+WEIJU_SOURCE+"&&id="+statusId+"&&comment="+comment;
		proxyInterface(fromuid, getRequestJson(URL_CREATE_COMMENTS, request, WEIJU_SOURCE, 0), null);
	}
	
	
	/**
	 * 发送定向微博
	 * 
	 * @param pluginResult
	 * @param fromuid
	 * @param attach
	 * @return
	 */
	
	static String sendOrientWeibo(String fromuid,String text,String listId, byte[] attach) {
		String statusId = null;
		String urlForSendOrientWeibo = null;
		ProxyResult proxyResult = null;
		String request = String.format("status=%s&visible=3&source=%s&list_id=%s", text, WEIJU_SOURCE,listId);
		if (null == attach || 0 == attach.length) {
			urlForSendOrientWeibo = URL_SENDORIENTWEIBO_TEXT;
			proxyResult = proxyInterface(fromuid, getRequestJson(urlForSendOrientWeibo, request, WEIJU_SOURCE, 0), attach);
		} else {
			urlForSendOrientWeibo = URL_SENDORIENTWEIBO_IMAGE;
			proxyResult = proxyInterface(fromuid, getRequestJson(urlForSendOrientWeibo, request, WEIJU_SOURCE, 2), attach);
		}
		ApiLogger.warn("[AppProxy][发布定向微博] " + proxyResult.result);
		if (proxyResult.flag) {
			try {
				JsonWrapper resultJson = new JsonWrapper(proxyResult.result);
				statusId = resultJson.get("idstr");
			} catch (Exception e) {
				ApiLogger.warn("[AppProxy Failed] 发布定向微博返回结果失败 " + e);
			}
		} else {
			ApiLogger.warn("[AppProxy Failed] 发布定向微博失败");
		}
		return statusId;
	}

	/**
	 * 查询某个用户所有的分组的信息
	 * @param fromuid
	 * @return
	 */
	
	static String getGroupsInfo(String fromuid){
		String result = null;
		String request = String.format("source=%s&uid=%s", WEIJU_SOURCE, fromuid);
		ProxyResult proxyResult = proxyInterface(fromuid,getRequestJson(URL_GET_LISTS, request, WEIJU_SOURCE, 1), null);
		if(proxyResult.result.contains("idstr")){
			result = proxyResult.result;
		}
		return result;
	}
	
	/**
	 * 创建微博分组
	 * 
	 * @param fromuid
	 * @return statusId
	 * @throws Exception
	 */
	static String createGroupWeibo(String fromuid) {
		String listId = null;
		long name = System.currentTimeMillis();
		String request = String.format("source=%s&name=微聚%s", WEIJU_SOURCE, GroupName.timeToStr(name));//weiju source 1124443769
		ProxyResult proxyResult = proxyInterface(fromuid, getRequestJson(URL_CREATEGROUP, request, WEIJU_SOURCE, 0), null);
		ApiLogger.warn("[AppProxy][创建微博分组] " + proxyResult.result);
		if (proxyResult.flag) {
			try {
				JsonWrapper resultJson = new JsonWrapper(proxyResult.result);
				listId = resultJson.get("idstr");
			} catch (Exception e) {
				ApiLogger.warn("[AppProxy Failed] 创建微博分组返回结果失败 " + "proxyResult: " + proxyResult.result + " " + e);
			}
		} else {
			ApiLogger.warn("[AppProxy Failed] 创建微博分组失败");
		}
		return listId;
	}
	
	/**
	 * 获取用户信息
	 * @param fromuid
	 * @param uid
	 * @return
	 */
	static String getUserInfo(String fromuid, String uid){
		String result = null;
		String request = String.format("source=%s&uid=%s", WEIJU_SOURCE, uid);
		ProxyResult proxyResult = proxyInterface(fromuid, getRequestJson(URL_GET_USER_INFO, request, WEIJU_SOURCE, 1), null);
		if(proxyResult.result.contains("screen_name")){
			result =  proxyResult.result;
		}else{
			ApiLogger.warn("[AppProxy Failed] 获取用户信息失败," + "proxyResult" + proxyResult.result);
		}
		return result;
	}
	
	/**
	 * 获取指定的微博信息
	 * @param statusId
	 * @param fromuid
	 * @return
	 */
	static String getStatusInfo(String statusId, String fromuid){
		String result = null;
		String request = String.format("source=%s&id=%s", WEIJU_SOURCE, statusId);
		ProxyResult proxyResult = proxyInterface(fromuid, getRequestJson(URL_GET_STATUS_INFO, request, WEIJU_SOURCE, 1), null);
		if(proxyResult.result.contains("created_at")){
			result = proxyResult.result;
		}else{
			ApiLogger.warn("[AppProxy Failed] 获取微博信息失败," + "proxyResult" + proxyResult.result);
		}
		return result;
	}
	
	static String getListMembersInfo(String listId, String fromuid){
		String result = "{\"code\":2002,\"text\":\"[ProxyService] getListMembersInfo failure\"}";
		HashSet<String> membersId = getMembersId(Long.valueOf(listId), fromuid);
		HashSet<String> bilateralMembers = getBilateralIds(fromuid);
		if(0 != membersId.size() && 0 != bilateralMembers.size()){
			HashSet<String> intersection = GetIntersection.getIntersectionOf2Set(membersId, bilateralMembers);
			if(0 != intersection.size()){
				String info = null;
				String name = null;
				String userId = null;
				String remark = null;
				List<JsonBuilder> list =  new LinkedList<JsonBuilder>();
				for(String uid : intersection){
					info = getUserInfo(fromuid, uid);
					if(null != info){
						try {
							JsonBuilder builder = new JsonBuilder();
							JsonWrapper json = new JsonWrapper(info);
							name = json.get("name");
							userId = json.get("idstr");
							remark = json.get("remark");
							builder.append("name", name);
							builder.append("idstr", userId);
							builder.append("remark", remark);
							builder.flip();
							list.add(builder);
						
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				}
				JsonBuilder jsonStr = new JsonBuilder();
				jsonStr.append("key", "bi_users");
				jsonStr.appendJsonArr("usersInfo", list);
				
				result = jsonStr.flip().toString();
			}
		}
		return result;
	}
	/**
	 * 向分组中增加成员
	 * 
	 * @param fromuid
	 * @param pluginResult
	 * @return
	 */
	
	static boolean addMembers2Group(String fromuid, String listId,HashSet<String> uidsSet) {
		boolean result = true;
		ProxyResult proxyResult = null;
		String request;
		//max=20 because批量添加组员最多支持20个
		List<String> uidsList =  DivisionUids.divisionUids(uidsSet, max);
		
		for(String uids : uidsList){
			request = String.format("uids=%s&source=%s&list_id=%s", uids, WEIJU_SOURCE, listId);
			proxyResult = proxyInterface(fromuid, getRequestJson(URL_ADDMEMBERTOGROUP_BATCH, request, WEIJU_SOURCE, 0), null);
			if(!proxyResult.flag){
				log.warn("[proxyResult][微博分组批量添加成员出错]:uids " + uids);
				//删除分组 
				request = String.format("source=%s&list_id=%s", uids, WEIJU_SOURCE,  listId);
				proxyResult = proxyInterface(fromuid, getRequestJson(URL_DELETE_LIST, request, WEIJU_SOURCE, 0), null);
				if(!proxyResult.flag){
					//TODO 这种日志需要后续手动处理
					log.warn("[proxyResult] 删除分组时出错,list_id: " +  listId);
				}
				result = false;
				break;
			}
		}
		return result;
	}

	/**
	 * 得到某一组的成员ids 通过appProxy的方式调用/friendships/groups/members
	 * 
	 * @throws Exception
	 * 
	 */
	public static HashSet<String> getMembersId(long listId, String fromuid) {
		HashSet<String> members = new HashSet<String>();
		ProxyResult proxyResult = null;
		String request = String.format("source=%s&list_id=%d", WEIJU_SOURCE, listId);
		proxyResult = proxyInterface(fromuid, getRequestJson(URL_GET_GROUPMEMBERS, request, WEIJU_SOURCE, 1), null);
		if (proxyResult.flag) {
			try {
				JsonWrapper json = new JsonWrapper(proxyResult.result);
				long[] ids = json.getLongArr("users");
				for(long uid:ids){
					members.add(String.valueOf(uid));
				}
			} catch (Exception e) {
				ApiLogger.warn("[AppProxy Failed] 获取分组中成员失败", e);
			}
		}else {
			ApiLogger.warn("[AppProxy Failed] appProxy获取分组中成员失败: proxyResult: " + proxyResult.result);
		}
		return members;
	}
	
	/**
	 * 得到某一组的成员ids 通过appProxy的方式调用/friendships/groups/members
	 * 
	 * @throws Exception
	 * 
	 */
	public static List<Long> getMembersIdAsLong(long listId, String fromuid) {
		List<Long> members = new ArrayList<Long>();
		ProxyResult proxyResult = null;
		String request = String.format("source=%s&list_id=%d", WEIJU_SOURCE, listId);
		proxyResult = proxyInterface(fromuid, getRequestJson(URL_GET_GROUPMEMBERS, request, WEIJU_SOURCE, 1), null);
		
		if (proxyResult.flag) {
			try {
				JsonWrapper json = new JsonWrapper(proxyResult.result);
				long[] ids = json.getLongArr("users");
				
				for(Long uid : ids) {
					members.add(uid);
				}
			} 
			catch (Exception e) {
				ApiLogger.warn("[AppProxy Failed] 获取分组中成员失败", e);
			}
		}else {
			ApiLogger.warn("[AppProxy Failed] appProxy获取分组中成员失败: proxyResult: " + proxyResult.result);
		}
		
		return members;
	}

	// 获取指定人的双向联系人列表
	public static HashSet<String> getBilateralIds(String fromuid) {
		HashSet<String> bilateralIds = new HashSet<String>();
		ProxyResult proxyResult = new ProxyResult();
		String request = String.format("source=%s&uid=%s", WEIJU_SOURCE, fromuid);
		proxyResult = proxyInterface(fromuid, getRequestJson(URL_GET_BILATERALIDS, request, WEIJU_SOURCE, 1), null);
		if (proxyResult.flag) {
			try {
				JsonWrapper json = new JsonWrapper(proxyResult.result);
				long[] ids = json.getLongArr("ids");
				for(long uid:ids){
					bilateralIds.add(String.valueOf(uid));
				}
			} catch (Exception e) {
				ApiLogger.warn("[AppProxy Failed] 获取双向联系人失败");
			}
		} else {
			ApiLogger.warn("[AppProxy Failed] appProxy时获取双向联系人失败");
		}
		ApiLogger.warn("[getBilateralIds]:"+bilateralIds.size());
		return bilateralIds;
	}

	static String getRequestJson(String url, String request, String source, int requestType) {
		JsonBuilder json = new JsonBuilder();
		json.append("url", url);
		json.append("requestType", requestType);// 上传文件是0 post，下行文件是1 GET 2MUTIPART
		if(2 == requestType){
			json.append("fileParam", "pic");
		}
		json.append("params", request);
		json.append("serverType", 3); // 3是微博服务
		json.append("source", source);
		return json.flip().toString();
	}
	

	static ProxyResult proxyInterface(String fromuid, String requestJson, byte[] attach) {
		ProxyResult proxyResult = new ProxyResult();
		JsonWrapper json = null;
		try {
			json = new JsonWrapper(requestJson);
		} catch (Exception e) {
		}
		AppProxy appProxy = AppProxy.getInstance();
		proxyResult.result = appProxy.process(fromuid, "127.0.0.1", json,
				attach);
		// TODO对403进行校验
		if (null == proxyResult.result || proxyResult.result.contains("error_code")) {
			proxyResult.flag = false;
		} else {
			proxyResult.flag = true;
		}
		return proxyResult;
	}
	
}
