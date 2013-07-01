package com.weibo.wejoy.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import org.apache.commons.lang.StringUtils;

import cn.sina.api.commons.util.ApiLogger;

import com.weibo.wejoy.data.constant.DataConstants;

public class CommonUtil {
	
	private static ThreadLocal<CRC32> crc32Provider = new ThreadLocal<CRC32>() {
		@Override
		protected CRC32 initialValue() {
			return new CRC32();
		}
	};

	public static byte[] encode(String str) {
		try {
			return str.getBytes(DataConstants.DEFAULT_CHARSET);
		} catch (Exception e) {
			throw new RuntimeException("Error serializing String:" + str + " -> " + e);
		}
	}
	
	public static String decode(byte[] bytes, String charset){
		try {
			return new String(bytes, charset);
		} catch (Exception e) {
			throw new RuntimeException("Error deserializing bytes:" + Arrays.toString(bytes) + " -> " + e);
		}
	}

	public static int getHash4split(String id, int splitCount) {
		try {
			CRC32 crc = crc32Provider.get();
			crc.reset();
			crc.update(String.valueOf(id).getBytes("utf-8"));
			long h = crc.getValue();
			if (h < 0) {
				h = -1 * h;
			}
			int hash = (int) (h / splitCount % splitCount);
			return hash;
		} catch (UnsupportedEncodingException e) {
			ApiLogger.warn(new StringBuilder(64).append("Error: when hash4split, id=").append(id).append(", splitCount=")
					.append(splitCount), e);
			return -1;
		}
	}
	
	public static long parseLong(String value) {
		return parseLong(value, 0);
	}

	public static long parseLong(String value, String name) {
		return parseLong(value, 0, name);
	}

	public static long parseLong(String value, long defaultValue) {
		if (value == null || "".equals(value.trim()))
			return defaultValue;

		try {
			return Long.parseLong(value);
		} catch (Exception e) {
			ApiLogger.error(e);
		}

		return defaultValue;
	}

	public static long parseLong(String value, long defaultValue, String paramName) {
		if (value == null || "".equals(value.trim()))
			return defaultValue;

		try {
			return Long.parseLong(value);
		} catch (Exception e) {
			ApiLogger.error("parseLong error name=" + paramName + ",value=" + value, e);
		}

		return defaultValue;
	}


	public static int parseInteger(String value) {
		return parseInteger(value, 0);
	}

	public static int parseInteger(String value, int defaultValue) {
		if (value == null || "".equals(value.trim()))
			return defaultValue;

		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			ApiLogger.error(e);
		}

		return defaultValue;
	}
	
	public static boolean parseBoolean(String value){
		return parseBoolean(value, false);
	}
	
	public static boolean parseBoolean(String value, boolean defaultValue) {
		if (value == null || "".equals(value))
			return defaultValue;

		try {
			return Boolean.valueOf(value);
		} catch (Exception e) {
			ApiLogger.error(e);
		}

		return defaultValue;
	}
	
	public static List<String> strToList(String value){
		return strToList(value, ",");
	}
	
	public static List<String> strToList(String value, String split){
		List<String> ret = new ArrayList<String>();
		
		if(StringUtils.isBlank(value))
			 return ret;
		
		String[] arr = value.split(split);
		
		return arrToList(arr);
		
	}
	
	public static List<String> arrToList(String[] arr) {
		if (arr == null) return null;
		
		List<String> list = new ArrayList<String> ();
		
		for (int i = 0; i < arr.length ; i ++) {
			list.add(arr[i]);
		}
		
		return list;
	}
	
	public static void main(String[] args){
		System.out.println(strToList("180.149.138.88:6379,180.149.138.88:6379").toString());
	}
	

}
