package com.weibo.wejoy.wesync.listener.model;

/**
 * @ClassName: GroupOperation
 * @Description: 群操作
 * @author linlin
 * @date 2013-4-25 下午2:36:16
 */
public class GroupOperation {
	/** 群操作类型 */
	GroupOperationType type;
	/** 群ID */
	String gid;
	/** 操作者 */
	String operator;
	/** 被操作者 */
	String affectedUsers;

	public GroupOperation(GroupOperationType type, String gid, String operator,
			String affectedUsers) {
		this.type = type;
		this.gid = gid;
		this.operator = operator;
		this.affectedUsers = affectedUsers;
	}

	public GroupOperationType getType() {
		return type;
	}

	public String getGid() {
		return gid;
	}

	public String getOperator() {
		return operator;
	}

	public String getAffectedUsers() {
		return affectedUsers;
	}
}
