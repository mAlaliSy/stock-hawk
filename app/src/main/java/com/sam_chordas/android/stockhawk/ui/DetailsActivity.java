package com.sam_chordas.android.stockhawk.ui;

import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.Tools;
import com.db.chart.listener.OnEntryClickListener;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.db.chart.view.Tooltip;
import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Created by mohammad on 17/08/16.
 */

public class DetailsActivity extends AppCompatActivity {

    LineChartView lineChartView;

    String [] timePeriods;

    Spinner spinner ;

    String mSymbol;

    OkHttpClient mHttp;

    ProgressDialog progressDialog ;

    LineSet mLineSet;

    int mMin ;
    int mMax ;


    final String SYMBOL_ARG = "?symbol";
    final String RANGE_ARG = "?range";
    final String CHART_API_URL = "http://chartapi.finance.yahoo.com/instrument/1.0/" + SYMBOL_ARG + "/chartdata;type=quote;range=" + RANGE_ARG + "/json";

    MyAsyncTask myAsyncTask ;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_line_graph);

        spinner = (Spinner) findViewById(R.id.spinnerTime);

        lineChartView = (LineChartView) findViewById(R.id.linechart);

        lineChartView.setAxisColor(getColorFromRes(R.color.deepOrange));

        lineChartView.setXLabels(AxisController.LabelPosition.NONE);


        mHttp = new OkHttpClient();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.pleaseWait));

        mSymbol = getIntent().getExtras().getString("symbol");


        timePeriods = getResources().getStringArray(R.array.timePeriods);


        myAsyncTask = new MyAsyncTask();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                progressDialog.show();
                String period = timePeriods[i] + "m";
                if (myAsyncTask != null)
                    myAsyncTask.cancel(true);
                myAsyncTask = new MyAsyncTask();
                myAsyncTask.execute(period);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



    }


    int getColorFromRes(int res) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(res, getTheme());
        } else {
            return getResources().getColor(res);
        }
    }


    LineSet JsonArrayToLineSet(JSONArray jsonArray) throws JSONException {



        LineSet lineSet = new LineSet();

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject jsonObject = jsonArray.getJSONObject(i);

            String date = String.valueOf(jsonObject.getInt("Date"));
            float close = (float) jsonObject.getDouble("close");


            StringBuilder stringBuilder = new StringBuilder(date);
            stringBuilder.insert(4 , "/");
            stringBuilder.insert(7 , "/");


            lineSet.addPoint(stringBuilder.toString(), close);

        }

        return lineSet;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int selectedPeriod = spinner.getSelectedItemPosition();
        outState.putInt("timePeriod" , selectedPeriod);


    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);


        if (savedInstanceState.containsKey("timePeriod")){
            spinner.setSelection(savedInstanceState.getInt("timePeriod"));
        }

    }

    class MyAsyncTask extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... strings) {
            String range = strings[0];

            String url = CHART_API_URL;

            url = url.replace(SYMBOL_ARG, mSymbol);
            url = url.replace(RANGE_ARG, range);

            Request request = new Request.Builder()
                    .url(url).build();

            try {
                Response response = mHttp.newCall(request).execute();
                if (!response.isSuccessful())
                    return null;
                return response.body().string();
            } catch (IOException e) {
                return null;
            }
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {

                String json = s.substring(s.indexOf("{"), s.lastIndexOf("}") + 1);


                JSONObject jsonObject = new JSONObject(json);


                JSONObject closeRanges = jsonObject.getJSONObject("ranges").getJSONObject("close");


                JSONArray jsonArray = jsonObject.getJSONArray("series");



                mLineSet = JsonArrayToLineSet(jsonArray);

                lineChartView.dismiss();

                mMin = closeRanges.getInt("min");
                mMax = closeRanges.getInt("max");

                int step ;

                // Find a good step

                if (mMin>250){ // Make the step multiplier of simple number. This will make the chart numbers look better .
                    step = mMin/10;

                    step /= 25;
                    step *= 25;

                }else if (mMin>25) {
                    step = mMin / 5;

                    step /= 5;
                    step *= 5;

                }else{
                    step = 1 ;
                }


                lineChartView.setStep(step);


                float radius ;

                int itemsCount = mLineSet.getEnd() ;

                if (itemsCount < 93)
                    radius = 7 ;
                else
                    radius = 4 ;

                mLineSet.setDotsRadius(radius);



                mLineSet.setColor(getColorFromRes(R.color.accent_color));
                mLineSet.setDotsColor(getColorFromRes(R.color.deepOrange));

                mLineSet.setFill(getColorFromRes(R.color.blueGray));
                mLineSet.setThickness(3);


                lineChartView.addData(mLineSet);



                lineChartView.setOnEntryClickListener(new OnEntryClickListener() {
                    @Override
                    public void onClick(int setIndex, int entryIndex, Rect rect) {

                        lineChartView.dismissAllTooltips();


                        String label = mLineSet.getLabel(entryIndex);
                        float value = mLineSet.getValue(entryIndex);

                        int width = (int)Tools.fromDpToPx(150);
                        int height = (int)Tools.fromDpToPx(32) ;

                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width,height);


                        layoutParams.leftMargin = rect.centerX() - width/2;
                        layoutParams.topMargin = rect.top ;



                        Tooltip tooltip = new Tooltip(DetailsActivity.this , R.layout.tooltip,R.id.tooltipText);


                        tooltip.setHorizontalAlignment(Tooltip.Alignment.CENTER);
                        tooltip.setVerticalAlignment(Tooltip.Alignment.TOP_TOP);

                        tooltip.setLayoutParams(layoutParams);



                        TextView text = (TextView) tooltip.findViewById(R.id.tooltipText);

                        text.setText(label + " : " + value);

                        lineChartView.showTooltip(tooltip , true);




                    }
                });


                lineChartView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        lineChartView.dismissAllTooltips();
                    }
                });

                lineChartView.show();

                progressDialog.dismiss();

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }


}
