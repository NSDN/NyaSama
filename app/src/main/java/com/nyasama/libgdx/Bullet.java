package com.nyasama.libgdx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.nyasama.libgdx.utility.IObject;
import com.nyasama.libgdx.utility.Renderer;

/**
 * Created by drzzm on 2017.12.3.
 */

public class Bullet implements IObject {

    private Vector2 pos;
    private Vector2 vel;
    private Vector2 acc;
    private Vector2 dir;

    private Texture tex;
    private float scale;
    private float r, g, b, a;

    public boolean grazed;

    public Bullet(Vector2 pos, Vector2 dir, Texture tex, float scale) {
        this.pos = pos; this.dir = dir;
        this.tex = tex; this.scale = scale;

        vel = Vector2.Zero; acc = Vector2.Zero;
        r = g = b = a = 1.0F;

        grazed = false;
    }

    public void onUpdate(int t) {
        vel = vel.add(acc);
        pos = pos.add(vel);
    }

    public void onRender(Renderer renderer) {
        renderer.draw(tex, pos.x, pos.y, dir.angle(), scale, r, g, b, a);
    }

}
