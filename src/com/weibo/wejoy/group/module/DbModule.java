package com.weibo.wejoy.group.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.dom4j.Element;

import cn.sina.api.commons.util.ApiLogger;
import cn.sina.api.data.dao.util.JdbcTemplate;
 
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.dao.ShareStrategy;
import com.weibo.wejoy.group.dao.impl.HashShareStrategy;
import com.weibo.wejoy.group.dao.impl.OriList2GroupDaoImpl;
import com.weibo.wejoy.utils.CommonUtil;
import com.weibo.wejoy.utils.XmlUtil;

public class DbModule extends AbstractContactModule {
	
	private String configPath = "dove-db.xml";
	
	@Override
	public String getConfigPath(){
		return configPath;
	}
	
	@Provides
	@Singleton
	List2GroupDao provideList2GroupDao() {
		OriList2GroupDaoImpl list2GroupDaoImpl = new OriList2GroupDaoImpl();
		try {
			list2GroupDaoImpl.setShareStrategys(initShareStrategys());
			return list2GroupDaoImpl;
		} catch (Exception e) {
			ApiLogger.error("when provide List2GroupDao, error occured,: ", e);
			throw new RuntimeException("provide List2GroupDao error");
		}
	}
	
	public void doOtherInitialization() {
		//bind(List2GroupService.class).to(List2GroupServiceImpl.class);
		//bind(List2GroupDao.class).to(OriList2GroupDaoImpl.class);
		
	}
	

	public Map<String, ShareStrategy> initShareStrategys() {
		Map<String, ShareStrategy> strategyMap = new HashMap<String, ShareStrategy>();
		
		try {
			Element elem = XmlUtil.getRootElement(document);
			List<Element> strategys = XmlUtil.getChildElements(elem);

			for (Element strategy : strategys) {
				HashShareStrategy shareStrategy = new HashShareStrategy();
				String strategykey = XmlUtil.getAttByName(strategy, "strategykey");
				String shareDBPrefix = XmlUtil.getAttByName(strategy, "shareDBPrefix");
				String shareDBCount = XmlUtil.getAttByName(strategy, "shareDBCount");
				String shareTableCount = XmlUtil.getAttByName(strategy, "shareTableCount");
				String splitCount = XmlUtil.getAttByName(strategy, "splitCount");
				String shareOncePrefix = XmlUtil.getAttByName(strategy, "shareOncePrefix");
				shareStrategy.setShareDBCount(Integer.valueOf(shareDBCount));
				shareStrategy.setShareDBPrefix(shareDBPrefix);
				shareStrategy.setShareTableCount(Integer.valueOf(shareTableCount));
				shareStrategy.setSplitCount((Integer.valueOf(splitCount)));
				shareStrategy.setShareOncePrefix(shareOncePrefix);

				strategyMap.put(strategykey, shareStrategy);

				Element config = XmlUtil.getElementByName(strategy, "config");
				Element jtl = XmlUtil.getElementByName(strategy, "jdbctemplate");

				Map<String, JdbcTemplate> jts = new HashMap<String, JdbcTemplate>();
				shareStrategy.setJts(jts);
				//db name must start from 1
				AtomicInteger counter = new AtomicInteger(1);
				
				int dbCount = CommonUtil.parseInteger(shareDBCount);
				Element masterConfig = XmlUtil.getElementByName(jtl, "master");
				ComboPooledDataSource masterDs = getDataSource(config);
				initDatasource(masterDs, masterConfig);

				Element slaveConfig = XmlUtil.getElementByName(jtl, "slave");
				ComboPooledDataSource slaveDs = getDataSource(config);
				initDatasource(slaveDs, slaveConfig);

				JdbcTemplate jtl0 = new JdbcTemplate();
				jtl0.setDataSource(masterDs);
				List<DataSource> slavelist = new ArrayList<DataSource>();
				slavelist.add(slaveDs);
				jtl0.setDataSourceSlaves(slavelist);
				for (int i = 0; i < dbCount; i++) {

					jts.put(shareDBPrefix + counter.getAndIncrement(), jtl0);
				}
			}
			return strategyMap;
		} catch (Exception e) {
			System.out.println(e);
			ApiLogger.error("when provide ClusterDatabases, error occured,: ", e);
			throw new RuntimeException("provide ClusterDatabases error");
		}

	}
	
	//TODO 类似这种db的基本配置，只应该加载一次，后期考虑优化
	private ComboPooledDataSource getDataSource(Element config) throws Exception {
		ComboPooledDataSource ds = new ComboPooledDataSource();
		
		String driver = XmlUtil.getAttByName(config, "driverClass");
		String minPoolSize = XmlUtil.getAttByName(config, "minPoolSize");
		String maxPoolSize = XmlUtil.getAttByName(config, "maxPoolSize");
		String idleConnectionTestPeriod = XmlUtil.getAttByName(config, "idleConnectionTestPeriod");
		String maxIdleTime = XmlUtil.getAttByName(config, "maxIdleTime");
		String breakAfterAcquireFailure = XmlUtil.getAttByName(config, "breakAfterAcquireFailure");
		String checkoutTimeout = XmlUtil.getAttByName(config, "checkoutTimeout");
		String acquireRetryAttempts = XmlUtil.getAttByName(config, "acquireRetryAttempts");
		String acquireRetryDelay = XmlUtil.getAttByName(config, "acquireRetryDelay");
		
		ds.setDriverClass(driver);
		ds.setMinPoolSize(Integer.valueOf(minPoolSize));
		ds.setMaxPoolSize(Integer.valueOf(maxPoolSize));
		ds.setIdleConnectionTestPeriod(Integer.valueOf(idleConnectionTestPeriod));
		ds.setMaxIdleTime(Integer.valueOf(maxIdleTime));
		ds.setBreakAfterAcquireFailure(Boolean.valueOf(breakAfterAcquireFailure));
		ds.setCheckoutTimeout(Integer.valueOf(checkoutTimeout));
		ds.setAcquireRetryAttempts(Integer.valueOf(acquireRetryAttempts));
		ds.setAcquireRetryDelay(Integer.valueOf(acquireRetryDelay));
		
		return ds;
	}
	
	private void initDatasource(ComboPooledDataSource ds, Element dselem) {
		ds.setJdbcUrl(XmlUtil.getAttByName(dselem, "url"));
		ds.setUser(XmlUtil.getAttByName(dselem, "user"));
		ds.setPassword(XmlUtil.getAttByName(dselem, "password"));
	}
	

/*	public static void main(String[] args) {
		
		Injector injector = Guice.createInjector(new DbModule());
		List2GroupDao list2GroupDao = injector.getInstance(List2GroupDao.class);
		System.out.println(list2GroupDao.getClass().getName());
		long listId = 3888083389912465l;
		long statusId = 3599599991871499l;
		String groupId = "G$1678339034$63";
		String uid = "1678339034";
		//boolean result =  list2GroupDao.saveListId2GroupIdmMapping(listId, statusId, groupId, uid);
		//boolean sta = list2GroupDao.deleteNotActiveGidsInDb(groupId, uid);
		int index = groupId.indexOf("$");
		String dd = groupId.substring(index+1, groupId.length());
		int index1 = dd.indexOf("$");
		String ss = dd.substring(0,index1);
		System.out.println(ss);
		
	}*/
}
