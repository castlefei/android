package com.specknet.orientandroid;

import android.arch.persistence.room.*;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.Date;

@Entity
public class Record implements Parcelable {

    private static int LastId = 0;

    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "date")
    private Date datetime;

    @ColumnInfo(name = "type_of_walk")
    private String typeOfWalk;

    @ColumnInfo(name = "count")
    private int count;

    public Record copy(){
        Record tmp = new Record(typeOfWalk, count);
        tmp.uid = this.uid;
        tmp.datetime = this.datetime;
        return tmp;
    }

    protected Record(String typeOfWalk, int count){
        this(typeOfWalk, count, 0);
    }

    protected Record(String typeOfWalk, int count, int dateBias){
        this.typeOfWalk = typeOfWalk;
        this.count = count;
        this.uid = ++LastId;
        Calendar tmp = Calendar.getInstance();
        tmp.add(Calendar.DAY_OF_YEAR, dateBias);
        this.datetime = tmp.getTime();
    }

    protected Record(Parcel in) {
        uid = in.readInt();
        typeOfWalk = in.readString();
        count = in.readInt();
        datetime = (Date) in.readValue(Date.class.getClassLoader());
    }

    public static final Creator<Record> CREATOR = new Creator<Record>() {
        @Override
        public Record createFromParcel(Parcel in) {
            return new Record(in);
        }

        @Override
        public Record[] newArray(int size) {
            return new Record[size];
        }
    };

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }

    public String getTypeOfWalk() {
        return typeOfWalk;
    }

    public void setTypeOfWalk(String typeOfWalk) {
        this.typeOfWalk = typeOfWalk;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(uid);
        parcel.writeValue(datetime);
        parcel.writeString(typeOfWalk);
        parcel.writeInt(count);
    }
}
