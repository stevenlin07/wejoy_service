package com.weibo.wejoy.service.scheduled;

import java.util.Timer;
import java.util.TimerTask;

import java.util.List;

import org.apache.log4j.Logger;

import cn.sina.api.commons.util.JsonBuilder;
import cn.sina.api.commons.util.JsonWrapper;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.wejoy.app.AppUtil;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.module.DbModule;
import com.weibo.wejoy.wesync.WesyncApiImpl;

public enum RecommnService{
	
	INSTANCE;
	
	private static Logger log = Logger.getLogger(RecommnService.class);
	private WesyncApiImpl wesyncInstance = WesyncApiImpl.getInstance();
	private AppUtil appUtil = AppUtil.getInstance();
	private Timer timer = new Timer();
	private final static String SYSTEM = "8899";
	private final static String RECO =  "9876";
	private final static int RECONUM = 3;
	private final long startTime = 600*1000;
	private final long timeslice = 8*3600*1000;
	private static List2GroupDao list2GroupDao;
	
	static {
		Injector injector = Guice.createInjector(new DbModule());
		list2GroupDao = injector.getInstance(List2GroupDao.class);
	}

	public void start(){
		timer.schedule(new RecoTask(), startTime, timeslice);
	}
	
	class RecoTask extends TimerTask {
		@Override
		public void run() {
			log.info("[Get][Recommn]");
			send();
		}
	}

	
	public void send(){ // //取db，得到所有微聚注册用户的uid
		List<String>  uids = list2GroupDao.getUid();
		if(null == uids){ 
			log.warn("[RecommnApi] 调用getUid方法时没有获取到结果");
			return;
		}
		JsonBuilder builder = new JsonBuilder();
		for(String uid : uids){
			String recoUrl = String.format("http://api.ds.weibo.com/user_reco?uid=%s", uid);
			String result = appUtil.requestGetUrl(recoUrl, null,null);
			if(null == result){
				continue;
			}
			JsonWrapper json;
			try {
				json = new JsonWrapper(result);
				String data = json.getJsonNode("data").toString();
				JsonWrapper resultJson = new JsonWrapper(data.substring(1, data.length() -1));
				String reco = resultJson.get("reco");
				String content = getRecommend(reco, RECONUM);
				builder.append("type", RECO);
				builder.append("statusIds", content);
				builder.append("uid", uid);
				wesyncInstance.sendToUser(SYSTEM, uid, builder.flip().toString());
				builder.reset();
			}catch (Exception e) {
				log.info("[RecommnService][parse error] uid: " + uid);
				e.printStackTrace();
				builder.reset();
			}		
		}		
	}
		
	//得到推荐的微博id
	public String getRecommend(String reco, int length){
		StringBuilder buider = new StringBuilder();
		String[] line = reco.split(",");
		String[] first = line[0].split(":");
		buider.append(first[0].substring(1, first[0].length())).append(",");

		for(int i=1;i<length;i++){
			String[] later = line[i].split(":");
			buider.append(later[0]).append(",");
		}
		String result = buider.toString();
		return result.substring(0, result.length() -1);
	}

}
