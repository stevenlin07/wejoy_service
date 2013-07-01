package com.weibo.wejoy.group.module;


import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * 
 * @author liyuan7
 *
 * 统一通过guice提供类的实例
 * 
 * 新加入的模块需要在Guice.createInjector中指定
 *
 * @param <T>
 */

public class DoveGuiceProvider<T> {

	private static Injector injector;

	static {
		injector = Guice.createInjector(new DbModule(), new DbModule());
	}

	public static Injector getInjector() {
		return injector;

	}

	public static <T> T getInstance(Class<T> clazz) {
		return injector.getInstance(clazz);
	}
}
