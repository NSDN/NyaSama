package com.nyasama.libgdx.utility;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Created by D.zzm on 2015.6.17.
 */


public class DrawCore {

    private SpriteBatch Batch;
    private BitmapFont Font;

    public void DrawBegin() {
        Batch.begin();
    }

    public void DrawEnd() {
        Batch.end();
    }

    public void DrawString(float x, float y, float Scale, Color color, String Str) {
        Font.getData().setScale(Scale);
        Font.setColor(color);
        Font.draw(Batch, Str, x, y);
    }

    public void DrawBack(Texture Tex) {
        Batch.draw(Tex, 0, 0, Tex.getWidth(), Tex.getHeight());
    }

    public void DrawPic(Texture Tex, float x, float y, int Rotate) {
        DrawPic(Tex, x, y, Rotate, 1, 1);
    }

    public void DrawPic(Texture Tex, float x, float y, float alpha) {
        DrawPic(Tex, x, y, 0, 1, 1, 1, 1, 1, alpha);
    }

    public void DrawPic(Texture Tex, float x, float y, float Rotate, float ScX, float ScY) {
        DrawPic(Tex, x, y, Rotate, ScX, ScY, 1, 1, 1, 1);
    }

    public void DrawPic(Texture Tex, float x, float y, float Rotate, float ScX, float ScY, float r, float g, float b, float a) {
        int Width = Tex.getWidth(), Height = Tex.getHeight();
        Batch.setColor(r, g, b, a);
        Batch.draw(Tex, x - Width / 2 * ScX, y - Height / 2 * ScY, Width / 2, Height / 2, Width, Height, ScX, ScY, Rotate, 0, 0, Width, Height, false, false);
        Batch.setColor(1, 1, 1, 1);
    }

    public DrawCore() {
        Batch = new SpriteBatch();
        Font = new BitmapFont();
    }
}