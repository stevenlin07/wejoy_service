package com.weibo.wejoy.firehose.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.weibo.wejoy.app.AppProxy;

import cn.sina.api.commons.util.ApiLogger;
import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper;

public class ProxyService {
	
	static String URL_GET_GROUPMEMBERS = "http://i2.api.weibo.com/2/friendships/groups/members/ids.json";
	static String URL_GET_BILATERALIDS = "http://i2.api.weibo.com/2/friendships/friends/bilateral/ids.json";
	static String WEIJU_SOURCE;
	static String ORIGINAL_SOURCE;
	private static final String configfile = "source.properties";
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
	
	//获取指定人的双向联系人列表
	static HashSet<String> getBilateralIds(String fromuid) {
		HashSet<String> bilateralIds = new HashSet<String>();
		ProxyResult proxyResult = new ProxyResult();
		String request = String.format("source=%s&uid=%s", WEIJU_SOURCE, fromuid);
		proxyResult = proxyInterface(fromuid, getRequestJson(URL_GET_BILATERALIDS, request, WEIJU_SOURCE, 1), null);
		if(proxyResult.flag){
			try {
		        JsonWrapper json = new JsonWrapper(proxyResult.result);
		        long[] ids = json.getLongArr("ids");
		        for (long uid : ids)
		          bilateralIds.add(String.valueOf(uid));
		      }
		      catch (Exception e) {
		        ApiLogger.warn("[AppProxy Failed] 获取双向联系人失败");
		      }
		}else{
			ApiLogger.warn("[AppProxy Failed] appProxy时获取双向联系人失败");
		}
		
		return bilateralIds;
	}
	

	/**
	 * 得到某一组的成员ids
	 * 通过appProxy的方式调用/friendships/groups/members
	 * @throws Exception 
	 * 
	 */
	
	static HashSet<String> getMembersId(String fromuid, long listId) {

		HashSet<String> members = new HashSet<String>();
		ProxyResult proxyResult = null;
		String request = String.format("source=%s&list_id=%d", WEIJU_SOURCE, listId);
		proxyResult = proxyInterface(fromuid, getRequestJson(URL_GET_GROUPMEMBERS,request, WEIJU_SOURCE, 1), null);
		if(proxyResult.flag){
			   try {
			        JsonWrapper json = new JsonWrapper(proxyResult.result);
			        long[] ids = json.getLongArr("users");
			        for (long uid : ids)
			          members.add(String.valueOf(uid));
			      }
			      catch (Exception e) {
			        ApiLogger.warn("[AppProxy Failed] 获取分组中成员失败");
			      }
		}else{
			ApiLogger.warn("[AppProxy Failed] appProxy时获取分组中成员失败");
		}
	
		return members;
	}
		
	static String getRequestJson(String url, String request, String source, int requestType) {
		JsonBuilder json = new JsonBuilder();
		json.append("url", url);
		json.append("requestType", requestType);// 上传文件是0 post，下行文件是1 get
		json.append("params", request);
		json.append("serverType", 3); // 3是微博服务
		json.append("source", source);
		return json.flip().toString();
	}

	static ProxyResult proxyInterface(String fromuid, String requestJson,
			byte[] attach) {
		ProxyResult proxyResult = new ProxyResult();
		JsonWrapper json = null;
		try {
			json = new JsonWrapper(requestJson);
		} catch (Exception e) {
		}
		AppProxy appProxy = AppProxy.getInstance();
		proxyResult.result = appProxy.process(fromuid, "127.0.0.1", json,
				attach);
		//TODO对403进行校验
		if (null == proxyResult.result || proxyResult.result.contains("error_code")) {
			proxyResult.flag = false;
		} else {
			proxyResult.flag = true;
		}
		return proxyResult;
	}
	
}
