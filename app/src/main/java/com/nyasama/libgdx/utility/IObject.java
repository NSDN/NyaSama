package com.nyasama.libgdx.utility;

/**
 * Created by drzzm on 2017.12.4.
 */

public interface IObject {

    enum Result {
        DONE, END
    }

    Result onUpdate(int t);
    Result onRender(Renderer renderer);
}
