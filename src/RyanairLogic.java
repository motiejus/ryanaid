package ryanaid;

import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.logging.*;
import java.util.regex.MatchResult;
import java.io.File;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.Select;

public class RyanairLogic {
	//private HtmlUnitDriver driver;
	private WebDriver driver;
	
	private String from;
	private String to;
	
	public Calendar date_now; // was: day, month, year
	
	private int procDaysThisWeek = 0;
	private int daysWithFlightsThisWeek = 0;
	private int procFlightsToday = 0;
	private boolean new_search = true;
	public boolean next_week = false;
	
	public static final String firebugVersion = "1.6.0";
	public static final String firebugPath = "/home/motiejus-gentoo/stuff/firebug-"+firebugVersion+"-fx.xpi";
	public int waitAfterClick;
	
    private static Logger logger = Logger.getLogger("ryanair.crawler");

    public static enum Months {Nil, Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec};

	// Browser on. Only Firefox supported (yet).
    public RyanairLogic(String browser, Integer socks_port, String socks_host) throws Exception {
        FirefoxProfile profile = new FirefoxProfile();
        
        if (socks_port != 0) {
        	profile.setPreference("network.proxy.type", 1); // manual configuration
        	profile.setPreference("network.proxy.socks_port", socks_port);
        	profile.setPreference("network.proxy.socks", socks_host);
        }
		profile.setPreference("network.image.imageBehavior", 2);

		if (browser.equals("FirefoxFirebug")) {
	        try {
				profile.addExtension(new File(firebugPath));
				profile.setPreference("extensions.firebug.currentVersion", "1.6.0");
				profile.setPreference("extensions.firebug.net.enableSites", true);
				profile.setPreference("extensions.firebug.console.enableSites", true);
				profile.setPreference("extensions.firebug.script.enableSites", true);
				//profile.setPreference("extensions.firebug.allPagesActivation", "on");
				// 0 - load all; 1 - from same site; 2 - load no images
				driver = new FirefoxDriver(profile);
	        } catch (Exception e) {
				System.err.println("Firebug extension not found");
				throw e;
			}
        } else {
        	logger.log(Level.INFO, "Launching firefox w/o firebug");
			driver = new FirefoxDriver(profile);
        }
	}
	
	public void getFlights(String from, String to, Calendar date_from) {
		new_search = true;
		this.from = from;
		this.to = to;
		date_now = (Calendar) date_from.clone();
	}
	
	public void getFlights() {
		new_search = true;
	}

	private void checkIfThePageIsOk() throws PleaseMakeNewSearchException, PleaseStopThisSearchException, PleaseNextWeekException {
		try {
			if (!driver.getPageSource().contains("Select A Flight")) {
				// something is wrong.
//				if (driver.getPageSource().contains("Sorry, there are no available flights departing on the")) {
				boolean stop = false;
				boolean next_week = false;
				
				try {
					driver.findElement(By.id("ttable1"));
					logger.log(Level.WARNING, "No flights on this page, but table is present. Advancing to next week.");
					next_week = true;
				} catch (NoSuchElementException e) {
					if (driver.getPageSource().contains("Select and Continue")) {
						stop = true;
					}
				}
				if (stop) {	throw new PleaseStopThisSearchException("No flights on the page at all.");	}
				if (next_week) { throw new PleaseNextWeekException(); }
				waitALittle();
				throw new PleaseMakeNewSearchException("We are screwed, the page is just not ok.");
			}
		}
		catch (UnhandledAlertException e) {
			throw new PleaseMakeNewSearchException("Unhandled alert in pageIsFine");
		}
	}
	
	public Flight nextFlight() throws PleaseMakeNewSearchException, PleaseStopThisSearchException, PleaseNextWeekException, PleaseFlightAgainException {
		// find out where are we
		String url = driver.getCurrentUrl();

		// simple check if we have page as expected
		if (!url.contains("FRSelect.aspx") || new_search) {
			logger.log(Level.INFO, "Starting new search");
			newSearch(); waitALittle();
		}

		// This week is over or other requested
		if (next_week || daysWithFlightsThisWeek != 0 && daysWithFlightsThisWeek == procDaysThisWeek) {
			logger.log(Level.INFO, "Going to next week, processed days this week: "+procDaysThisWeek);
			nextWeek(); waitALittle();
		}
		checkIfThePageIsOk();
		
		List<WebElement> days = driver.findElements(By.xpath("//th[ not(div[@class='planeNoFlights']) ]"));
		daysWithFlightsThisWeek = days.size();
		if (procDaysThisWeek == 0 && procFlightsToday == 0) {
			logger.log(Level.INFO, "Searching for day available and clicking it");
			days.get(0).click(); waitALittle();
			checkIfThePageIsOk();
		}

		// find out if days[procDaysThisWeek] is clicked
		String cls = driver.findElement(By.xpath(
				"//th[ not(div[@class='planeNoFlights']) ][position() = "+(procDaysThisWeek+1)+"]")
				).getAttribute("class");
		
		if (!cls.contains("on")) {
			logger.log(Level.INFO, "Clicking on day "+procDaysThisWeek);
			driver.findElements(By.xpath("//th[ not(div[@class='planeNoFlights']) ]")).get(procDaysThisWeek).click();
			procFlightsToday = 0;
		}
		
		checkIfThePageIsOk();
		int flightsTodaySize = driver.findElements(By.xpath("//input[@type='radio']")).size();

		boolean todayIsSelected = driver.findElements(By.xpath("//input[@type='radio']")).get(procFlightsToday).isSelected();
		
		// Select the desired flight TODAY if there is more than one 
		if (flightsTodaySize != 1 && !todayIsSelected) {
			logger.log(Level.INFO, "Clicking on flight "+procFlightsToday +" day "+ procDaysThisWeek);
			driver.findElements(By.xpath("//input[@type='radio']")).get(procFlightsToday).click();
			waitALittle();
		}
		
		// We are on the flight that we are interested in

		// Sleep until the div we want is visible or 5 seconds is over
        long end = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < end) {
        	try {
        		String status = driver.findElement(By.xpath("//p[@class='hdr']")).getText(); 
        		if (status.contains("Please")) {
            		throw new NoSuchElementException("Not loaded yet");
        		}
        		if (status.contains("Unavailable")) {
        			// we are screwed, make new search
        			logger.log(Level.WARNING, "We are screwed, requesting new search");
        			throw new PleaseMakeNewSearchException("we are screwed");
        		}
        		WebElement priceDiv = driver.findElement(By.id("totalPrice"));
            	if (priceDiv.getText().contains("Please")) {
            		throw new NoSuchElementException ("Not loaded yet");
            	}
        	} catch (StaleElementReferenceException e) {
        		H.waitALittle(50);
        		continue;
        	} catch (NoSuchElementException e) {
        		H.waitALittle(50);
        		continue;
        	}
        	break; // Price found
        }
        
        Flight flight = null;
        try {
        	flight = getFlightInfo(flightsTodaySize);
        } catch (RuntimeException e) {
        	// proceed to next day/flight/whatever
            procFlightsToday++;
            if (procFlightsToday == flightsTodaySize) {
            	procDaysThisWeek++; // this day is over
            }
            throw new PleaseFlightAgainException("Something went wrong while fetching flight info");
        }

        procFlightsToday++;
        if (procFlightsToday == flightsTodaySize) {
        	procDaysThisWeek++; // this day is over
        }
        return flight;
	}
	
	public Flight getFlightInfo(int flightsTodaySize) {

        String price = driver.findElement(By.id("totalPrice")).getText(); 
        String flightDetails = driver.findElement(By.xpath("//tr[@class='on']/td[7]")).getText();
        String seatsLeft_str = "";
        int seatsLeft = 0;
        try {
        	seatsLeft_str = driver.findElement(By.xpath("//tr[@class='on']/td[5]//div")).getText();
        	Scanner tempscan = new Scanner(seatsLeft_str);
        	tempscan.findInLine("(\\d+) seats left");
        	MatchResult res = tempscan.match();
        	seatsLeft = Integer.parseInt(res.group(1));
        } catch (NoSuchElementException e) { }
        
        String flightTimes = driver.findElement(By.xpath("//tr[@class='on']/td[8]")).getText();
        
        
    	Scanner l = new Scanner(flightDetails);
    	//					  day	 month	 year
    	l.findInLine("^\\w{3}(\\d+) (\\w{3}) (\\d+)");
    	MatchResult mr = l.match();
    	
    	date_now.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mr.group(1)));
    	date_now.set(2000+Integer.parseInt(mr.group(3)),
    			MonthStrToNumber(mr.group(2))-1,
    			Integer.parseInt(mr.group(1)));
    	Scanner departTime_sc = new Scanner(flightTimes.split("\n")[0]);
        departTime_sc.findInLine("(\\d+):(\\d+) Depart");
        MatchResult mr1 = departTime_sc.match();
        date_now.set(Calendar.HOUR_OF_DAY, Integer.parseInt(mr1.group(1)));
        date_now.set(Calendar.MINUTE, Integer.parseInt(mr1.group(2)));
        Scanner returnTime_sc = new Scanner(flightTimes.split("\n")[1]);
        returnTime_sc.findInLine("(\\d+:\\d+) Arrive");
        MatchResult mr2 = returnTime_sc.match();
        
    	return new Flight(from, to, date_now, mr2.group(1), price, seatsLeft);	
	}
	
	public void nextWeek() {
		driver.findElement(By.xpath("//a[contains(text(), 'Next Week')]")).click();
		procFlightsToday = 0;
		procDaysThisWeek = 0;
		next_week = false;
		// TODO: 1 is only for insurance. In reality,
		// one should parse the real previously clicked
		// day and set the value accordingly.
		date_now.add(Calendar.DAY_OF_YEAR, 1);
	}
	
	private void newSearch() throws PleaseStopThisSearchException {
        String day_str = H.twoDigitNum(date_now.get(Calendar.DAY_OF_MONTH));
        String monthyear = Months.values()[date_now.get(Calendar.MONTH)+1] + " " + date_now.get(Calendar.YEAR);
        logger.log(Level.INFO, "Day: "+day_str+", monthyear: "+monthyear);
        try {
        	driver.manage().deleteAllCookies();
        } catch (WebDriverException e) { 
        	logger.log(Level.WARNING, "Cookies not removed, but should've been.");
        }
		
        waitALittle();
		driver.get("http://www.bookryanair.com/skysales/FRSearch.aspx");
		//driver.get("http://www.ryanair.com/en/booking/form");
        
        driver.findElement(By.xpath("//label[text() = 'One Way']")).click();
        //driver.findElement(By.id("tt2")).click(); // FIXME: for HtmlUnit, since it couldn't find one above :/
        
        try {
        	driver.findElement(By.xpath("//label[contains(text(),'travel dates are flexible')]")).click();
        } catch (NoSuchElementException e) {
        	driver.findElement(By.id("SearchBy")).setSelected();
        }
        
        new Select(driver.findElement(By.xpath(
        		"//select[option[text() = 'Origin' or text() = 'Departing from'] ]"))).selectByValue(from); // from
        Select to_sel = 
        new Select(driver.findElement(By.xpath(
        		"//select[option[text() = 'Destination' or text() = 'Going to'] ]"))); // to
        to_sel.selectByValue(to);
        
        if (!to_sel.getAllSelectedOptions().get(0).getText().contains(to)) {
        	throw new PleaseStopThisSearchException("No such TO destination!");
        }
        
        // TODO change to human readable
        new Select(driver.findElement(By.xpath( // from - day
        		"//select[@id='AvailabilitySearchInputFRSearchView_DropDownListMarketDay1' or @id='sector_1_d']"))).selectByVisibleText(day_str);
        new Select(driver.findElement(By.xpath( // from - monthyear
        		"//select[@id='AvailabilitySearchInputFRSearchView_DropDownListMarketMonth1' or @id='sector_1_m']"))).selectByVisibleText(monthyear);

        driver.findElement(By.xpath(
        		"//input[@id=//@for[parent::label [contains(text(), 'I have read and accept')] ]]")).setSelected();
        
        // Ugly.. Must disagree the spanish javascript "confirm"
        ((JavascriptExecutor) driver).executeScript("window.confirm = function(msg) { return false; }");

        // Now submit the form.
        try {
        	driver.findElement(By.xpath("//input[@type='submit']")).click();
        } catch (NoSuchElementException e) {
        	driver.findElement(By.xpath("//button[contains(text(), 'Book Cheap Flights')]")).click();
        }
        new_search = false;
        waitALittle();
        
        /* Find out which day we should start with. 
         * Ryanair usually throws us to the middle of the week, 
         * so we don't need to process those first flights.
         */
		String url = driver.getCurrentUrl();
		if (url.contains("FRSearch.aspx")) {
			throw new PleaseStopThisSearchException("We are stuck in select, probably no dst flight.");
			
		}
        
        List<WebElement> allDays = driver.findElements(By.xpath("//th"));
        procDaysThisWeek = -1;
        boolean found_needed = false;
        for (WebElement day: allDays) {
        	// Format of date is Tue, 14 Dec 10
        	if (day.getText().contains(day_str+" "+Months.values()[date_now.get(Calendar.MONTH)+1]))
        		found_needed = true;

        	try {
        		day.findElement(By.xpath("div[@class='planeNoFlights']"));
        	} catch (NoSuchElementException e) { // we don't have div that indicates "no flights"
        		procDaysThisWeek++;
        		if (found_needed)
        			break;
        	}
        }
	}
	
	public boolean hasNextFlight() {
		return true;
	}

   public static int MonthStrToNumber(String month) {
    	int i = 0;
    	for (Months m : Months.values()) {
    		if (m.toString().equals(month))
    			return i;
    		i++;
    	}
    	throw new RuntimeException("Month not found for this index: "+i);
    }

    public void waitALittle() {
   		H.waitALittle(waitAfterClick);
    }
 
    public String getPageSource() {
    	return driver.getPageSource();
    }
    public String getCurrentUrl () {
    	return driver.getCurrentUrl();
    }
    
    public void close() {
    	driver.close();
    }
}