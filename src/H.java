package ryanaid;

import java.util.Calendar;

public class H {
	public static String twoDigitNum(int number) {
		String number_str = number+"";
		return number_str.length() == 1? "0"+number_str : number_str;
	}

    public static void waitALittle(int little) {
    	if (little > 0) {
    		try {Thread.sleep(little);} catch (InterruptedException e1) {}
    	}
    }
    public static String humanTime(Calendar c) {
    	return c.get(Calendar.YEAR)+"-"+
		H.twoDigitNum(c.get(Calendar.MONTH)+1)+"-"+
		H.twoDigitNum(c.get(Calendar.DAY_OF_MONTH)) + " "+
		H.twoDigitNum(c.get(Calendar.HOUR_OF_DAY)) + ":"+
		H.twoDigitNum(c.get(Calendar.MINUTE));
    }
    public static String humanTimeSec(Calendar c) {
    	return humanTime(c)+":"+H.twoDigitNum(c.get(Calendar.SECOND));
    }
}
