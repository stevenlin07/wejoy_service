package com.weibo.wejoy.group.dao.impl;


import cn.sina.api.commons.util.ApiUtil;


/**
 * <pre>
 *  
 *  分库分表策略 :
 *     rang 		:  id % (shareDBCount * shareTableCount) 
 *  
 *     db 			:  if (shareDBCount == 1) db is shareDBPrefix
 *     				   else  db is shareDBPrefix + rang % shareDBCount 
 *  
 * 	   tableSuffix 	:  rang / shareDBCount 
 *   	
 * </pre>
 * 
 */
public class HashShareStrategy extends AbstractShareStrategy {
	
	@Override
	public String getTableSuffix(long id) {
		if(shareTableCount == 1) return shareOncePrefix;
		
		long hash = ApiUtil.getHash4split(id, splitCount);
				
		return String.valueOf(hash / shareDBCount + 1);
	}
	
	public String getTableSuffix(String id) {
		return null;
	}

	@Override
	public String getDBName(long id) {
		if (shareDBCount == 1) return shareOncePrefix;
		
		long hash = ApiUtil.getHash4split(id, splitCount);

		return shareDBPrefix + ( hash % shareDBCount + 1);
	}
	

	public void setSplitCount(int splitCount) {
		this.splitCount = splitCount;
	}
	
	public void setShareOncePrefix(String shareOncePrefix) {
		this.shareOncePrefix = shareOncePrefix;
	}
	
	private int splitCount;
	private String shareOncePrefix;
	
	public static void main(String[] arsg){
		long id = 2565640713l;
		
		HashShareStrategy test = new HashShareStrategy();
		test.splitCount = 64;
		test.shareDBPrefix = "contact_";
		test.shareDBCount = 4;
		test.shareTableCount = 16;
		
		System.out.println("dbName: "+ test.getDBName(id));
		System.out.println("tableName: "+ test.getTableSuffix(id));
	}
	
}

