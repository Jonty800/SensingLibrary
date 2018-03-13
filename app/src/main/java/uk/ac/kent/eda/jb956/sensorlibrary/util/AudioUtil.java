package uk.ac.kent.eda.jb956.sensorlibrary.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

class AudioUtil {

    public static double soundPressureLevel(final float[] buffer) {
        double value = Math.pow(localEnergy(buffer), 0.5);
        value = value / buffer.length;
        return linearToDecibel(value);
    }

    private static double linearToDecibel(double value) {
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

    private static double localEnergy(final float[] buffer) {
        double power = 0.0D;
        for (float element : buffer) {
            power += element * element;
        }
        return power;
    }

    private static double prior;

    public static void applyPreemphasis(float[] in, double preemphasisFactor) {
        // set the prior value for the next Audio
        double nextPrior = prior;
        if (in.length > 0) {
            nextPrior = in[in.length - 1];
        }
        if (in.length > 1 && preemphasisFactor != 0.0) {
            // do preemphasis
            double current;
            double previous = in[0];
            in[0] = (float) (previous - preemphasisFactor * prior);
            for (int i = 1; i < in.length; i++) {
                current = in[i];
                in[i] = (float) (current - preemphasisFactor * previous);
                previous = current;
            }
        }
        prior = nextPrior;
    }

    public static float[] getMFCCFromInputBuffer(float[] input, int samplingRate, int numOfCep) {
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

    public static float[] getMagnitudeSpectrum(float frame[]) {
        float magSpectrum[] = new float[frame.length];
        FFT fft = new FFT(frame.length, new HammingWindow());
        // calculate FFT for current frame

        fft.forwardTransform(frame);

        // calculate magnitude spectrum
        for (int k = 0; k < frame.length / 2; k++) {
            magSpectrum[frame.length / 2 + k] = fft.modulus(frame, frame.length / 2 - 1 - k);
            magSpectrum[frame.length / 2 - 1 - k] = magSpectrum[frame.length / 2 + k];
        }

        return magSpectrum;
    }

    public static int[] getTopMagnitudes(float[] bins, int numberOfMagnitudes, float samplingRate) {
        double numOfBins = bins.length;
        List<Float> target = new ArrayList<>();
        double freqPerBin = (double) samplingRate / numOfBins;
        for (int i = 0; i < numOfBins; i++) {
            double freq = (double) i * freqPerBin;
            if (freq >= 300 && freq <= 3400) {
                target.add(bins[i]);
            }
        }

        float[] subsample = subsample(target, 30);
        return indexesOfTopElements(subsample, numberOfMagnitudes);
    }

    private static float[] subsample(List<Float> input, int amount) {
        int count = 0;
        float[] target2 = new float[input.size()];
        for (int i = 0; i < target2.length; i += amount) {
            if (i + amount > target2.length)
                continue;
            List<Float> sample = input.subList(i, i + amount);
            float average = arrayAverage(sample);
            target2[count++] = average;
        }
        return Arrays.copyOf(target2, count);
    }

    public static float[] subsample(float[] input, int amount) {
        int count = 0;
        float[] target2 = new float[input.length];
        for (int i = 0; i < target2.length; i += amount) {
            if (i + amount > target2.length)
                continue;
            float[] sample = Arrays.copyOfRange(input, i, i + amount);
            float average = arrayAverage(sample);
            target2[count++] = average;
        }
        return Arrays.copyOf(target2, count);
    }

    private static int[] indexesOfTopElements(float[] orig, int N) {
        float[] copy = Arrays.copyOf(orig, orig.length);
        Arrays.sort(copy);
        float[] honey = Arrays.copyOfRange(copy, copy.length - N, copy.length);
        int[] result = new int[N];
        int resultPos = 0;
        for (int i = 0; i < orig.length; i++) {
            float onTrial = orig[i];
            int index = Arrays.binarySearch(honey, onTrial);
            if (resultPos + 1 > result.length)
                continue;
            if (index < 0) continue;
            result[resultPos++] = i;
        }
        return result;
    }

    private static float arrayAverage(float[] marks) {
        float sum = 0;
        if (marks.length != 0) {
            for (float mark : marks) {
                sum += mark;
            }
            return sum / marks.length;
        }
        return sum;
    }

    private static float arrayAverage(List<Float> marks) {
        float sum = 0;
        if (!marks.isEmpty()) {
            for (float mark : marks) {
                sum += mark;
            }
            return sum / marks.size();
        }
        return sum;
    }
}
