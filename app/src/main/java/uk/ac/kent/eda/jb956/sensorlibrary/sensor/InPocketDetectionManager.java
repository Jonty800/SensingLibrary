package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.data.InPocketContext;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class InPocketDetectionManager {

    private double D_th1 = 0.25;
    private double D_th2 = 0.5;
    private double I_dark = 50; //below 50
    public List<Double> lightValues = new ArrayList<>();
    public List<Double> proximityValues = new ArrayList<>();
    private List<Double> lightValuesNorm = new ArrayList<>();
    private List<Double> proximityValuesNorm = new ArrayList<>();

    private final String TAG = "InPocketDetectionManager";
    private static InPocketDetectionManager instance;
    private final Context context;

    static synchronized InPocketDetectionManager getInstance(Context context) {
        if (instance == null)
            instance = new InPocketDetectionManager(context);
        return instance;
    }

    public InPocketDetectionManager(Context context) {
        this.context = context;
    }

    InPocketContext getDetectionResult() {
        if (proximityValues.size() == 0 || lightValues.size() == 0)
            return InPocketContext.UNKNOWN;
        proximityValuesNorm = norm(proximityValues);
        lightValuesNorm = norm(lightValues);
        double M_proximity = arrayAverage(proximityValuesNorm);
        double M_light = arrayAverage(lightValuesNorm);

        System.out.println(M_proximity + " - " + D_th2 + " - " + M_light + " - " + I_dark);

        if (M_proximity < D_th2 && M_light < I_dark) {
            int numClose = 0;
            int numNear = 0;
            int numFar = 0;
            for (int i = 0; i < proximityValuesNorm.size(); i++) {
                double v = proximityValuesNorm.get(i);
                if (v < D_th1)
                    numClose++;
                else if (D_th1 <= v && v < D_th2)
                    numNear++;
                else if (v >= D_th2)
                    numFar++;
            }
            double P_close = numClose / proximityValuesNorm.size();
            double P_near = numNear / proximityValuesNorm.size();
            double P_far = numFar / proximityValuesNorm.size();
            double[] pocketArr = new double[]{1, 0, 0};
            double[] bagArr = new double[]{0, 1, 0};
            double[] f = new double[]{P_close, P_near, P_far};
            double mDistancePocket = manhattanDistance(pocketArr, f);
            double mDistancBag = manhattanDistance(bagArr, f);
            if (mDistancePocket < mDistancBag) {
                return InPocketContext.IN_POCKET;
            } else {
                return InPocketContext.IN_BAG;
            }
        } else {
            return InPocketContext.OUTSIDE_POCKET_BAG;
        }
    }

    private List<Double> norm(List<Double> vector) {
        double max = maxValue(vector);
        List<Double> normalized = new ArrayList<>();
        for (int i = 0; i < vector.size(); i++) {
            double v = vector.get(i);
            double x1 = v / max;
            normalized.add(x1);
        }
        return normalized;
    }

    private double maxValue(List<Double> array) {
        List<Double> list = new ArrayList<>();
        for (double d : array) {
            list.add(d);
        }
        return Collections.max(list);
    }

    private double arrayAverage(List<Double> marks) {
        double sum = 0;
        if (!marks.isEmpty()) {
            for (Double mark : marks) {
                sum += mark;
            }
            return sum / marks.size();
        }
        return sum;
    }

    private double manhattanDistance(double[] vector1, double[] vector2) {
        double sum = 0;
        for (int i = 0; i < vector1.length; i++) {
            sum += Math.abs(vector1.length - vector2.length);
        }
        return sum;
    }
}
