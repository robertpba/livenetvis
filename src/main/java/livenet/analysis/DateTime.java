package livenet.analysis;

/*
 * Copyright (c) 2000-2018 Robert Biuk-Aghai
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You
 * may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import java.sql.Date;
import java.sql.Time;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class DateTime implements java.io.Serializable
{
    GregorianCalendar calendar;

    public DateTime(Date date, Time time)
    {
	GregorianCalendar dateCal = new GregorianCalendar();
	dateCal.setTime(date);

	GregorianCalendar timeCal = new GregorianCalendar();
	timeCal.setTime(time);

	calendar = new GregorianCalendar(dateCal.get(Calendar.YEAR),
					 dateCal.get(Calendar.MONTH),
					 dateCal.get(Calendar.DATE),
					 timeCal.get(Calendar.HOUR),
					 timeCal.get(Calendar.MINUTE),
					 timeCal.get(Calendar.SECOND));
    }


    public DateTime()
    {
	calendar = new GregorianCalendar();
    }


    /**
       Returns the number of seconds since the "epoch" (1970/1/1,00:00:00)
       represented by this DateTime object.
    **/
    public long getSeconds()
    {
	return (long) (calendar.getTime().getTime() / 1000);
    }


    /**
       Returns the number of whole days' difference between the supplied
       DateTime object and this DateTime object.
    **/
    public int getDaysDiff(DateTime datetime)
    {
	long mySeconds = this.getSeconds();
	long otherSeconds = datetime.getSeconds();
	long difference = Math.abs(otherSeconds - mySeconds);
	return (int) (difference / (60 * 60 * 24));
    }


    /**
       Returns the number of whole weeks' difference between the supplied
       DateTime object and this DateTime object.
    **/
    public int getWeeksDiff(DateTime datetime)
    {
	long mySeconds = this.getSeconds();
	long otherSeconds = datetime.getSeconds();
	long difference = Math.abs(otherSeconds - mySeconds);
	return (int) (difference / (60 * 60 * 24 * 7));
    }


    public String toString()
    {
	return calendar.get(Calendar.DATE) + "/" +
	    (calendar.get(Calendar.MONTH) + 1) + "/" +
	    calendar.get(Calendar.YEAR) + " " +
	    calendar.get(Calendar.HOUR_OF_DAY) + ":" +
	    calendar.get(Calendar.MINUTE) + ":" +
	    calendar.get(Calendar.SECOND);
    }
}
