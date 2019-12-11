package com.specknet.orientandroid;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;


public class PlotActivity extends Activity {

    private XYPlot plot;
    private ArrayList<Record> records;
    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plot);

        records = MainActivity.records;
        plot = findViewById(R.id.plot);

        Integer[] walk_arr = new Integer[]{0,0,0,0,0,0};
        Integer[] climb_arr = new Integer[]{0,0,0,0,0,0};
        Integer[] run_arr = new Integer[]{0,0,0,0,0,0};
        Integer[] default_arr = new Integer[]{0,0,0,0,0,0};
        Calendar begin = Calendar.getInstance();
        begin.add(Calendar.DAY_OF_YEAR, -4);

        int i = 0;
        while(begin.compareTo(Calendar.getInstance()) <= 0) {
            int test = begin.get(Calendar.DAY_OF_YEAR);
            for (Record record : records){
                Calendar tmp = Calendar.getInstance();
                tmp.setTime(record.getDatetime());
                if (tmp.get(Calendar.DAY_OF_YEAR) == begin.get(Calendar.DAY_OF_YEAR)){
                    switch (record.getTypeOfWalk()){
                        case "walking":
                            walk_arr[i] += record.getCount();
                            break;
                        case "climbing":
                            climb_arr[i] += record.getCount();
                            break;
                        case "running":
                            run_arr[i] += record.getCount();
                            break;
                        default:
                            default_arr[i] += record.getCount();
                            break;
                    }
                }
            }
            begin.add(Calendar.DAY_OF_YEAR, 1);
            i++;
        }

        XYSeries walking = new SimpleXYSeries(Arrays.asList(walk_arr), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "walking");
        XYSeries climbing = new SimpleXYSeries(Arrays.asList(climb_arr), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "climbing");
        XYSeries running = new SimpleXYSeries(Arrays.asList(run_arr),SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "running");

        BarFormatter walkingFormatter = new BarFormatter(Color.parseColor("#2ba358"), Color.BLACK);
        plot.addSeries(walking, walkingFormatter);
        BarFormatter climbingFormatter = new BarFormatter(Color.parseColor("#ffd03b"), Color.BLACK);
        plot.addSeries(climbing, climbingFormatter);
        BarFormatter runningFormatter = new BarFormatter(Color.parseColor("#f15a29"), Color.BLACK);
        plot.addSeries(running, runningFormatter);

        BarRenderer barRenderer = plot.getRenderer(BarRenderer.class);
        barRenderer.setBarOrientation(BarRenderer.BarOrientation.SIDE_BY_SIDE);
        barRenderer.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_GAP, PixelUtils.dpToPix(20));

        plot.getGraph();
    }

}
