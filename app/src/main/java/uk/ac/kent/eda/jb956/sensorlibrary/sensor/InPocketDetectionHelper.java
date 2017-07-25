package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.data.InPocketContext;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class InPocketDetectionHelper {

    private double th_pocket = 1.50;
    private double th_bag = 2;
    private double i_dark = 50; //below 50
    public List<Double> lightValues = new ArrayList<>();
    public List<Double> proximityValues = new ArrayList<>();
    //private List<Double> lightValuesNorm = new ArrayList<>();
   // private List<Double> proximityValuesNorm = new ArrayList<>();

    private final String TAG = "InPocketDetectionManager";
    private static InPocketDetectionHelper instance;

    static synchronized InPocketDetectionHelper getInstance() {
        if (instance == null)
            instance = new InPocketDetectionHelper();
        return instance;
    }

    InPocketContext getDetectionResult() {
        if (proximityValues.size() == 0 || lightValues.size() == 0)
            return InPocketContext.UNKNOWN;
       // proximityValuesNorm = norm(proximityValues);
       // lightValuesNorm = norm(lightValues);
        double m_proximity = arrayAverage(proximityValues);
        double m_light = arrayAverage(lightValues);

       // System.out.println(m_proximity + " - " + th_bag + " - " + m_light + " - " + i_dark);

        if (m_proximity < th_bag && m_light < i_dark) {
            int numClose = 0;
            int numNear = 0;
            int numFar = 0;
            /*for (int i = 0; i < proximityValues.size(); i++) {
                double proximity = proximityValues.get(i);
                if (proximity < th_pocket)
                    numClose++;
                else if (th_pocket <= proximity && proximity < th_bag)
                    numNear++;
                else if (proximity >= th_bag)
                    numFar++;
            }
            double p_close = numClose / proximityValues.size();
            double p_near = numNear / proximityValues.size();
            double p_far = numFar / proximityValues.size();*/
            //double[] pocketArr = new double[]{1, 0, 0};
            //double[] bagArr = new double[]{0, 1, 0};
            //double[] f = new double[]{p_close, p_near, p_far};
            //double mDistancePocket = manhattanDistance(pocketArr, f);
            //double mDistancBag = manhattanDistance(bagArr, f);

            if (m_proximity <= th_pocket) {
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
