package uk.ac.kent.eda.jb956.sensorlibrary.util;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

import java.util.Arrays;

/**
 * <p>
 * The noise filter, same as implemented in sphinxbase/sphinxtrain/pocketsphinx.
 * https://github.com/cmusphinx/sphinx4/blob/master/sphinx4-core/src/main/java/edu/cmu/sphinx/frontend/denoise/Denoise.java
 * <p>
 * Noise removal algorithm is inspired by the following papers:
 * <p>
 * Computationally Efficient Speech Enchancement by Spectral Minina Tracking by G. Doblinger
 * <p>
 * Power-Normalized Cepstral Coefficients (PNCC) for Robust Speech Recognition by C. Kim.
 * <p>
 * For the recent research and state of art see papers about IMRCA and A
 * Minimum-Mean-Square-Error Noise Reduction Algorithm On Mel-Frequency Cepstra
 * For Robust Speech Recognition by Dong Yu and others
 **/
public class NoiseFilter {

    private double lambdaPower = 0.7;
    private double lambdaA = 0.995;
    private double lambdaB = 0.5;
    private double lambdaT = 0.85;

    private double muT = 0.2; //0.2

    private double maxGain = 20.0;

    private int smoothWindow = 4; //4

    private float[] power;
    private float[] noise;
    private float[] floor;
    private float[] peak;
    private final static double EPS = 1e-10;

    public float[] denoise(float[] input) {
        int length = input.length;

        if (power == null)
            initStatistics(input, length);

        updatePower(input);

        estimateEnvelope(power, noise);

        float[] signal = new float[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.max(power[i] - noise[i], 0);
        }

        estimateEnvelope(signal, floor);

        tempMasking(signal);

        powerBoosting(signal);

        float[] gain = new float[length];
        for (int i = 0; i < length; i++) {
            gain[i] = (float) (signal[i] / (power[i] + EPS));
            gain[i] = (float) Math.min(Math.max(gain[i], 1.0 / maxGain), maxGain);
        }
        float[] smoothGain = smooth(gain);

        for (int i = 0; i < length; i++) {
            input[i] *= smoothGain[i];
        }

        return input;
    }

    private void updatePower(float[] input) {
        for (int i = 0; i < input.length; i++) {
            power[i] = (float) (lambdaPower * power[i] + (1 - lambdaPower) * input[i]);
        }
    }

    private void estimateEnvelope(float[] signal, float[] envelope) {
        for (int i = 0; i < signal.length; i++) {
            if (signal[i] > envelope[i])
                envelope[i] = (float) (lambdaA * envelope[i] + (1 - lambdaA) * signal[i]);
            else
                envelope[i] = (float) (lambdaB * envelope[i] + (1 - lambdaB) * signal[i]);
        }
    }

    private float[] smooth(float[] gain) {
        float[] result = new float[gain.length];
        for (int i = 0; i < gain.length; i++) {
            int start = Math.max(i - smoothWindow, 0);
            int end = Math.min(i + smoothWindow + 1, gain.length);
            float sum = 0;
            for (int j = start; j < end; j++) {
                sum += gain[j];
            }
            result[i] = sum / (end - start);
        }
        return result;
    }

    private void powerBoosting(float[] signal) {
        for (int i = 0; i < signal.length; i++) {
            if (signal[i] < floor[i])
                signal[i] = floor[i];
        }
    }

    private void tempMasking(float[] signal) {
        for (int i = 0; i < signal.length; i++) {
            float in = signal[i];

            peak[i] *= lambdaT;
            if (signal[i] < lambdaT * peak[i])
                signal[i] = (float) (peak[i] * muT);

            if (in > peak[i])
                peak[i] = in;
        }
    }

    private void initStatistics(float[] input, int length) {
        /* no previous data, initialize the statistics */
        power = Arrays.copyOf(input, length);
        noise = Arrays.copyOf(input, length);
        floor = new float[length];
        peak = new float[length];
        for (int i = 0; i < length; i++) {
            floor[i] = (float) (input[i] / maxGain);
        }
    }
}

