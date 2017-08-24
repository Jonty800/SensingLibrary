package uk.ac.kent.eda.jb956.sensorlibrary.util;

import be.tarsos.dsp.mfcc.MFCC;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class AudioUtil {

    public static double soundPressureLevel(final float[] buffer) {
        double value = Math.pow(localEnergy(buffer), 0.5);
        value = value / buffer.length;
        return linearToDecibel(value);
    }

    public static double linearToDecibel(double value) {
        return 20.0D * Math.log10(value);
    }

    public static double calculateRMS(float[] floatBuffer) {
        double rms = 0.0;
        for (float aFloatBuffer : floatBuffer) {
            rms += aFloatBuffer * aFloatBuffer;
        }
        rms = rms / (double) floatBuffer.length;
        rms = Math.sqrt(rms);
        return rms;
    }

    public static double localEnergy(final float[] buffer) {
        double power = 0.0D;
        for (float element : buffer) {
            power += element * element;
        }
        return power;
    }

    public float[] getMFCCFromInputBuffer(float[] input, int samplingRate, int numOfCep) {
        MFCC mfcc = new MFCC(input.length, samplingRate, numOfCep, 40, 133.3334f, ((float) samplingRate) / 2f);
        // Magnitude Spectrum
        float bin[] = mfcc.magnitudeSpectrum(input);
        // get Mel Filterbank
        float fbank[] = mfcc.melFilter(bin, mfcc.getCenterFrequencies());
        // Non-linear transformation
        float f[] = mfcc.nonLinearTransformation(fbank);
        // Cepstral coefficients
        return mfcc.cepCoefficients(f);
    }
}
