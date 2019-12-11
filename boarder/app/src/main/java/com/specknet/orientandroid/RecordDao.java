package com.specknet.orientandroid;
import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.*;

import java.util.List;

@Dao
public interface RecordDao {
    @Query("SELECT * FROM record")
    LiveData<List<Record>> getAll();

    @Query("SELECT * FROM record WHERE uid IN (:recordIds)")
    LiveData<List<Record>> loadAllByIds(int[] recordIds);

//    @Query("SELECT * FROM record WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    Record findByName(String first, String last);

    @Insert(onConflict = OnConflictStrategy.FAIL)
    void insert(Record record);

    @Insert
    void insertAll(Record... records);

    @Delete
    void delete(Record record);

    @Query("DELETE FROM record")
    void deleteAll();
}