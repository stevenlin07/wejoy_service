package test.com.weibo.dove;

import org.junit.Test;

import com.weibo.wejoy.service.util.DoveType;

public class TestDoveType {

	@Test
	public void testDove() {
		String str = "chatInfos";
		System.out.println(DoveType.valueOfStr(str).name());
	}
}
