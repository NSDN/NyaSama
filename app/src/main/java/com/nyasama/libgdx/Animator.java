package com.nyasama.libgdx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.nyasama.libgdx.utility.IObject;
import com.nyasama.libgdx.utility.Renderer;

/**
 * Created by drzzm on 2017.12.3.
 */

public class Animator implements IObject {

    private Texture tex;

    private class State {
        private Vector2 pos;

        private float rotate;
        private float scale;
        private float r, g, b, a;

        private State() {
            pos = Vector2.Zero;
            rotate = scale = 0;
            r = g = b = a = 1.0F;
        }
    }

    private State src, dst;

    private int cnt;
    public int length;

    public Animator(Texture tex, int length) {
        this.tex = tex;
        this.length = length;

        src = new State(); dst = new State();
        cnt = 0;
    }

    public Animator setSource(Vector2 pos, float rotate, float scale) {
        return setSource(pos, rotate, scale, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public Animator setSource(Vector2 pos, float rotate, float scale, float r, float g, float b, float a) {
        src.pos = pos; src.rotate = rotate; src.scale = scale;
        src.r = r; src.g = g; src.b = b; src.a = a;
        return this;
    }

    public Animator setDest(Vector2 pos, float rotate, float scale) {
        return setDest(pos, rotate, scale, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public Animator setDest(Vector2 pos, float rotate, float scale, float r, float g, float b, float a) {
        dst.pos = pos; dst.rotate = rotate; dst.scale = scale;
        dst.r = r; dst.g = g; dst.b = b; dst.a = a;
        return this;
    }

    public void onUpdate(int t) {

    }

    public void onRender(Renderer renderer) {

    }

}
