package com.weibo.wejoy.utils;

import java.util.List;
/*
 * @author WeJoy Group
 */
public class JsonBuilder {
	
	private StringBuilder sb;
	private boolean flip = false;
	
	public JsonBuilder() {
		sb = new StringBuilder();
		sb.append("{");
	}
	
	public JsonBuilder(int initCapacity) {
		sb = new StringBuilder(initCapacity);
		sb.append("{");
	}
	
	public JsonBuilder(String content, boolean flip) {
		sb = new StringBuilder(content);
		this.flip = flip;
	}
	
	public JsonBuilder(JsonBuilder json) {
		sb = new StringBuilder(json.sb);
		flip = json.flip;
	}
	
	public JsonBuilder append(String name, String value) {
		if (name == null || value == null)
			return this;
		
		if (sb.length() > 1)
			sb.append(",");
		sb.append("\"").append(name).append("\":\"").append(toJsonStr(value)).append("\"");
		return this;
	}
	
	public JsonBuilder append(String name, long value) {
		if (sb.length() > 1)
			sb.append(",");
		sb.append("\"").append(name).append("\":").append(value);
		return this;
	}
	
	public JsonBuilder append(String name, int value) {
		if (sb.length() > 1)
			sb.append(",");
		sb.append("\"").append(name).append("\":").append(value);
		return this;
	}
	
	public JsonBuilder append(String name, JsonBuilder value) {
		return appendJsonValue(name, value == null ? null : value.toString());
	}
	
	public JsonBuilder appendJsonValue(String name, String jsonValue) {
		if (name == null || jsonValue == null)
			return this;
		
		if (sb.length() > 1)
			sb.append(",");
		sb.append("\"").append(name).append("\":").append(jsonValue);
		return this;
	}
	
	public JsonBuilder flip() {
		sb.append('}');
		flip = true;
		return this;
	}
	
	public JsonBuilder reset() {
		sb.setLength(0);
		sb.append("{");
		return this;
	}
	
	public JsonBuilder appendStrArr(String name, String[] value) {
		if (name == null)
			return this;
		
		if (sb.length() > 1)
			sb.append(",");
		sb.append("\"").append(name).append("\":");
		if (value != null){
			sb.append("[");
			int i=0;
			for(String s :value){
				if(i++>0){
					sb.append(",");
				}
				sb.append("\"").append(toJsonStr(s)).append("\"");
			}
			sb.append("]");
		}else{
			sb.append("[]");
		}
		
		return this;
	}
	
	public JsonBuilder appendJsonArr(String name, List<JsonBuilder> jsonArr) {
		if (name == null)
			return this;
		
		if (sb.length() > 1)
			sb.append(",");
		sb.append("\"").append(name).append("\":");
		if (jsonArr != null){
			sb.append("[");
			int i=0;
			for(JsonBuilder s :jsonArr){
				if(i++>0){
					sb.append(",");
				}
				sb.append(s.toString());
			}
			sb.append("]");
		}else{
			sb.append("[]");
		}
		
		return this;
	}
	
	/**
	 * 附加long数组到buffer中
	 * @param name 待添加的属性名称
	 * @param values	long数组
	 * @return	如果数组不为空，则返回按照","分割的字符串，否则返回"[]"
	 */
	public JsonBuilder appendLongArr(String name, long[] values) {
		if (name == null)
			return this;
		
		if (sb.length() > 1) {
			sb.append(",");
		}
		sb.append("\"").append(name).append("\":");
		if (values != null){
			sb.append("[");
			for(int i = 0; i < values.length; i++){
				if(i>0){
					sb.append(",");
				}
				sb.append(toJsonStr(String.valueOf(values[i])));
			}
			sb.append("]");
		}else{
			sb.append("[]");
		}
		
		return this;
	}
	
	public JsonBuilder appendStrArr(String name, long[] values) {
        if (name == null)
            return this;
        
        if (sb.length() > 1)
            sb.append(",");
        sb.append("\"").append(name).append("\":");
        if (values != null){
            sb.append("[");
            int i=0;
            for(long val : values){
                if(i++>0){
                    sb.append(", ");
                }
                sb.append("\"").append(String.valueOf(val)).append("\"");
            }
            sb.append("]");
        }else{
            sb.append("[]");
        }
        
        return this;
    }
	
	/**
	 * 附加Long列表到buffer中
	 * @param name 待添加的属性名称
	 * @param values	long列表
	 * @return	如果List不为空，则返回按照","分割的字符串，否则返回"[]"
	 */
	public JsonBuilder appendLongList(String name, List<Long> values) {
		if (name == null)
			return this;
		
		if (sb.length() > 1) {
			sb.append(",");
		}
		sb.append("\"").append(name).append("\":");
		if (values != null){
			sb.append("[");
			int i = 0;
			for(long value : values){
				if(i++>0){
					sb.append(",");
				}
				sb.append(toJsonStr(String.valueOf(value)));
			}
			sb.append("]");
		}else{
			sb.append("[]");
		}
		
		return this;
	}
	
	private static String toJsonStr(String value) {
		if (value == null)
			return null;
		boolean valid = true;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c < 32 || c == '"' || c == '\\' || c == '\n' || c == '\r' || c == '\t' || c == '\f' || c == '\b') {
				valid = false;
				break;
			}
		}
		if (valid)
			return value;
		
		StringBuilder buf = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
            switch(c) {
                case '"':
                	buf.append("\\\"");
                    break;
                case '\\':
                    buf.append("\\\\");
                    break;
                case '\n':
                    buf.append("\\n");
                    break;
                case '\r':
                    buf.append("\\r");
                    break;
                case '\t':
                    buf.append("\\t");
                    break;
                case '\f':
                    buf.append("\\f");
                    break;
                case '\b':
                    buf.append("\\b");
                    break;
                    
                default:
                	if (c < 32) {
                		buf.append("\\u00");
                		String str = Integer.toHexString(c);
                		if (str.length() == 1)
                			buf.append('0');
                		buf.append(str);
                	} else {
                		buf.append(c);
                	}
            }
		}
		return buf.toString();
	}

	@Override
	public String toString() {
		if (!flip)
			flip();
		return sb.toString();
	}
}