package com.nyasama.libgdx.utility;

import com.badlogic.gdx.graphics.Texture;

/**
 * Created by D.zzm on 2015.6.17.
 */

public class BulletObject {
    public final static int JUDGE_GRAZE = 1;
    public final static int JUDGE_MISS = 2;
    public final static int JUDGE_AWAY = 3;

    public float x;
    public float y;
    public float dx;
    public float dy;
    public int Rotate;
    public boolean Direction;
    public boolean IsEnabled;
    public boolean IsGrazed;
    public Texture Tex;
    public float ScaleXY;
    public float r;
    public float g;
    public float b;
    public float a;

    public BulletObject() {
        x = 0;
        y = 0;
        dx = 0;
        dy = 0;
        Rotate = 0;
        Direction = true;
        IsEnabled = false;
        IsGrazed = true;
        Tex = null;
        ScaleXY = 0;
        r = 0;
        g = 0;
        b = 0;
        a = 0;
    }

    public void Init() {
        x = 0;
        y = 0;
        dx = 0;
        dy = 0;
        Rotate = 0;
        Direction = true;
        IsEnabled = false;
        IsGrazed = true;
        Tex = null;
        ScaleXY = 0;
        r = 0;
        g = 0;
        b = 0;
        a = 0;
    }

    public boolean PreJudge(float pX, float pY, float Range) {
        return Math.abs((double) pX - (double) x) < (double) Range && Math.abs((double) pY - (double) y) < (double) Range;
    }

    public int Judge(float pX, float pY, float Min, float Max) {
        if (Utility.Distance(x, y, pX, pY) > Max) return JUDGE_AWAY;
        else if (Utility.Distance(x, y, pX, pY) > Min) return JUDGE_GRAZE;
        else return JUDGE_MISS;

    }
}
