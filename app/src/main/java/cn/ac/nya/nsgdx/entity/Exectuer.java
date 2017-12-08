package cn.ac.nya.nsgdx.entity;

import cn.ac.nya.nsgdx.utility.IObject;
import cn.ac.nya.nsgdx.utility.Renderer;

/**
 * Created by drzzm on 2017.12.7.
 */

public abstract class Exectuer implements IObject {

    @Override
    public abstract Result onUpdate(int t);

    @Override
    public Result onRender(Renderer renderer) {
        return null;
    }

}
