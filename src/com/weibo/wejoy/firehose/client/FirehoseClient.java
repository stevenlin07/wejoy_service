package com.weibo.wejoy.firehose.client;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.weibo.wejoy.firehose.receiver.impl.GroupCommentReceiver;
import com.weibo.wejoy.firehose.receiver.impl.GroupStatusReceiver;
import com.weibo.wejoy.firehose.receiver.impl.RelationChangeReceiver;

public class FirehoseClient {

	public static void start() {
		String config = "firehose.xml";
		ApplicationContext ctx = null;
		try {
			ctx = new ClassPathXmlApplicationContext(new String[] { config });
			GroupCommentReceiver gcr = (GroupCommentReceiver) ctx.getBean("streamingForComment");
			gcr.init();

			GroupStatusReceiver gsr = (GroupStatusReceiver) ctx.getBean("streamingForStatus");
			gsr.init();

			RelationChangeReceiver rcr = (RelationChangeReceiver) ctx.getBean("streamingForRelationChange");
			rcr.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
