package test.com.weibo.firehose;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.weibo.wejoy.firehose.receiver.impl.GroupCommentReceiver;

public class TestComments {

	
	
	@Test
	public void testComments() throws InterruptedException {
		
		List<String> streamingURLList = new LinkedList<String>();
		streamingURLList.add("http://10.75.5.83:8082/comet?appid=msn&amp;filter=grp_comment,*");
		int curStreamURLIndex = 0;
		
		GroupCommentReceiver gcr = new GroupCommentReceiver();
		gcr.setCurStreamURLIndex(curStreamURLIndex);
		gcr.setStreamingURLList(streamingURLList);
		gcr.init();
		Thread.currentThread().join();
	}
}
