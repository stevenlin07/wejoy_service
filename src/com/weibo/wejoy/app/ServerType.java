package com.weibo.wejoy.app;

public enum ServerType {

	weimiPlatform(1), weimiActivity(2), weiboPlatform(3), unknown(99);

	private final int value;

	private ServerType(int value) {
		this.value = value;
	}

	public int get() {
		return value;
	}

	public static ServerType valueOf(int value) {
		for (ServerType type : ServerType.values()) {
			if (value == type.value)
				return type;
		}
		return unknown;
	}
}
