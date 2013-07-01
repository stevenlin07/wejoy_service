package com.weibo.wejoy.firehose.receiver;


public interface StreamingReceiver {
	public void processLine(String line);
}
