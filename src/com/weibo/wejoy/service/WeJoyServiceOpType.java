package com.weibo.wejoy.service;

public enum WeJoyServiceOpType {

	chatInfo(0),
	createGroupChat(1),
	register(2),
	groups(3),
	bi_users(4),
	userInfo(5),
	statusInfo(6),
	groupMembers(7),
	
	addGroupMember(20),
	delGroupMember(21),
	quitGroup(22),
	chatMembers(23),
	
	UNKNOWN(99);

	private final int value;

	private WeJoyServiceOpType(int value) {
		this.value = value;
	}

	public int get() {
		return value;
	}
	
	public static WeJoyServiceOpType valueOf(int value){
		for( WeJoyServiceOpType type: WeJoyServiceOpType.values() ){
			if ( value == type.value ) return type;
		}
		return UNKNOWN;
	}
	
	public static WeJoyServiceOpType valueOfStr(String value){
		for( WeJoyServiceOpType type: WeJoyServiceOpType.values() ){
			if ( value.equals(type.name()) ) return type;
		}
		return UNKNOWN;
	}
}
