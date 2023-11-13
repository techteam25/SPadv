package org.tyndalebt.storyproduceradv.tools.sqlite
  
import android.content.Context  
import android.database.sqlite.SQLiteDatabase  
import android.database.sqlite.SQLiteOpenHelper  
import android.content.ContentValues  
import android.database.Cursor  
import android.database.sqlite.SQLiteException  
  
//creating the database logic, extending the SQLiteOpenHelper base class  
class EventTableHelper(context: Context): SQLiteOpenHelper(context,DATABASE_NAME,null,DATABASE_VERSION) {  
    companion object {  
    
       private val DATABASE_VERSION = 1  
       private val DATABASE_NAME = "SPadvDatabase"  
       private val TABLE_EVENT = "EventTable"  
        
       private val KEY_EVENT_ID  = "event_id"  
       private val KEY_PHONE_ID  = "phone_id" 
       private val KEY_STORY_NUMBER  = "story_number"  
       private val KEY_ETHNOLOG  = "ethnolog"
       private val KEY_LWC  = "lwc" 
       private val KEY_TRANSLATOR_EMAIL  = "translator_email" 
       private val KEY_TRAINER_EMAIL  = "trainer_email" 
       private val KEY_CONSULTANT_EMAIL  = "consultant_email"      
       private val KEY_VIDEO_NAME  = "video_name" 
    }  
    override fun onCreate(db: SQLiteDatabase?) {  
 
       //creating table with fields  
        val CreateEventTable = ("CREATE TABLE " + TABLE_EVENT + "("  +
                KEY_EVENT_ID  + " INTEGER PRIMARY KEY," +  
                KEY_PHONE_ID + " TEXT,"  + 
                KEY_STORY_NUMBER  + " TEXT,"  +   
                KEY_ETHNOLOG + " TEXT,"  + 
                KEY_LWC  + " TEXT,"  + 
                KEY_TRANSLATOR_EMAIL + " TEXT,"  + 
                KEY_TRAINER_EMAIL + " TEXT,"  + 
                KEY_CONSULTANT_EMAIL + " TEXT,"  +       
                KEY_VIDEO_NAME  + " TEXT"  +  
                     
                ")")  
        db?.execSQL(CreateEventTable)  
    }  
  
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {  

        db!!.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENT)  
        onCreate(db)  
    }  
 
 
    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
     
  
    //method to insert data  
    fun addEvent(event: EventTable):Long{  
    
        val db = this.writableDatabase  
        val contentValues = ContentValues() 
        
        // contentValues.put(KEY_EVENT_ID, event.event_id)  // auto-increment
        contentValues.put(KEY_PHONE_ID, event.phone_id)
        contentValues.put(KEY_STORY_NUMBER, event.story_number)   
        contentValues.put(KEY_ETHNOLOG, event.ethnolog) 
        contentValues.put(KEY_LWC, event.lwc) 
        contentValues.put(KEY_TRANSLATOR_EMAIL, event.translator_email) 
        contentValues.put(KEY_TRAINER_EMAIL, event.trainer_email)
        contentValues.put(KEY_CONSULTANT_EMAIL, event.consultant_email)      
        contentValues.put(KEY_VIDEO_NAME, event.video_name)         
        
        // Inserting Row  
        val success = db.insert(TABLE_EVENT, null, contentValues)  
        //2nd argument is String containing nullColumnHack  
        db.close() // Closing database connection  
        return success  
        
    } 


    //@Throws(SQLiteConstraintException::class)
    fun viewEvent(event_id : Int) : List<EventTable> {
    
        val db=writableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("select * from " + TABLE_EVENT + " WHERE " + KEY_EVENT_ID + "='" + event_id + "'", null)
        }catch (e: SQLiteException){
            onCreate(db)
            return ArrayList()
        }
        val eventList = fillListFromCursor(cursor)
        return eventList  
    }
     
    
    //method to read data, returns all rows  
    fun viewAllEvents() : List<EventTable>{  
    
        val selectQuery = "SELECT  * FROM $TABLE_EVENT"  
        val db = this.readableDatabase  
        var cursor: Cursor? = null  
        try{  
            cursor = db.rawQuery(selectQuery, null)  
        }catch (e: SQLiteException) {  
            db.execSQL(selectQuery)  
            return ArrayList()  
        }  
 
        val eventList = fillListFromCursor(cursor)
        return eventList  
    } 
    
    
    private fun fillListFromCursor(cursor : Cursor?) : List<EventTable> {

        val eventList : ArrayList<EventTable> = ArrayList<EventTable>()
        if (cursor != null) {         
           var event_id : Int  // auto-increment
           var phone_id : String
           var story_number : String   
           var ethnolog : String 
           var lwc : String 
           var translator_email : String 
           var trainer_email : String
           var consultant_email : String      
           var video_name : String  
           
           if (cursor.moveToFirst()) {  
               do {  
                   event_id = cursor.getInt(cursor.getColumnIndex(KEY_EVENT_ID))   // auto-increment
                   phone_id = cursor.getString(cursor.getColumnIndex(KEY_PHONE_ID)) 
                   story_number = cursor.getString(cursor.getColumnIndex(KEY_STORY_NUMBER))    
                   ethnolog = cursor.getString(cursor.getColumnIndex(KEY_ETHNOLOG))  
                   lwc = cursor.getString(cursor.getColumnIndex(KEY_LWC))  
                   translator_email = cursor.getString(cursor.getColumnIndex(KEY_TRANSLATOR_EMAIL))  
                   trainer_email  = cursor.getString(cursor.getColumnIndex(KEY_TRAINER_EMAIL)) 
                   consultant_email  = cursor.getString(cursor.getColumnIndex(KEY_CONSULTANT_EMAIL))       
                   video_name  = cursor.getString(cursor.getColumnIndex(KEY_VIDEO_NAME))
               
                   val event= EventTable(
                           event_id = event_id, // auto-increment
                           phone_id = phone_id, 
                           story_number = story_number,  
                           ethnolog = ethnolog,
                           lwc = lwc,
                           translator_email = translator_email, 
                           trainer_email = trainer_email,
                           consultant_email = consultant_email,      
                           video_name = video_name 
                           )  
                   eventList.add(event)  
               } while (cursor.moveToNext())  
           } 
        }   
        return eventList
    }    
        
     
    //method to update data  
    fun updateEvent(event : EventTable) : Int {  
        val db = this.writableDatabase  
        val contentValues = ContentValues()  
       
        //contentValues.put(KEY_EVENT_ID, event.event_id)
        contentValues.put(KEY_PHONE_ID, event.phone_id)
        contentValues.put(KEY_STORY_NUMBER, event.story_number)   
        contentValues.put(KEY_ETHNOLOG, event.ethnolog) 
        contentValues.put(KEY_LWC, event.lwc) 
        contentValues.put(KEY_TRANSLATOR_EMAIL, event.translator_email) 
        contentValues.put(KEY_TRAINER_EMAIL, event.trainer_email)
        contentValues.put(KEY_CONSULTANT_EMAIL, event.consultant_email)      
        contentValues.put(KEY_VIDEO_NAME, event.video_name)          
  
        // Updating Row  
        val success = db.update(TABLE_EVENT, contentValues, "event_id=" + event.event_id, null)  
        //2nd argument is String containing nullColumnHack  
        db.close() // Closing database connection  
        return success  
    }  
    
        
    //method to delete data  
    //@Throws(SQLiteConstraintException::class)
    fun deleteEvent(event: EventTable) : Int {  
        val db = this.writableDatabase  
        val contentValues = ContentValues()  
        contentValues.put(KEY_EVENT_ID, event.event_id)  
        
        // Deleting Row  
        val success = db.delete(TABLE_EVENT,"event_id=" + event.event_id, null)  
        //2nd argument is String containing nullColumnHack  
        db.close() // Closing database connection  
        return success  
    } 


   fun deleteEvent(event_id : Int) : Int {
      val db = this.writableDatabase
      val contentValues = ContentValues()
      contentValues.put(KEY_EVENT_ID, event_id)

      // Deleting Row
      val success = db.delete(TABLE_EVENT,"event_id=" + event_id, null)
      //2nd argument is String containing nullColumnHack
      db.close() // Closing database connection
      return success
   }
}  
