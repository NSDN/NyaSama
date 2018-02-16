package cn.ac.nya.nsgdx.utility;

import com.badlogic.gdx.math.Vector2;

/**
 * Created by D.zzm on 2015.6.16.
 */
public class Utility {

    public static class Color3 {
        public float r, g, b;
    }

    public static Color3 hsv2RGB(float h, float s, float v) {
        h = h % 360;
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = v - c;
        float rt, gt, bt;
        if (h >= 0 && h < 60) {
            rt = c; gt = x; bt = 0;
        } else if (h >= 60 && h < 120) {
            rt = x; gt = c; bt = 0;
        } else if (h >= 120 && h < 180) {
            rt = 0; gt = c; bt = x;
        } else if (h >= 180 && h < 240) {
            rt = 0; gt = x; bt = c;
        } else if (h >= 240 && h < 300) {
            rt = x; gt = 0; bt = c;
        } else if (h >= 300 && h < 360) {
            rt = c; gt = 0; bt = x;
        } else {
            rt = gt = bt = 0;
        }
        Color3 color = new Color3();
        color.r = rt + m;
        color.g = gt + m;
        color.b = bt + m;
        return color;
    }

    public static float dist2(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow((double) x2 - (double) x1, (double) 2) + Math.pow((double) y2 - (double) y1, (double) 2));
    }

    public static Vector2 vec2(float x, float y) {
        return Vector2.Zero.cpy().set(x, y);
    }

    public static Vector2 vec2h(float x, float y) {
        return vec2(x, y).scl(0.5F);
    }

}
