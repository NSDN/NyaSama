package com.nyasama.libgdx.utility;

/**
 * Created by D.zzm on 2015.6.16.
 */
public class Utility {

    public static float dist2(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow((double) x2 - (double) x1, (double) 2) + Math.pow((double) y2 - (double) y1, (double) 2));
    }

}
