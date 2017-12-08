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

    private Texture tex;

    protected Vector2 pos;
    private Vector2 dir;
    private Vector2 eye;

    public Player(Texture tex) {
        this.tex = tex;
        pos = Utility.vec2(0, 0);
        dir = Utility.vec2(0, 0);
    }

    public void flash(Vector2 vec) {
        pos = vec;
    }

    public void move(Vector2 vec) {
        pos = pos.add(vec);
    }

    public void look(Vector2 vec) {
        eye = vec;
    }

    @Override
    public Result onUpdate(int t) {
        dir = eye.sub(pos);
        return Result.DONE;
    }

    @Override
    public Result onRender(Renderer renderer) {
        renderer.draw(tex, pos.x, pos.y, dir.angle(), 1.0F);
        return Result.DONE;
    }
}
