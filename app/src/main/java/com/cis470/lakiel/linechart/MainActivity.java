package com.cis470.lakiel.linechart;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.DataPointInterface;

import android.graphics.Canvas;
import android.graphics.Paint;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static int RANGE = 4;
    private SensorManager sensorManager;
    private PointsGraphSeries<DataPoint> mSeries1;
    private PointsGraphSeries<DataPoint> mSeries2;
    private long graph1LastXValue = 0;
    private long graph2LastXValue = 0;
    GraphView graph1;
    GraphView graph2;
    private boolean lowOrHighPass;
    float weight = 0.1f;
    float mLowPassX;
    float mLowPassY;
    float mLowPassZ;

    //High pass
    float mLastX;
    float mLastY;
    float mLastZ;

    float mHighPassX;
    float mHighPassY;
    float mHighPassZ;

    float[] gravity = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSensorData();
        setContentView(R.layout.activity_main);

        graph1 = (GraphView) findViewById(R.id.graph1);
        mSeries1 = new PointsGraphSeries<DataPoint>();
        graph1.addSeries(mSeries1);

        graph1.getViewport().setScalable(true);
        graph1.getViewport().setScrollable(true);
        graph1.getViewport().setScalableY(true);
        graph1.getViewport().setScrollableY(true);

        graph2 = (GraphView) findViewById(R.id.graph2);
        mSeries2 = new PointsGraphSeries<DataPoint>();
        graph2.addSeries(mSeries2);

        graph2.getViewport().setScalable(true);
        graph2.getViewport().setScrollable(true);
        graph2.getViewport().setScalableY(true);
        graph2.getViewport().setScrollableY(true);

        mSeries1.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                paint.setStrokeWidth(5);
                canvas.drawLine(x - 20, y - 20, x + 20, y + 20, paint);
                canvas.drawLine(x + 20, y - 20, x - 20, y + 20, paint);
            }
        });

        mSeries2.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                paint.setStrokeWidth(5);
                canvas.drawLine(x - 20, y - 20, x + 20, y + 20, paint);
                canvas.drawLine(x + 20, y - 20, x - 20, y + 20, paint);
            }
        });

        graph1.getViewport().setXAxisBoundsManual(true);
        graph1.getViewport().setMinX(0);
        graph1.getViewport().setMaxX(RANGE);
        graph1.getViewport().setYAxisBoundsManual(true);
        graph1.getViewport().setMaxY(10);
        graph1.getViewport().setMinY(0);

        graph2.getViewport().setXAxisBoundsManual(true);
        graph2.getViewport().setMinX(0);
        graph2.getViewport().setMaxX(RANGE);
        graph2.getViewport().setYAxisBoundsManual(true);
        graph2.getViewport().setMaxY(10);
        graph2.getViewport().setMinY(0);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                long ts = event.timestamp;

                if (0 == graph1LastXValue)
                    graph1LastXValue = ts;
                float nts = (ts - graph1LastXValue) * 0.000000001f;
                System.out.println("ts = " + nts);

                if (nts > RANGE) {
                    graph1LastXValue = ts;
                    DataPoint[] dps = new DataPoint[1];
                    dps[0] = new DataPoint(0, 0);
                    mSeries1.resetData(dps);
                    nts = 0;
                }

                DataPoint dataPoint = new DataPoint(nts, y);
                mSeries1.appendData(dataPoint, false, 100);

                //Second Graph
                if (0 == graph2LastXValue)
                    graph2LastXValue = ts;
                nts = (ts - graph2LastXValue) * 0.000000001f;
                System.out.println("ts = " + nts);

                if (nts > RANGE) {
                    graph2LastXValue = ts;
                    DataPoint[] dps = new DataPoint[1];
                    dps[0] = new DataPoint(0, 0);
                    mSeries2.resetData(dps);
                    nts = 0;
                }

                if (lowOrHighPass) {
                    mLowPassX = lowPass(x, mLowPassX);
                    mLowPassY = lowPass(y, mLowPassY);
                    mLowPassZ = lowPass(z, mLowPassZ);

                    double sumOfSquares = (mLowPassX * mLowPassX)
                            + (mLowPassY * mLowPassY)
                            + (mLowPassZ * mLowPassZ);
                    double acceleration = Math.sqrt(sumOfSquares);
                    dataPoint = new DataPoint(nts, acceleration);
                    mSeries2.appendData(dataPoint, false, 100);
                }

                if (!lowOrHighPass) {
                    mHighPassX = highPass(x, mLastX, mHighPassX);
                    mHighPassY = highPass(y, mLastY, mHighPassY);
                    mHighPassZ = highPass(z, mLastZ, mHighPassZ);
                    mLastX = x;
                    mLastY = y;
                    mLastZ = z;

                    double sumOfSquares = (mHighPassX * mHighPassX)
                            + (mHighPassY * mHighPassY)
                            + (mHighPassZ * mHighPassZ);
                    double acceleration = Math.sqrt(sumOfSquares);
                    dataPoint = new DataPoint(nts, acceleration);
                    mSeries2.appendData(dataPoint, false, 100);
                }
                break;
        }
    }

    // simple low-pass filter
    float lowPass(float current, float last) {
        return last * (1.0f - weight) + current * weight;
    }

    // simple high-pass filter
    float highPass(float current, float last, float filtered) {
        return weight * (filtered + current - last);
    }

    private void getSensorData() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this,
                sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onClickLowPass(View view) {
        lowOrHighPass = true;
        DataPoint[] dps = new DataPoint[1];
        dps[0] = new DataPoint(0, 0);
        mSeries2.resetData(dps);
        Toast.makeText(this, "Switching to Low Pass", Toast.LENGTH_SHORT).show();
    }

    public void onClickHighPass(View view) {
        lowOrHighPass = false;
        DataPoint[] dps = new DataPoint[1];
        dps[0] = new DataPoint(0, 0);
        mSeries2.resetData(dps);
        Toast.makeText(this, "Switching to High Pass", Toast.LENGTH_SHORT).show();
    }
}