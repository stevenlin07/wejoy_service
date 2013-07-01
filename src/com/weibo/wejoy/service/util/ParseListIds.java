package com.weibo.wejoy.service.util;

import java.util.LinkedList;
import java.util.List;

import cn.sina.api.commons.util.JsonBuilder;

public class ParseListIds {
	
	public static String parstListIds(String line){

		List<JsonBuilder> list = new LinkedList<JsonBuilder>();
		JsonBuilder jsonStr = new JsonBuilder();
		
		String[] sss = line.split("},");
		boolean isFirst = true;
		for(int i=0;i<sss.length;i=i+2){
			JsonBuilder json = new JsonBuilder();
			int indexIdStr = sss[i].indexOf("idstr");
			int indexName = sss[i].indexOf("name");
			int indexMode = sss[i].indexOf("mode");
			json.append("list_id", sss[i].substring(indexIdStr+8, indexName-3));
			json.append("name",sss[i].substring(indexName+7, indexMode-3));
			if(isFirst){
				json.flip();
				isFirst = false;
			}else{
				json.flip();
			}
			
			list.add(json);
			
		}
		
		jsonStr.append("key", "groups");
		jsonStr.appendJsonArr("value", list);
		
		return jsonStr.flip().toString();
	}
	
}
