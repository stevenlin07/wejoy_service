package com.weibo.wejoy.firehose.util;

public class ParseResult {
	public boolean flag = true;
	
	public long listId;//分组id
	public long statusId;//分组定向微博id
	
	public String fromuid;
	public String commentContent;//分组定向评论的评论内容
	public String commentUid; //分组定向评论者id
	public String commentSource;//分组定向评论来源
	}
