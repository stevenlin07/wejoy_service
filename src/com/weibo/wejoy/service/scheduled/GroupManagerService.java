package com.weibo.wejoy.service.scheduled;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.module.DbModule;
import com.weibo.wejoy.wesync.WesyncApiImpl;

public enum GroupManagerService {
	
	INSTANCE;
	
	/**
	 * 群的最后交互时间：>30天 删掉此群
	 * 觉得应该在群聊转评论时记录时间更好，数据库中已增加最后评论时间字段，每次群聊转评论时更新一下此字段
	 * 前置任务：这块需要整合源哥代码
	 */
	
	private static Logger log = Logger.getLogger(GroupManagerService.class);
	private static List2GroupDao list2GroupDao;
	private WesyncApiImpl wesyncInstance = WesyncApiImpl.getInstance();
	private Timer timer = new Timer();
	private final long startTime = 600*1000;
	private final long timeslice = 24*3600*1000;
	
	 static {
		 Injector injector = Guice.createInjector(new DbModule());
		 list2GroupDao = injector.getInstance(List2GroupDao.class);
	}

	
	/*对于不活跃大于30天的群
	1.wesync 删除群
	2.从db中删除群*/
	 
	public void start(){
		timer.schedule(new GroupManagerTask(), startTime,timeslice);
	}
 
	class GroupManagerTask extends TimerTask {
		@Override
		public void run() {
			log.info("GroupManagerService begin");
			//cleanNotActiveGroup();
		}
	}
	
	public void cleanNotActiveGroup(){
		ArrayList<String> gids =  (ArrayList<String>) list2GroupDao.getInvalidGids();
		String withtag= "quiteMember";
		boolean isSuccess = false;
		if(null != gids){
			for(String gid : gids){
				boolean result = false;
				HashSet<String> uids = wesyncInstance.groupMembers(gid);
				for(String uid : uids){
					 result = wesyncInstance.quitGroup(uid, gid);
					 if(!result){
						 log.warn("[GroupManagerService:cleanNotActiveGroup] 退出群时失败 gid: " + gid + "uid: " + uid + "WesyncResult: " + result);
						 break;
					 }
				}
				
				if(!result){
					isSuccess = list2GroupDao.deleteNotActiveGidsInDb(gid, getFromuid(gid));
					if(!isSuccess){
						isSuccess = list2GroupDao.deleteNotActiveGidsInDb(gid, getFromuid(gid));
						if(!isSuccess){
							//TODO 将需要数据修复的日志打到一个特别指定的地方
							log.warn("[GroupManagerService:cleanNotActiveGroup] 从db中删除gid时失败: " + gid);
						}
					}
				}
			}
		}
	}
	
	private static String getFromuid(String gid){
		int index1 = gid.indexOf("$");
		String back = gid.substring(index1 + 1, gid.length());
		int index2 = back.indexOf("$");
		String fromuid = back.substring(0, index2); 
		return fromuid;
	}
	
}
