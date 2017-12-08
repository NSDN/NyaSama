package cn.ac.nya.nsgdx.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import cn.ac.nya.nsgdx.utility.IObject;
import cn.ac.nya.nsgdx.utility.Renderer;
import cn.ac.nya.nsgdx.utility.Utility;

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

        vel = Utility.vec2(0, 0); acc = Utility.vec2(0, 0);
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
