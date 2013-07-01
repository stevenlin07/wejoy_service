package com.weibo.wejoy.service.util;

public class GroupName {

	 

    private static final String str = "QWERTYUIOPASDFGHJ+KLZXCVBNM9753108642mnbvcxz=lkjhgfdsapoiuytrewq";

    private static final byte[] bytes = str.getBytes();

    private static final long MAX = 281474976710656L;

    private static long[] mask = new long[48];

    

    static {

        for (int i = 0; i < 48; ++i) {

            mask[i] = (1L << i);

        }

    }

 

    public static String timeToStr(long t) {

        if (t < 0 || MAX <= t) return null;

        byte[] bts = new byte[8];

        for (int i = 0; i < 48; ++i) {

            bts[i & 0x07] |= ((t & mask[i]) >>> (i - (i >>> 3)));

        }

        for (int i = 0; i < 8; ++i) {

            bts[i] = bytes[bts[i]];

        }

        return new String(bts);

    }

    

    /**

    * @param args

    */
/*    public static void main(String[] args) {

        long v = System.currentTimeMillis();
		String request = String.format("name=微聚%s",timeToStr(v));

       System.out.println(request);

    }*/

 

}

 
