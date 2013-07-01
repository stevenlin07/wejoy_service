package com.weibo.wejoy.service.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class DivisionUids {
	
	
	public static List<String> divisionUids(HashSet<String> raw, int max) {
		Iterator<String> it = raw.iterator();
		List<String> list = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		int num =0;
		String value = null;
		while(it.hasNext()){
			value = (String) it.next();
			sb.append(value).append(",");
			num ++;
			if(num == max){
				num =0;
				list.add(sb.toString().substring(0,sb.toString().length()-1));
				sb.delete(0, sb.length());
			}
		}
		list.add(sb.toString().substring(0,sb.toString().length()-1));
		
		return list;
	}

}
