package apollo.util;

/**
* This handles months in the format YYYY-MM.  This is used for accounting applications.
* It is pretty much the same as DateYMD, without the date part.
*/

public class DateYM implements Comparable, java.io.Serializable {
	int year;	//4 digit year
	int month;  //month from 1..12

	public static String getCurrentMonth() {
		return new java.text.SimpleDateFormat("yyyy-MM").format(new java.util.Date());
	}

	public static DateYM fromDateYMD(DateYMD dymd) {
		return new DateYM(dymd.getYear(),dymd.getMonth());
	}

	public DateYM() {
		this(getCurrentMonth());

		//note: this is actually doing a lot of stuff under the hood.
		//first it create a new Date and a SimpleDate format, then a new String
		//then it calls the other constructor, which calls fromString()
		//which creates a String array to split it.  Then it creates a DateYM field
		//from that and copies the fields over.
		//So it creates at least 5 objects in the course of constructing this
		//not a problem, but may not be the most efficient
	}

	public DateYM(String s) {
		DateYM d2=fromString(s);
		this.year=d2.year;
		this.month=d2.month;
	}

	public DateYM(int y,int m) {
		year=y;
		month=m;
	}

	public int getYear() {return year;}
	public int getMonth() {return month;}

	public String toString() {
		return sortKey();
	}

	//returns something like 'January 2017'
	public String toMonthString() {
		return monthName(month)+" "+year;
	}

	public static String monthName(int m) {
		switch (m) {
			case 1: return "January";
			case 2: return "February";
			case 3: return "March";
			case 4: return "April";
			case 5: return "May";
			case 6: return "June";
			case 7: return "July";
			case 8: return "August";
			case 9: return "September";
			case 10: return "October";
			case 11: return "November";
			case 12: return "December";
			default: return "err";
		}
	}

	public String sortKey() {
		return year+"-"+dos(month);
	}

	public static String dos(int i) {
		if (i>9) {
			return String.valueOf(i);
		} else {
			return "0"+String.valueOf(i);
		}
	}

	//returns either -1 0 or 1
	public int compareTo(Object o) {
		if (o==null) {return -1;}  	   //put that at the end
		if (o instanceof DateYM) {
			DateYM d=(DateYM)o;
			return sortKey().compareTo(d.sortKey());
		} else {
			return -1;
		}
	}

	//boolean logic
	public boolean lt(DateYM d) {
		return compareTo(d)==-1;
	}
	public boolean lte(DateYM d) {
		int tri=compareTo(d);
		return (tri==-1 || tri==0);
	}
	public boolean eq(DateYM d) {
		return compareTo(d)==0;
	}
	public boolean gt(DateYM d) {
		return compareTo(d)==1;
	}
	public boolean gte(DateYM d) {
		int tri=compareTo(d);
		return (tri==0 || tri==1);
	}

	//handles input in the form YYYY-MM or YYYY-MM-DD
	public static DateYM fromString(String s) throws IllegalArgumentException {
		try {
			String[] sa=s.split("-");
			int iy=Integer.parseInt(sa[0]);
			int im=Integer.parseInt(sa[1]);
			return new DateYM(iy,im);
		} catch (Exception x) {
			throw new IllegalArgumentException("invalid format: "+s);
		}
	}
}