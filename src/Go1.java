package ryanaid;

import java.util.Calendar;
import java.util.Scanner;
import java.util.logging.*;
import java.util.regex.MatchResult;

import java.util.Map;

public class Go1  {

	private static Logger logger = Logger.getLogger("ryanair");
    public static void main(String[] args) {
    	RyanairLogic crawler = null;
    	try { 
	    	logger.log(Level.INFO, "Starting Browser");
	    	
	    	int wait_ms = 2000; // Wait after a click, ms
	    	int check_days = 3650; // By default check for the next 10 years
	    	//int check_days = 15; // By default check for the next 10 years
	    	
	    	//String from = "Kaunas (KUN)";
	    	//String to = "Edinburgh (EDI)";
	    	String from = "KUN";
	    	String to   = "GRO";
	    	//String to = "Frankfurt-Hahn (HHN)";

	    	Integer socks_port = 0;
	    	String socks_host = "";
	        
	    	Calendar date_from = Calendar.getInstance();
	    	date_from.add(Calendar.DAY_OF_YEAR, 1); // start tomorrow by default

	    	Map<String, String> env = System.getenv();

	    	if (env.containsKey("date_from")) {
	    		Scanner date_from_sc = new Scanner(env.get("date_from"));
	    		date_from_sc.findInLine("(\\d+)-(\\d+)-(\\d+)");
	    		MatchResult res = date_from_sc.match();
	    		date_from.set(
	    			Integer.parseInt(res.group(1)),
	    			Integer.parseInt(res.group(2))+1,
	    			Integer.parseInt(res.group(3))
	    			);
	    	}
	        
	    	if (env.containsKey("check_days")) check_days = Integer.parseInt(env.get("check_days"));
	    	if (env.containsKey("from")) 	   from = env.get("from");
	    	if (env.containsKey("to")) 		   to = env.get("to");
	        if (env.containsKey("wait_ms"))    wait_ms = Integer.parseInt(env.get("wait_ms"));

			Calendar dateTo = Calendar.getInstance();
			dateTo.add(Calendar.DAY_OF_YEAR, check_days); // add this # of days
			logger.log(Level.INFO, "Checking up to " + H.humanTime(dateTo));
			
	        if (env.containsKey("socks_host")) socks_host = env.get("socks_host");
	    	if (env.containsKey("socks_port")) socks_port = Integer.parseInt(env.get("socks_port"));
	    	try {
	    		crawler = new RyanairLogic("FirefoxFirebug", socks_port, socks_host);
	    	} catch (Exception e) {
	    		logger.log(Level.INFO, "FirefoxFirebug crashed, opening w/o one");
	    		crawler = new RyanairLogic("Firefox", socks_port, socks_host);
	    	}
	    	crawler.waitAfterClick = wait_ms; // Wait after clicking anything, ms
	    	crawler.getFlights(from, to, date_from);

	    	while (crawler.hasNextFlight()) {
	    		Flight flight = null;
	    		try {
	    			flight = crawler.nextFlight();
	    		} catch (PleaseFlightAgainException e) {
	    			logger.log(Level.WARNING, "Something went wrong fetching the flight, fetching another one");
	    			continue;
	    		} catch (PleaseMakeNewSearchException e) {
	    			logger.log(Level.INFO, e.getMessage());
	    			crawler.getFlights();
	    			continue;
	    		} catch (PleaseNextWeekException e) {
	    			crawler.next_week = true;
	    			continue;
	    		} catch (PleaseStopThisSearchException e) {
	    			logger.log(Level.SEVERE, e.getMessage());
	    			System.err.println(crawler.getCurrentUrl()+"\n\n"+crawler.getPageSource());
	    			crawler.close();
	    			System.exit(0);
	    		}
	    		System.out.println(flight);
	    		
	    		if (crawler.date_now.after(dateTo)) {
	    			logger.log(Level.INFO, "This was the last day we needed. Exiting");
	    			crawler.close();
	    			System.exit(0);
	    		}
	    		H.waitALittle(wait_ms);
	    	}
    	} catch (Exception e) {
    		logger.log(Level.SEVERE, "Application crashed. Here is the trace");
    		e.printStackTrace();
    		logger.log(Level.SEVERE, "Here is the HTML:");
    		try {
    			System.err.println(crawler.getCurrentUrl()+"\n\n"+crawler.getPageSource());
    		} catch (Exception e2) {
    			logger.log(Level.SEVERE, "Printing page source failed. Something is REALLY wrong here.");
    		}
    		crawler.close();
    		System.exit(1);
    	}
        crawler.close();
    }
}