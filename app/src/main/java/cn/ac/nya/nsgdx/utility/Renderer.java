package cn.ac.nya.nsgdx.utility;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Created by D.zzm on 2015.6.17.
 */
public class Renderer implements RenderUtil.IRenderer {

    public static class Texture extends com.badlogic.gdx.graphics.Texture implements RenderUtil.IDrawable {

        public Texture(String path) {
            super(path);
        }

    }

    private SpriteBatch batch;
    private BitmapFont font;

    @Override
    public void begin() {
        batch.begin();
    }

    @Override
    public void end() {
        batch.end();
    }

    @Override
    public void drawString(float x, float y, float scale, RenderUtil.Color4 color, String str) {
        font.getData().setScale(scale);
        batch.setColor(color.r, color.g, color.b, color.a);
        font.draw(batch, str, x, y);
        batch.setColor(1, 1, 1, 1);
    }

    @Override
    public void draw(RenderUtil.IDrawable drawable, float x, float y, float rotate, float scale, RenderUtil.Color4 color) {
        if (drawable instanceof Texture) {
            Texture texture = (Texture) drawable;

            int Width = texture.getWidth(), Height = texture.getHeight();
            batch.setColor(color.r, color.g, color.b, color.a);
            batch.draw(texture,
                    x - (float) Width / 2.0F, y - (float) Height / 2.0F,
                    (float) Width / 2.0F, (float) Height / 2.0F,
                    Width, Height, scale, scale, rotate,
                    0, 0, Width, Height, false, false
            );
            batch.setColor(1, 1, 1, 1);
        }

    }

    public Renderer() {
        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    public void drawString(float x, float y, float scale, Color color, String str) {
        font.getData().setScale(scale);
        batch.setColor(color);
        font.draw(batch, str, x, y);
        batch.setColor(1, 1, 1, 1);
    }

    public void draw(Texture texture) {
        batch.draw(texture, 0, 0, texture.getWidth(), texture.getHeight());
    }

    public void draw(Texture texture, float x, float y, int rotate) {
        draw(texture, x, y, rotate, 1);
    }

    public void draw(Texture texture, float x, float y, float alpha) {
        draw(texture, x, y, 0, 1, 1, 1, 1, alpha);
    }

    public void draw(Texture texture, float x, float y, float rotate, float scale) {
        draw(texture, x, y, rotate, scale, 1, 1, 1, 1);
    }

    public void draw(Texture texture, float x, float y, float rotate, float scale, float r, float g, float b, float a) {
        draw(texture, x, y, rotate, scale, RenderUtil.color4(r, g, b, a));
    }



}