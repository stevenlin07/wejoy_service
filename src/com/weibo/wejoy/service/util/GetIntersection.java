package com.weibo.wejoy.service.util;

import java.util.HashSet;
import java.util.Iterator;

public class GetIntersection {
	
	//TODO 改为模板类型
	public static HashSet<String> getIntersectionOf2Set(HashSet<String> set1, HashSet<String> set2){
		HashSet<String> intersection = new HashSet<String>();
		Iterator<String> iterator1 =set1.iterator();	
		Iterator<String> iterator2 =set2.iterator();
		
		while(iterator1.hasNext()){
			String v1 = iterator1.next();
			iterator2 = set2.iterator();
			while(iterator2.hasNext()){
				String v2 = iterator2.next();
				if(v1.equals(v2)){
					intersection.add(v1);
				}
			}
		}	
		return intersection;
	}
	
}


