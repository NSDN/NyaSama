package cn.ac.nya.nsgdx.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import cn.ac.nya.nsgdx.utility.IObject;
import cn.ac.nya.nsgdx.utility.Renderer;
import cn.ac.nya.nsgdx.utility.Utility;

/**
 * Created by drzzm on 2017.12.5.
 */

public abstract class Player implements IObject {

    protected Vector2 pos;
    protected Vector2 dir;
    protected Vector2 eye;

    private Texture[] tex;
    protected float scale;

    public Player(Texture tex) {
        this.tex = new Texture[] { tex };
        pos = Utility.vec2(0, 0);
        dir = Utility.vec2(0, 0);
        eye = Utility.vec2(0, 0);
        scale = 1.0F;
    }

    public Player(Texture[] tex) {
        this.tex = tex;
        pos = Utility.vec2(0, 0);
        dir = Utility.vec2(0, 0);
        eye = Utility.vec2(0, 0);
        scale = 1.0F;
    }

    public void flash(Vector2 vec) {
        pos = vec.cpy();
    }

    public void move(Vector2 vec) {
        pos = pos.add(vec);
    }

    public void look(Vector2 vec) {
        eye = vec.cpy();
    }

    @Override
    public Result onUpdate(int t) {
        dir = pos.cpy().sub(eye);
        return Result.DONE;
    }

    @Override
    public Result onRender(Renderer renderer) {
        for (Texture t : tex)
            renderer.draw(t, pos.x, pos.y, dir.angle(), scale);
        return Result.DONE;
    }
}
