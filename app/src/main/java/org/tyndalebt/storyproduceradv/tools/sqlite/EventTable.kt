package org.tyndalebt.storyproduceradv.tools.sqlite

//@Entity(tableName = "event_table")
data class EventTable(
  
    //@PrimaryKey(autoGenerate = true)
    var event_id : Int,
    
    var phone_id : String,
    var story_number : String, 
    var ethnolog  : String,
    var lwc : String,
    var translator_email : String,
    var trainer_email : String,
    var consultant_email : String,
    var video_name : String
    
    // @ColumnInfo(name = "published_author")
    // var author: String
)
