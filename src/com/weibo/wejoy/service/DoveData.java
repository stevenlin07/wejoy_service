package com.weibo.wejoy.service;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.sina.api.data.storage.cache.MemCacheStorage;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.dao.RegisterDao;
import com.weibo.wejoy.group.module.DbModule;

public enum DoveData {

	INSTANCE;
	
	public List2GroupDao list2GroupDao;
	public  MemCacheStorage<String> memCacheStorage;
	public RegisterDao registerDao;
	
	@SuppressWarnings("unchecked")
	DoveData () {
		Injector injector = Guice.createInjector(new DbModule());
		list2GroupDao = injector.getInstance(List2GroupDao.class);
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "firehose.xml" });
		registerDao = (RegisterDao)ctx.getBean("registerDao");
		memCacheStorage =  (MemCacheStorage<String>) ctx.getBean("memCacheStorage");
	}
	
}
