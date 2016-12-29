package apollo.util;

/**
* This handles dates in the format YYYY-MM-DD, which is a partial ISO 8601 format.
*/

public class DateYMD implements Comparable, java.io.Serializable {
	int year;	//4 digit year
	int month;  //month from 1..12
	int day;

	public static String getDate() {
		return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
	}

	public DateYMD() {
		this(getDate());
	}

	public DateYMD(String s) {
		DateYMD d2=fromString(s);
		this.year=d2.year;
		this.month=d2.month;
		this.day=d2.day;
	}

	public DateYMD(int y,int m,int d) {
		year=y;
		month=m;
		day=d;
	}

	public int getYear() {return year;}
	public int getMonth() {return month;}
	public int getDay() {return day;}

	public String toString() {
		return sortKey();
	}

	public String toDateString() {
		return monthName(month)+" "+day+", "+year;
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
		return year+"-"+dos(month)+"-"+dos(day);
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
		if (o instanceof DateYMD) {
			DateYMD d=(DateYMD)o;
			return sortKey().compareTo(d.sortKey());
		} else {
			return -1;
		}
	}

	//boolean logic
	public boolean lt(DateYMD d) {
		return compareTo(d)==-1;
	}
	public boolean lte(DateYMD d) {
		int tri=compareTo(d);
		return (tri==-1 || tri==0);
	}
	public boolean eq(DateYMD d) {
		return compareTo(d)==0;
	}
	public boolean gt(DateYMD d) {
		return compareTo(d)==1;
	}
	public boolean gte(DateYMD d) {
		int tri=compareTo(d);
		return (tri==0 || tri==1);
	}

	//expects input in the form YYYY-MM-DD
	public static DateYMD fromString(String s) throws IllegalArgumentException {
		try {
			String[] sa=s.split("-");
			int iy=Integer.parseInt(sa[0]);
			int im=Integer.parseInt(sa[1]);
			int id=Integer.parseInt(sa[2]);
			return new DateYMD(iy,im,id);
		} catch (Exception x) {
			throw new IllegalArgumentException("invalid format: "+s);
		}
	}
}