package com.weibo.wejoy.app;

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

import cn.sina.api.commons.util.JsonWrapper;

public class AppProxy {

	private static Logger log = Logger.getLogger(AppProxy.class);
	private static AppProxy appProxy;
	private static TAuthUtil tauth = TAuthUtil.getInstance();
	
	private AppProxy() {

	}

	public static AppProxy getInstance() {
		if(appProxy == null) {
			synchronized(AppProxy.class) {
				if(appProxy == null) {
					appProxy = new AppProxy();
				}
			}
		}	
		return appProxy;
	}
	
	public String process(String fromuid,String remoteIp,JsonWrapper json,byte[] attach) {
		String result = "{\"code\":403,\"text\":\"app request failed\"}";
		try{
			AppRequest appRequest = AppProxyUtil.decodeAppRequest(json,attach);
			appRequest.uid = fromuid;
			appRequest.remoteIp = remoteIp;
			if(200 != appRequest.code){
				result = "{\"code\":"+appRequest.code+",\"text\":\""+appRequest.text+"\"}";
				log.error(result);
				return result;
			}
//			if(ServerType.weimiActivity == appRequest.serverType){
//				result = replyWeimiActivity(appRequest);
//			}else if(ServerType.weimiPlatform == appRequest.serverType){
//				result = replyWeimiPlatform(appRequest);
//			}else
				if(ServerType.weiboPlatform == appRequest.serverType){
				result = replyWeiboPlatform(appRequest);
			}
			if(null == result){
				result = "{\"code\":403,\"text\":\"app request failed\"}";
			}		
		}catch(Exception e) {
			log.error(e.getMessage(),e);
		}	
		return result;
	}
	
//	private String replyWeimiActivity(AppRequest appRequest){
//		appRequest.url = weimiActivity+appRequest.url;
//		Map<String, String> headers = new HashMap<String, String>();
//		headers.put("ls_uid",appRequest.uid);
//		return AppProxyUtil.deliverAppRequest(appRequest,headers);
//	}
//	
//	private String replyWeimiPlatform(AppRequest appRequest){
//		appRequest.url = weimiPlatform+appRequest.url;
//		Map<String, String> headers = new HashMap<String, String>();
//		headers.put("X-Matrix-UID", appRequest.uid);
//		headers.put("X-Matrix-AppID",Util.getConfigProp("X-Matrix-AppID","1"));
//		headers.put("X-Matrix-RemoteIP", appRequest.remoteIp);
//		return AppProxyUtil.deliverAppRequest(appRequest,headers);
//	}
	
	private String replyWeiboPlatform(AppRequest appRequest){
		Map<String, String> headers = new HashMap<String, String>();
		if(null == appRequest.source){
			return "{\"code\":403,\"text\":\"weibo request source is empty!\"}";
		}
		headers.put("Authorization",tauth.getToken(appRequest.uid,appRequest.source));
		headers.put("cuid",appRequest.uid);
		System.out.println(headers.toString());
		return AppProxyUtil.deliverAppRequest(appRequest,headers);
	}
}
