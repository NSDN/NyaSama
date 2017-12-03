package com.nyasama.libgdx.utility.old;

import com.badlogic.gdx.graphics.Texture;
import com.nyasama.libgdx.utility.Renderer;

/**
 * Created by D.zzm on 2015.6.16.
 * Translated from VB.NET, ThSAG.
 */

public class Animation {


    private Texture ImgSrc;
    private long Counter;
    private static Renderer Engine;

    private double AniValue;

    public Animation(Texture Tex, long StartTime) {
        if (Engine == null) Engine = new Renderer();
        ImgSrc = Tex;
        Counter = StartTime;
        AniValue = 0D;
    }

    private void Draw(float x, float y, float Alpha) {
        Draw(x, y, 0F, 0F, Alpha);
    }

    private void Draw(float x, float y, float Angle, float Scale, float Alpha) {
        Engine.draw(ImgSrc, x, y, Angle, Scale, 1, 1, 1, Alpha);
    }

    private void Draw(double x, double y, double Angle, double Scale, double Alpha) {
        Engine.begin();
        Engine.draw(ImgSrc, (float) x, (float) y, (float) Angle, (float) Scale, 1, 1, 1, (float) Alpha);
        Engine.end();
    }

    public boolean Fade(float x, float y, long NowTime) {
        return FadeIn(x, y, NowTime) & FadeOut(x, y, NowTime - 128);
    }

    public boolean FadeIn(float x, float y, long NowTime) {
        return FadeIn(x, y, 1, NowTime);
    }

    public boolean FadeIn(float x, float y, long NowTime, int TotalTime) {
        return Trans(x, y, 1, 0, 0, x, y, 1, 0, 255,
                TotalTime, NowTime);
    }

    public boolean FadeIn(float x, float y, float Scale, long NowTime) {
        return Trans(x, y, Scale, 0, 0, x, y, Scale, 0, 255,
                128, NowTime);
    }

    public boolean FadeOut(float x, float y, long NowTime) {
        return FadeOut(x, y, 1, NowTime);
    }

    public boolean FadeOut(float x, float y, long NowTime, int TotalTime) {
        return Trans(x, y, 1, 0, 255, x, y, 1, 0, 0,
                TotalTime, NowTime);
    }

    public boolean FadeOut(float x, float y, float Scale, long NowTime) {
        return Trans(x, y, Scale, 0, 255, x, y, Scale, 0, 0,
                128, NowTime);
    }

    public boolean ScaleIn(float x, float y, long NowTime) {
        return ScaleIn(x, y, 0, NowTime, false);
    }

    public boolean ScaleIn(float x, float y, long NowTime, boolean WithFade) {
        return ScaleIn(x, y, 0, NowTime, WithFade);
    }

    public boolean ScaleIn(float x, float y, float StaticAngle, long NowTime) {
        return ScaleIn(x, y, StaticAngle, 1, NowTime, false);
    }

    public boolean ScaleIn(float x, float y, float StaticAngle, long NowTime, boolean WithFade) {
        return ScaleIn(x, y, StaticAngle, 1, NowTime, WithFade);
    }

    public boolean ScaleIn(float x, float y, float StaticAngle, float MaxScale, long NowTime) {
        return ScaleIn(x, y, StaticAngle, MaxScale, NowTime, false);
    }

    public boolean ScaleIn(float x, float y, float StaticAngle, float MaxScale, long NowTime, boolean WithFade) {
        if (WithFade) {
            return Trans(x, y, 0, StaticAngle, 0, x, y, MaxScale, StaticAngle, 255,
                    50, NowTime);
        } else {
            return Trans(x, y, 0, StaticAngle, 0, x, y, MaxScale, StaticAngle, 255,
                    50, NowTime);
        }
    }

    public boolean ScaleOut(float x, float y, long NowTime) {
        return ScaleOut(x, y, 0, NowTime, false);
    }

    public boolean ScaleOut(float x, float y, long NowTime, boolean WithFade) {
        return ScaleOut(x, y, 0, NowTime, WithFade);
    }

    public boolean ScaleOut(float x, float y, float StaticAngle, long NowTime) {
        return ScaleOut(x, y, StaticAngle, 1, NowTime, false);
    }

    public boolean ScaleOut(float x, float y, float StaticAngle, long NowTime, boolean WithFade) {
        return ScaleOut(x, y, StaticAngle, 1, NowTime, WithFade);
    }

    public boolean ScaleOut(float x, float y, float StaticAngle, float MaxScale, long NowTime) {
        return ScaleOut(x, y, StaticAngle, MaxScale, NowTime, false);
    }

    public boolean ScaleOut(float x, float y, float StaticAngle, float MaxScale, long NowTime, boolean WithFade) {
        if (WithFade) {
            return Trans(x, y, MaxScale, StaticAngle, 255, x, y, 0, StaticAngle, 0,
                    50, NowTime);
        } else {
            return Trans(x, y, MaxScale, StaticAngle, 255, x, y, 0, StaticAngle, 255,
                    50, NowTime);
        }
    }

    public void Rotate(float x, float y, float SpeedReduce, long NowTime) {
        Rotate(x, y, (NowTime - Counter) / SpeedReduce);
    }

    public void Rotate(float x, float y, float Angle) {
        Draw(x, y, Angle);
    }

    public boolean Trans(float sX, float sY, float sScale, float sAngle, float sFade, float tX, float tY, float tScale, float tAngle, float tFade,
                         long TotalTime, long NowTime) {
        if (NowTime - Counter < 0) {
            Draw(sX, sY, sAngle, sScale, sFade);
        } else if (NowTime - Counter <= TotalTime) {
            AniValue = Math.sin((NowTime - Counter) / (2 * TotalTime) * Math.PI);
            Draw(sX + (tX - sX) * AniValue,
                    sY + (tY - sY) * AniValue,
                    sAngle + (tAngle - sAngle) * AniValue,
                    sScale + (tScale - sScale) * AniValue,
                    sFade + (tFade - sFade) * AniValue);
        } else {
            return true;
        }
        return false;
    }

}
