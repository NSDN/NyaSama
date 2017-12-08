package cn.ac.nya.nsgdx.utility;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Created by D.zzm on 2015.6.17.
 */
public class Renderer {

    private SpriteBatch batch;
    private BitmapFont font;

    public void begin() {
        batch.begin();
    }

    public void end() {
        batch.end();
    }

    public void drawString(float x, float y, float scale, Color color, String str) {
        font.getData().setScale(scale);
        font.setColor(color);
        font.draw(batch, str, x, y);
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
        int Width = texture.getWidth(), Height = texture.getHeight();
        batch.setColor(r, g, b, a);
        batch.draw(texture,
                x - (float) Width / 2.0F, y - (float) Height / 2.0F,
                (float) Width / 2.0F, (float) Height / 2.0F,
                Width, Height, scale, scale, rotate,
                0, 0, Width, Height, false, false
        );
        batch.setColor(1, 1, 1, 1);
    }

    public Renderer() {
        batch = new SpriteBatch();
        font = new BitmapFont();
    }

}