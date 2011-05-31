
package in.dharmin.calendar;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.preference.PreferenceManager;

public class DBHelper {
	private static final String DATABASE_NAME = "mycal.db";
	private static final String TABLE_NAME = "mainTable";
	private static final String KEY_ID = "_id";
	private toDoDBOpenHelper dbHelper;
	private SQLiteDatabase db;
	private final Context context;
		
	public DBHelper(Context _context)
	{
		context = _context;
		dbHelper = new toDoDBOpenHelper(context,DATABASE_NAME,null,1);
	}
	public void open() throws SQLiteException{

		try
		{
			db = dbHelper.getWritableDatabase();
		}catch(SQLiteException e)
		{
			e.printStackTrace();
			db = dbHelper.getReadableDatabase();
		}
	}
	public void dropDatabase(){
		context.deleteDatabase(DATABASE_NAME);
	}
	public Cursor queryDatabase()
	{
		String[] result_columns = new String[]{KEY_ID,"_dtstart","_dtend","_dtmodified","_attendee",
			"_uid","_dtcreated","_dtstamp","_desc","_location","_status","_schoolid","_summary","_allday"};
		Cursor allRows = db.query(true,TABLE_NAME, result_columns, null, null, null, 
				null,null,null);
	
		return allRows;
	}
	public Cursor queryRow(String where)
	{
		String id="";
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if(prefs.getBoolean("cal1", false))
			id = "'"+prefs.getString("cal1ID", "")+"'";
		if(prefs.getBoolean("cal2", false))
			id += ",'"+prefs.getString("cal2ID", "")+"'";
		if(prefs.getBoolean("cal3", false))
			id += ",'"+prefs.getString("cal3ID", "")+"'";
		
		if(id.startsWith(","))
			id=id.substring(1);
		
		if(id.length()<1)
			id="''";
		
		where += " AND _schoolid IN ("+id+")";
		
		String[] result_columns = new String[]{KEY_ID,"_dtstart","_dtend","_dtmodified","_attendee",
				"_uid","_dtcreated","_dtstamp","_desc","_location","_status","_summary","_schoolid","_allday"};
		Cursor allRows = db.query(true,TABLE_NAME, result_columns, where, null, 
				null, null,"_dtstart",null);
		return allRows;
	}
	
	public long insert(ContentValues contentValues)
	{
		return db.insert(TABLE_NAME, null, contentValues);
	}
	public boolean update(ContentValues contentValues,String where)
	{
		return db.update(TABLE_NAME, contentValues, where, null)>0;
	}

	public boolean remove(String where)
	{
		where = KEY_ID+"="+where;
		return db.delete(TABLE_NAME, where, null)>0;
	}
	public boolean removeSchool(String where)
	{
		where = "_schoolid='"+where+"'";
		return db.delete(TABLE_NAME, where, null)>0;
	}
	public void close()
	{
		db.close();
	}	
	private static class toDoDBOpenHelper extends SQLiteOpenHelper{

		private static final String DATABASE_CREATE = "create table if not exists "+TABLE_NAME+" " +
				"("+KEY_ID+" integer primary key autoincrement, _dtstart integer not null, _dtend integer not null," +
						"_dtmodified integer, _uid text,_dtcreated integer,_dtstamp integer," +
						"_desc text,_location text,_status text,_schoolid text,_attendee text,_summary text,_allday boolean DEFAULT 0);";

		public toDoDBOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_NAME);
			onCreate(db);
		}
		
	}
}
