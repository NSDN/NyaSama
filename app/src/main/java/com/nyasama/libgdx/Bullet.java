package com.nyasama.libgdx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.nyasama.libgdx.utility.IObject;
import com.nyasama.libgdx.utility.Renderer;

/**
 * Created by drzzm on 2017.12.3.
 */

public abstract class Bullet implements IObject {

    protected Vector2 pos;
    protected Vector2 vel;
    protected Vector2 acc;
    protected Vector2 dir;

    private Texture tex;
    protected float scale;
    protected float r, g, b, a;

    public boolean grazed;

    public Bullet(Vector2 pos, Vector2 dir, Texture tex) {
        this(pos, dir, tex, 1.0F);
    }

    public Bullet(Vector2 pos, Vector2 dir, Texture tex, float scale) {
        this.pos = pos; this.dir = dir;
        this.tex = tex; this.scale = scale;

        vel = Vector2.Zero; acc = Vector2.Zero;
        r = g = b = a = 1.0F;

        grazed = false;
    }

    public Result onUpdate(int t) {
        vel = vel.add(acc);
        pos = pos.add(vel);
        return Result.DONE;
    }

    public Result onRender(Renderer renderer) {
        renderer.draw(tex, pos.x, pos.y, dir.angle(), scale, r, g, b, a);
        return Result.DONE;
    }

}
