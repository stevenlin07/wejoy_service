package com.weibo.wejoy.firehose.receiver.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import cn.sina.api.commons.util.JsonWrapper;

import com.weibo.wejoy.firehose.receiver.StreamingReceiver;
import com.weibo.wejoy.firehose.util.ParseForFirehose;
import com.weibo.wesync.data.GroupOperationType;



/**
 *  author : xiaojing9@staff.sina.com.cn
 *  处理群组变更
 * 
 */

public class GroupChangeReceiver implements Runnable, StreamingReceiver {
	private final Logger log = Logger.getLogger(GroupChangeReceiver.class);
	private MultiThreadedHttpConnectionManager httpConnManager;
	private ExecutorService executor;
	private DataInputStream in;
	private HttpClient client = null;
	private byte[] recBuf;

	private int curStreamURLIndex;
	private List<String> streamingURLList;

	private final int recBufSize = 256;
	private int recIndex = 0;
	private long lastMsgLocation = -1L;
	
	public void init() {
		httpConnManager = new MultiThreadedHttpConnectionManager();
		httpConnManager.getParams().setMaxConnectionsPerHost(
				HostConfiguration.ANY_HOST_CONFIGURATION, 2);
		httpConnManager.getParams().setMaxTotalConnections(2);
		httpConnManager.getParams().setSoTimeout(Integer.MAX_VALUE);
		httpConnManager.getParams().setConnectionTimeout(10000);
		httpConnManager.getParams().setReceiveBufferSize(655350);

		client = new HttpClient(httpConnManager);

		new Thread(this).start();
		int threadCount=Runtime.getRuntime().availableProcessors();
		executor=Executors.newFixedThreadPool(threadCount);
	}

	public byte[] readLineBytes() throws IOException {
		byte[] result = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int readCount = 0;

		// some recbuf releave
		if (recIndex > 0 && read(bos)) {
			return bos.toByteArray();
		}

		while ((readCount = in.read(recBuf, recIndex, recBuf.length - recIndex)) > 0) {
			recIndex = recIndex + readCount;

			if (read(bos)) {
				break;
			}
		}

		result = bos.toByteArray();

		if (result == null
				|| (result != null && result.length <= 0 && recIndex <= 0)) {
			throw new IOException(
			"++++ Stream appears to be dead, so closing it down");
		}
		return result;
	}

	private boolean read(ByteArrayOutputStream bos) {
		boolean result = false;
		int index = -1;

		for (int i = 0; i < recIndex - 1; i++) {
			if (recBuf[i] == 13 && recBuf[i + 1] == 10) {
				index = i;
				break;
			}
		}

		if (index >= 0) {
			bos.write(recBuf, 0, index);

			byte[] newBuf = new byte[recBufSize];

			if (recIndex > index + 2) {
				System.arraycopy(recBuf, index + 2, newBuf, 0, recIndex
						- index - 2);
			}

			recBuf = newBuf;
			recIndex = recIndex - index - 2;

			result = true;
		} else {
			if (recBuf[recIndex - 1] == 13) {
				bos.write(recBuf, 0, recIndex - 1);
				Arrays.fill(recBuf,(byte)0);
				//recBuf = new byte[recBufSize];
				recBuf[0] = 13;
				recIndex = 1;
			} else {
				bos.write(recBuf, 0, recIndex);
				Arrays.fill(recBuf,(byte)0);
				//recBuf = new byte[recBufSize];
				recIndex = 0;
			}
		}
		return result;
	}

	public void run() {
		log.debug("start to parse firehose msg from " + streamingURLList);
		while(true){
			GetMethod method=null;
			recIndex = 0;
			recBuf = new byte[recBufSize];
			try{
				method = connectStreamServer();
				
				while(true){
					String line = new String(readLineBytes());
					processLine(line);
				}
			}
			catch(StreamingException se) {
				curStreamURLIndex=++curStreamURLIndex % streamingURLList.size();
				log.error("streaming connect or read error",se);
			}
			catch(Exception e) {
				log.error("streaming process error ",e);
			}
			finally {
				try {
					if(lastMsgLocation!=-1){
						saveReadLocation( ++ lastMsgLocation);
					}
					if (method != null){
						method.releaseConnection();
					}
				} catch (Exception e1) {
					log.error(e1);
				}
			}
		}
	}
	
	
	@Override
	public void processLine(String line) {

		try{
			//System.out.println(line);
			executor.execute(new MessageProcessor(line));
		}catch(Exception e){
			log.error("firehose解析错误");
		}
		
	}
	
	
	private GetMethod connectStreamServer() throws StreamingException {
		GetMethod method;
		String targetURL=streamingURLList.get(curStreamURLIndex);
		
		if(lastMsgLocation > 0) {
			targetURL+= ("&loc=" + lastMsgLocation);
		}

		log.info("StreamingReceiver http get url="+targetURL);
		method = new GetMethod(targetURL);
		int statusCode;
		try {
			statusCode = client.executeMethod(method);
		} catch(Exception e){
			throw new StreamingException("stream url connect failed", e);
		}

		if (statusCode != HttpStatus.SC_OK) {
			throw new StreamingException("bad streaming url response code!!!");
		}
		log.info("connect to the streaming server OK");
		try {
			in =new DataInputStream(method.getResponseBodyAsStream());
		} catch (IOException e) {
			throw new StreamingException("get stream input io exception", e);
		}
		return method;
	}

	private void saveReadLocation(long location) {
		// TO DO
	}

	
	class MessageProcessor implements Runnable {
		private final String  line;
		public MessageProcessor(String line){
			this.line = line;
		}

	public void run() {	
		try{	//分组变更
			JsonWrapper json = new JsonWrapper(line);
			String groupType = json.getNode("text").get("type");
			String event = json.getNode("text").get("event");	
			long listId = json.getNode("text").getNode("user_group").getLong("gid");	
			//删除组中成员
				if(groupType.equals("user_group") && event.equals("del_group_member")){
					GroupOperationType type = GroupOperationType.removeMember;
					ParseForFirehose.processGroupChange(line, type, listId);
				//添加组中成员	
				}else if(groupType.equals("user_group") && event.equals("add_group_member")){
					GroupOperationType type = GroupOperationType.addMember;
					ParseForFirehose.processGroupChange(line, type, listId);
					//删除整个组
				}else if(groupType.equals("user_group") && event.equals("delete_group")){
					ParseForFirehose.processGroupChangeForQuit(line, listId);
				}else{
					//其他情况不做处理
				}
			}catch(Exception e){
				log.error("[GroupChangeReceiver] 处理分组变更错误", e);
			}			
		} 
	}
	
	public void setCurStreamURLIndex(int curStreamURLIndex) {
		this.curStreamURLIndex = curStreamURLIndex;
	}

	public void setStreamingURLList(List<String> streamingURLList) {
		this.streamingURLList = streamingURLList;
	}

	
	@SuppressWarnings("serial")
	private class StreamingException extends Exception {

		public StreamingException(String string, Exception e) {
			super(string, e);
		}

		public StreamingException(String string) {
			super(string);
		}
	}
	

	/*
	 * 分组变更消息格式      删除分组事件：delete_group     添加分组成员事件add_group_member    移除分组成员事件del_group_member
	{"id":1301186000000017,"text":{"type":"user_group","event":"add_group_member","user_group":{"uid":1734528095,"gid":3458654717744541,"members":[2477896162]}}}

*/
	//static  String message = "{\"id\":1301186000000017,\"text\":{\"type\":\"user_group\",\"event\":\"add_group_member\",\"user_group\":{\"uid\":1734528095,\"gid\":3458654717744541,\"members\":[2477896162]}}}";

}

