package com.specknet.orientandroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;


public class ProgressArcView extends View {

    private ArrayList<Record> records_arr;
    private static int[] walkingColour = {43, 163, 88};
    private static int[] climbingColour = {255, 208, 59};
    private static int[] runningColour = {241, 90, 41};
    private int base;
    private Record onGoingRecord;

    public ProgressArcView(Context context, AttributeSet attrs) {
        super(context,attrs);
        records_arr = new ArrayList<>();
    }

    public void addOnGoingRecord(Record record){
        onGoingRecord = record.copy();
        onGoingRecord.setCount(base + onGoingRecord.getCount());
        records_arr.add(onGoingRecord);
    }

    public void setCountOfOnGoingRecord(int count){
        onGoingRecord.setCount(base + count);
    }

    public Record getOnGoingRecord(){
        return onGoingRecord;
    }

    public Record getLastRecord(){
        return records_arr.get(records_arr.size() - 1);
    }

    public void setRecords(ArrayList<Record> values){
        records_arr.clear();

        // add all values into records and apply base length on each.
        // data should be from max to min when index from 0 to max.
        int base = 0;
        for (int i = 0; i < values.size(); i++){
            Record record = values.get(i).copy();
            if (record == null){
                continue;
            }
            Calendar tmp = Calendar.getInstance();
            tmp.setTime(record.getDatetime());
            if (tmp.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
                base += record.getCount();
                record.setCount(base);
                records_arr.add(record);
            }
        }
        this.base = base;
    }

    public void saveBase(){
        base = onGoingRecord.getCount();
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        int screenWidth = canvas.getWidth();  //Give your device screen width here
        int horizontalCentreX = screenWidth/2;
        int circleCentreX = horizontalCentreX;
        int screenHeight = canvas.getHeight();
        int circleCentreY = screenHeight/2;
        int radius = screenWidth/3;
        int arcStartingAngle = 270;
        int arcRectStartingX = circleCentreX - radius;
        int arcRectStartingY = circleCentreY - radius;
        int arcRectEndingX = circleCentreX + radius;
        int arcRectEndingY = circleCentreY + radius;

        Paint paint = new Paint();
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.FILL);
        final RectF oval = new RectF();
        paint.setStyle(Paint.Style.STROKE);
        oval.set(arcRectStartingX, arcRectStartingY, arcRectEndingX, arcRectEndingY);
        // base of the circle in grey
        paint.setColor(Color.rgb(188, 198, 204));
        canvas.drawArc(oval, arcStartingAngle, 360, false, paint);

        if (records_arr != null && !records_arr.isEmpty()){
            //plot records
            for (int i = records_arr.size() - 1; i >= 0; i--) {
                Record record = records_arr.get(i);
                if (record == null){
                    continue;
                }
                Calendar tmp = Calendar.getInstance();
                tmp.setTime(record.getDatetime());
                if (tmp.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)){
                    int[] colour = {0,0,0};
                    switch (record.getTypeOfWalk()){
                        case "walking":
                            colour = walkingColour;
                            break;
                        case "climbing":
                            colour = climbingColour;
                            break;
                        case "running":
                            colour = runningColour;
                            break;
                        default:
                            continue;
                    }
                    paint.setColor(Color.rgb(colour[0], colour[1], colour[2]));
                    canvas.drawArc(oval, arcStartingAngle, records_arr.get(i).getCount(), false, paint);
                }
            }
        }
    }
}
