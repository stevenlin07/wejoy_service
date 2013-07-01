package com.weibo.wejoy.firehose.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.sina.api.data.storage.cache.MemCacheStorage;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.module.DbModule;

@SuppressWarnings("unchecked")
public class GetGids {
	private static List2GroupDao list2GroupDao;
	private static MemCacheStorage<String> memCacheStorage;
	
	static {
		Injector injector = Guice.createInjector(new DbModule());
		list2GroupDao = injector.getInstance(List2GroupDao.class);
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"firehose.xml"});
		memCacheStorage =  (MemCacheStorage<String>) ctx.getBean("memCacheStorage");
	}
	
	public static List<String> getGids(String uid){
		List<String> gids = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		sb.append("M").append(uid);
		String gidsStr = memCacheStorage.get(sb.toString());
		if(null != gidsStr){
			String[] gidsArr = gidsStr.split(",");
			for(String gid : gidsArr){
				gids.add(gid);
			}
		}else{
			gids = list2GroupDao.getGroupIdsByUid(uid);
		}
		return gids;
	}

}
