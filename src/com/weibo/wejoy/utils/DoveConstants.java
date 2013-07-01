package com.weibo.wejoy.utils;

public class DoveConstants {

	/** 慢操作的时间区间定义 **/
	public static final int OP_TIMEOUT_H = 500;
	public static final int OP_TIMEOUT_L = 100;

	/** 数据库表类型 **/
	// public static final String LIST_TO_GROUP_MAPPING = "list2group"; //
	// 定向分组和群聊id映射
	public static final String LIST_TO_GROUP_MAPPING = "weiju_1"; // 定向分组和群聊id映射

	/** 数据库相关常量 **/
	public static final int BATCH_INSERT_SIZE = 200; // 批量插入大小

	/** mc 相关常量 **/
	public static final int CONTACT_META_MC_SIZE = 32; // contactMeta在mc占的字节数
	public static final int CONTACT_MC_SIZE = 16; // contact在mc占的字节数
	public static final int CONTACT_MAX_MC_SIZE = 200; // mc 中 contact列表的最大长度

	/** 通讯录业务逻辑相关 **/
	public static final int MAX_CONTACT_SIZE = 2000; // 通讯录最大长度
	public static final long EXPIRE_TIME = 30 * 24 * 3600 * 1000l; // 通讯录匹配额结果的过期时间
	public static final int MAX_FOLLOW_COUNT = 50000; // 最大的粉丝数
	public static final String SEPARATE = ","; // 通讯录分隔符

	/** 依赖到外部接口相关 **/
	public static final int MAX_REQUEST_MULTIUID_LENGTH = 90; // 请求外部接口multuid参数的最大长度

	public static final int MAX_REQUEST_BAMISSO_LENGTH = 29; // 请求外部接口bamisso参数的最大长度

	public static final int REQUEST_TIME_OUT = 2000; // 请求接口超时时间

	public static final String REQUEST_MULTIUID_TIME_OUT = "timeout_multiuid";
	public static final String REQUEST_BAMISSO_TIME_OUT = "timeout_bamisso";

	// 新增号码
	public static final String UPLOAD_NEW_SIGN = "+"; // 或不带任何标志
	// 删除号码
	public static final String UPLOAD_DELETE_SIGN = "-";

	public static final int MAX_REQUEST_WEMI_SIZE = 50; // 批量请求微米小平台接口长度限制

	/**
	 * 缓存后缀
	 * 
	 * @author maijunsheng
	 * 
	 */
	public enum ContactCacheSuffix {
		/** 通讯录信息 **/
		CONTACT_META(".cm"),
		/** 通讯录列表 **/
		CONTACT(".ct"),
		/** 上传标记 **/
		CONTACT_UPLOAD(".cu");

		private ContactCacheSuffix(String suffix) {
			this.suffix = suffix;
		}

		public String value() {
			return suffix;
		}

		private String suffix;
	}
}
