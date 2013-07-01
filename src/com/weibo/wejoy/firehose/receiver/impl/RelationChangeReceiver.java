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
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.sina.api.commons.util.JsonWrapper;

import com.weibo.wejoy.firehose.receiver.StreamingReceiver;
import com.weibo.wejoy.firehose.util.GetGids;
import com.weibo.wejoy.firehose.util.ParseForFirehose;
import com.weibo.wejoy.group.dao.RegisterDao;

/**
 *  author : xiaojing9@staff.sina.com.cn
 *  处理关系变更
 * 
 */

public class RelationChangeReceiver implements Runnable, StreamingReceiver {
	private final static Logger log = Logger.getLogger(RelationChangeReceiver.class);
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
	
	private static RegisterDao registerDao;

	static {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"firehose.xml"});
		registerDao = (RegisterDao)ctx.getBean("registerDao");
	}


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
		private String  line;
		public MessageProcessor(String line){
			this.line = line;
		}

	public void run() {	
		try{
			//关系变更
			JsonWrapper json = new JsonWrapper(line);
			if(ParseForFirehose.isRelationChange(json)){	
				//source代表取消关注或移除粉丝动作的操作者的id，对应的，target是被操作者的id
				String source = ParseForFirehose.getFromuidFromRelationChange(json);	
				String target = ParseForFirehose.getUidFromRelationChange(json);
				boolean sourceRegistered = registerDao.isRegistered(source);
				boolean targetRegistered = registerDao.isRegistered(target);
				
				if(sourceRegistered || targetRegistered){
					//从mc中获取所有source及target创建的聊天群,不成功取db
					List<String> gidsBelongedSource = GetGids.getGids(source);
					List<String> gidsBelongedTarget = GetGids.getGids(target);
					
					if(gidsBelongedSource.size() != 0){					
						ParseForFirehose.processRelationChange(gidsBelongedSource,source, target);
					}
					
					if(gidsBelongedTarget.size() != 0){
						ParseForFirehose.processRelationChange(gidsBelongedTarget, target, source);	
					}
				}
			}
		}catch(Exception e){
			log.error("[RelationChangeReceiver] 处理关系变更错误  " + e);
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
	
}

