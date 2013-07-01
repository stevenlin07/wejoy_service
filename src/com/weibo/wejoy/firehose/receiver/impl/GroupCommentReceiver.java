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
import cn.sina.api.data.storage.cache.MemCacheStorage;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weibo.wejoy.firehose.receiver.StreamingReceiver;
import com.weibo.wejoy.firehose.util.ParseForFirehose;
import com.weibo.wejoy.firehose.util.ParseResult;
import com.weibo.wejoy.group.dao.List2GroupDao;
import com.weibo.wejoy.group.dao.RegisterDao;
import com.weibo.wejoy.group.module.DbModule;
import com.weibo.wejoy.wesync.WesyncApiImpl;

/**
 * author : xiaojing9@staff.sina.com.cn 处理定向评论
 * 
 */

@SuppressWarnings("unchecked")
public class GroupCommentReceiver implements Runnable, StreamingReceiver {
	private final Logger log = Logger.getLogger(GroupCommentReceiver.class);
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
	private static List2GroupDao list2GroupDao;
	private static RegisterDao registerDao;
	private static MemCacheStorage<String> memCacheStorage;

	static {
		Injector injector = Guice.createInjector(new DbModule());
		list2GroupDao = injector.getInstance(List2GroupDao.class);
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				new String[] { "firehose.xml" });
		memCacheStorage = (MemCacheStorage<String>) ctx
				.getBean("memCacheStorage");
		registerDao = (RegisterDao) ctx.getBean("registerDao");

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
		int threadCount = Runtime.getRuntime().availableProcessors();
		executor = Executors.newFixedThreadPool(threadCount);
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
				System.arraycopy(recBuf, index + 2, newBuf, 0, recIndex - index
						- 2);
			}

			recBuf = newBuf;
			recIndex = recIndex - index - 2;

			result = true;
		} else {
			if (recBuf[recIndex - 1] == 13) {
				bos.write(recBuf, 0, recIndex - 1);
				Arrays.fill(recBuf, (byte) 0);
				// recBuf = new byte[recBufSize];
				recBuf[0] = 13;
				recIndex = 1;
			} else {
				bos.write(recBuf, 0, recIndex);
				Arrays.fill(recBuf, (byte) 0);
				// recBuf = new byte[recBufSize];
				recIndex = 0;
			}
		}
		return result;
	}

	public void run() {
		log.debug("start to parse firehose msg from " + streamingURLList);
		while (true) {
			GetMethod method = null;
			recIndex = 0;
			recBuf = new byte[recBufSize];
			try {
				method = connectStreamServer();

				while (true) {
					String line = new String(readLineBytes());
					processLine(line);
				}
			} catch (StreamingException se) {
				curStreamURLIndex = ++curStreamURLIndex
						% streamingURLList.size();
				log.error("streaming connect or read error", se);
			} catch (Exception e) {
				log.error("streaming process error ", e);
			} finally {
				try {
					if (lastMsgLocation != -1) {
						saveReadLocation(++lastMsgLocation);
					}
					if (method != null) {
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

		try {
			executor.execute(new MessageProcessor(line));
		} catch (Exception e) {
			log.error("firehose解析错误");
		}

	}

	private GetMethod connectStreamServer() throws StreamingException {
		GetMethod method;
		String targetURL = streamingURLList.get(curStreamURLIndex);
		if (lastMsgLocation > 0) {
			targetURL += ("&loc=" + lastMsgLocation);
		}

		log.info("StreamingReceiver http get url=" + targetURL);
		method = new GetMethod(targetURL);
		int statusCode;
		try {
			statusCode = client.executeMethod(method);
		} catch (Exception e) {
			throw new StreamingException("stream url connect failed", e);
		}

		if (statusCode != HttpStatus.SC_OK) {
			throw new StreamingException("bad streaming url response code!!!");
		}
		log.info("connect to the streaming server OK");
		try {
			in = new DataInputStream(method.getResponseBodyAsStream());
		} catch (IOException e) {
			throw new StreamingException("get stream input io exception", e);
		}
		return method;
	}

	private void saveReadLocation(long location) {
		// TO DO
	}

	class MessageProcessor implements Runnable {
		private final String line;

		public MessageProcessor(String line) {
			this.line = line;
		}

		public void run() {
			WesyncApiImpl wesyncInstance = WesyncApiImpl.getInstance();
			JsonWrapper json = null;
			try {
				json = new JsonWrapper(line);
			} catch (Exception e) {
				log.error("[GroupCommentReceiver] firehose处理定向评论出错", e);
				return;
			}
			if (!ParseForFirehose.isOrientGroupComment(json)) {
				return;
			}
			if (ParseForFirehose.getCommentSource(json).contains("微聚")) {
				return; // 过滤到此条微博，为了防止闭环
			}

			// 将此条评论转群聊的生效条件：fromuid是注册用户，且commentUid和fromuid是互粉好友
			String fromuid = ParseForFirehose.getUidFromComment(json);
			String commentUid = ParseForFirehose.getCommentUid(json);
			boolean hasRegistered = registerDao.isRegistered(fromuid);
			if (!hasRegistered) {
				return;
			}
			boolean isBilaterRelation = ParseForFirehose.isBilateralFans(fromuid, commentUid);
			if ( !fromuid.equals(commentUid)) { 
				if (!isBilaterRelation ) { // 还不是双向关注
					return;
				}
			} else {
				// 自己给自己评论的情况
			}
			
			ParseResult parseResult = ParseForFirehose.parseComment(json, commentUid);
			if (!parseResult.flag) {
				return;
			}
			StringBuilder sb = new StringBuilder();
			sb.append(parseResult.listId).append(",").append(parseResult.statusId);
			String gid = memCacheStorage.get(sb.toString());
			if (null == gid) {
				gid = list2GroupDao.getGroupId(parseResult.listId,
						parseResult.statusId, fromuid);
				if (null == gid) {// 一条定向评论来了，但群还没建好，丢掉此条消息
					log.warn("[group comment]从定向微博评论得到群聊id错误");
					return;
				}
			}
			// 给群发消息
			wesyncInstance.sendToGroup(parseResult.commentUid, gid, parseResult.commentContent);
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
