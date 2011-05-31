package in.dharmin.calendar;

import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;

public class Main extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
//        One way to use the calendar widget is putting it in the xml file is shown in main.xml
//        setContentView(R.layout.main);

        /*
         Other way is to add is using the java code as follows.
		*/
        MonthView mv = new MonthView(this);
        setContentView(mv);
//        Calendar cal = Calendar.getInstance();
//        cal.set(2012, Calendar.DECEMBER,12);
//        mv.GoToDate(cal.getTime());
    }
}