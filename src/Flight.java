package ryanaid;

import java.util.Calendar;
import java.util.Scanner;
import java.util.regex.MatchResult;

public class Flight {
	public Calendar departDate;
	public Calendar returnTime;
	public Calendar created;
	public int seatsLeft = 0; // 0 = max
	public String price;
	public String from;
	public String to;
	
	public Flight(String from, String to, Calendar departDate, String returnTimeStr, String price, int seatsLeft) {
		created = Calendar.getInstance();
		
		this.departDate = (Calendar) departDate.clone();
		returnTime = (Calendar) departDate.clone();
		// returnTimeStr is HH:MM format
		Scanner returnTime_sc = new Scanner(returnTimeStr);
		returnTime_sc.findInLine("(\\d+):(\\d+)");
		MatchResult rt = returnTime_sc.match();
		returnTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(rt.group(1)));
		returnTime.set(Calendar.MINUTE, Integer.parseInt(rt.group(2)));
		
		if (returnTime.before(departDate)) {
			// Lands after midnight or going east (time zone change).
			// If time difference is more than 12 hours,
			// then tomorrow
			// else - do nothing.
			long diff = departDate.getTimeInMillis() - returnTime.getTimeInMillis(); 
			if (diff > 1000 * 60 * 60 * 12) {
				returnTime.add(Calendar.DAY_OF_YEAR, 1);
			}
		}
		
		// counting return time.
		this.price = price;
		this.seatsLeft = seatsLeft;
		this.from = from;
		this.to = to;
	}
	public String toString() {
		return from+"-"+to+"|"+H.humanTime(departDate)+"|"+H.humanTime(returnTime)+
					       "|"+price+"|"+seatsLeft+"|"+H.humanTimeSec(created);
	}
}