package com.weibo.wejoy.service.scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.module.DbModule;

import cn.sina.api.data.storage.cache.MemCacheStorage;


/**
 *db重写缓存，时间间隔：6 hours 
 * @author xiaojing
 *
 */
@SuppressWarnings("unchecked")
public enum UpdateMc {
	
	INSTANCE;
	
	private Timer timer = new Timer();
	private final long startTime = 60*1000;
	private final long timeslice = 6*3600*1000;
	private static Logger log = Logger.getLogger(UpdateMc.class);
	private static  MemCacheStorage<String> memCacheStorage;
	private static List2GroupDao list2GroupDao;
	
	static {
		Injector injector = Guice.createInjector(new DbModule());
		list2GroupDao = injector.getInstance(List2GroupDao.class);
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "firehose.xml" });
		memCacheStorage =  (MemCacheStorage<String>) ctx.getBean("memCacheStorage");
	}
	
	public void start(){
		timer.schedule(new UpdataMcTask(), startTime,timeslice);
	}
	
	class UpdataMcTask extends TimerTask {
		@Override
		public void run() {
			log.info("UpdateMc begin");
			updateMc();
		}
	}

	private static  void updateMc(){
		List<String> uids = new ArrayList<String>();
		uids = list2GroupDao.getUid();
		boolean result ;
		for(String uid : uids){
			result = memCacheStorage.set(uid, uid);
			if(!result){
				result = memCacheStorage.set(uid, uid);
				if(!result){//TODO 日志打到特殊的地方
					log.warn("[UpdatMc:updataMc] db重写mc失败");
				}
			}
		}
	}

}
