package business;

import javax.jws.*;
import components.data.*;
import java.util.*;
import org.joda.time.*;
import java.text.SimpleDateFormat;

public class BusinessLayer {
	
	public static final String OPEN_TIME = "8:00:00";
	public static final int OPEN_LENGTH = 540; // how long office is open in minutes
	public static final int APPT_LENGTH = 15; // appt length in minutes
	public static final int APPT_GAP = 30; // required gap between appts in minutes
	private static java.sql.Date apptDate; // the date of the new appointment being validated

	/**
	 * Check that the appointment time does not conflict with any others.
	 */
	public static boolean isAvailable(Appointment newAppt, List<Object> sameDayAppts){
		if(sameDayAppts != null && !sameDayAppts.isEmpty()){
			// set appt date
			apptDate = newAppt.getApptdate();
			// create interval for length of appt
			Interval appt1 = dateAndTimeToInterval(newAppt.getApptdate(), newAppt.getAppttime(), APPT_LENGTH);
			String newPhlebId = ((Appointment)newAppt).getPhlebid().getId();
			for (Object obj : sameDayAppts){
				// see if appts have the same phleb and if appt is within business hours
				if((newPhlebId == ((Appointment)obj).getPhlebid().getId()) && isApptInBusinessHours(appt1)){
					Interval appt2 = dateAndTimeToInterval(((Appointment)obj).getApptdate(), ((Appointment)obj).getAppttime(), APPT_LENGTH);

					// see if new appointment conflicts with any that day
					if(appt1.overlaps(appt2) || !isValidGap(appt1, appt2)){
						return false;
					} 
				}
			}
		}
		return true;
	}

	/**
	 * Change a java.sql.Date and a java.sql.Time to an Interval of appointment length.
	 */
	private static Interval dateAndTimeToInterval(java.sql.Date date, java.sql.Time time, int length) {
	    String myDate = date + " " + time;
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    java.util.Date utilDate = new java.util.Date();
	    try {
	        utilDate = sdf.parse(myDate);
	    } catch (Exception e){
	        e.printStackTrace();
	    }

	    // make an interval for appointment length
	    DateTime start = new DateTime(utilDate);
	    DateTime end = start.plusMinutes(length);
	    Interval interval = new Interval(start, end);

	    return interval;
	}

	/**
	 * See if the interval between two appointments is greater or equal to the required gap length.
	 */
	private static boolean isValidGap(Interval appt1, Interval appt2){
		Interval gap = appt1.gap(appt2);
		int minutes = gap.toPeriod().getMinutes();
		int hours = gap.toPeriod().getHours();
		int totalMinutes = (hours*60) + minutes;
		return (totalMinutes >= APPT_GAP);
	}

	/**
	 * Returns whether or not the appointment falls completely between business hours.
	 */
	private static boolean isApptInBusinessHours(Interval appt){
		Interval businessHours = dateAndTimeToInterval(apptDate, java.sql.Time.valueOf(OPEN_TIME), OPEN_LENGTH);
		return businessHours.contains(appt);
	}
	
}