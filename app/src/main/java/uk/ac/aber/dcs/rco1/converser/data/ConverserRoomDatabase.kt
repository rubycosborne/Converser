package uk.ac.aber.dcs.rco1.converser.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import uk.ac.aber.dcs.rco1.converser.model.home.TranslationItem
import uk.ac.aber.dcs.rco1.converser.model.home.TranslationItemDao

//only support the message entity
@Database(entities = [TranslationItem::class], version = 1)
abstract class ConverserRoomDatabase: RoomDatabase(){

    //way to get hold of a MessageDao object - Room generates code for this
    abstract fun messageDao():TranslationItemDao

    //allow way to make object of the DB as room insists on this being an abstract class
    companion object{
        private var instance: ConverserRoomDatabase? = null
        private val coroutineScope = CoroutineScope(Dispatchers.Main)

        fun getDatabase(context: Context): ConverserRoomDatabase?{
            synchronized(this){
                if (instance == null){
                    instance =
                        Room.databaseBuilder<ConverserRoomDatabase>(
                            context.applicationContext,
                            ConverserRoomDatabase::class.java,
                            "converser_database"
                        )
                            .addCallback(roomDatabaseCallback(context)) //only use to pre-populate DB
                            //TODO: handle migrations if needed
                            .build()
                }
                return instance!!
            }
        }
        private fun roomDatabaseCallback(context: Context): Callback {
            return object: Callback(){
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                }
            }
        }
    }
}