package cn.ac.nya.nsgdx.entity;

import com.badlogic.gdx.math.Vector2;
import cn.ac.nya.nsgdx.utility.IObject;
import cn.ac.nya.nsgdx.utility.Renderer;
import cn.ac.nya.nsgdx.utility.Utility;

import static cn.ac.nya.nsgdx.utility.Renderer.Texture;

import java.util.LinkedList;

/**
 * Created by drzzm on 2017.12.3.
 */

public abstract class Bullet implements IObject {

    protected Vector2 pos;
    protected Vector2 vel;
    protected Vector2 acc;
    protected Vector2 aim;
    protected Vector2 dir;

    private Texture[] tex;
    protected float scale;
    protected float r, g, b, a;

    public boolean grazed;
    protected LinkedList<IObject> targets;

    public Bullet(Vector2 pos, Vector2 aim, Texture tex) {
        this(pos, aim, tex, 1.0F);
    }

    public Bullet(Vector2 pos, Vector2 aim, Texture tex, float scale) {
        this.pos = pos.cpy(); this.aim = aim.cpy();
        this.tex = new Texture[] { tex }; this.scale = scale;

        vel = Utility.vec2(0, 0); acc = Utility.vec2(0, 0);
        dir = Utility.vec2(0, 0);
        r = g = b = a = 1.0F;

        grazed = false;
        targets = new LinkedList<>();
    }

    public Bullet(Vector2 pos, Vector2 aim, Texture[] tex, float scale) {
        this.pos = pos.cpy(); this.aim = aim.cpy();
        this.tex = tex; this.scale = scale;

        vel = Utility.vec2(0, 0); acc = Utility.vec2(0, 0);
        dir = Utility.vec2(0, 0);
        r = g = b = a = 1.0F;

        grazed = false;
        targets = new LinkedList<>();
    }

    /*
    * TODO: 需检测玩家态, 玩家通常不可被销毁, 若销毁要先设置成销毁态, 使子弹注销玩家
    * */

    public Bullet register(IObject target) {
        if (targets.contains(target))
            targets.remove(target);
        targets.add(target);
        return this;
    }

    public Bullet deregister(IObject target) {
        if (targets.contains(target))
            targets.remove(target);
        return this;
    }

    public Bullet setVel(Vector2 vel) { this.vel = vel.cpy(); return this; }
    
    public Bullet setAcc(Vector2 acc) { this.acc = acc.cpy(); return this; }

    public Bullet setColor(float r, float g, float b) {
        return setColor(r, g, b, 1.0F);
    }

    public Bullet setColor(float r, float g, float b, float a) {
        this.r = r; this.g = g; this.b = b; this.a = a;
        return this;
    }

    public Result onUpdate(int t) {
        vel = vel.add(acc);
        pos = pos.add(vel);
        dir = pos.cpy().sub(aim);
        return Result.DONE;
    }

    public Result onRender(Renderer renderer) {
        for (Texture t : tex)
            renderer.draw(t, pos.x, pos.y, dir.angle(), scale, r, g, b, a);
        return Result.DONE;
    }

}
