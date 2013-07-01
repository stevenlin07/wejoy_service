package com.weibo.wejoy.service.util;

import java.util.HashSet;

import com.weibo.wejoy.service.WeJoyServiceOpType;

public class PluginResult {

	public int code = 1000;
	public String result;
	public WeJoyServiceOpType doveType;
	
	public String listId;
	public String gid ;
	public String text;
	public String uid;
	public String registerUid;
	public String statusId;
	public HashSet<String> uidsSet;
}
