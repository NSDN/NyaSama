package cn.ac.nya.nsgdx;

import cn.ac.nya.nsgdx.entity.Animator;
import cn.ac.nya.nsgdx.entity.Exectuor;
import cn.ac.nya.nsgdx.utility.Renderer;
import cn.ac.nya.nsgdx.utility.Utility;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

/**
 * Created by drzzm on 2018.2.14.
 */
public class NyaAbout extends NSGDX {

    private Runnable exitCallback;

    public NSGDX setOnExitCallback(Runnable exitCallback) {
        this.exitCallback = exitCallback;
        return this;
    }

    @Override
    protected void render(Renderer renderer) {
        renderer.begin();
        renderer.drawString(4, 16, 1, Color.WHITE, "frame: " + poolCluster.tickTime());
        renderer.end();
    }

    @Override
    protected void loadAssets() {
        load("nsdn_base"); load("nsdn_nya"); load("nyasama");
        for (int i = 1; i <= 5; i++) load("screen_" + i);

        soundManager.put("biu", Gdx.audio.newSound(Gdx.files.internal("sounds/biu.ogg")));
        musicManager.put("bgm", Gdx.audio.newMusic(Gdx.files.internal("sounds/bgm.ogg")));
    }

    @Override
    protected void initEntity() {
        poolCluster.add(new Animator()
                .start(Utility.vec2h(devWidth, devHeight), 180.0F, 2.0F, 0.0F)
                .next(get("nsdn_base"), Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 180.0F, 2.0F, 0.0F)
                .next(Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                .next(Animator.Interpolation.LINEAR, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                .next(Animator.Interpolation.QUADRATIC, 150, Utility.vec2h(devWidth, devHeight), -90.0F, 0.25F, 0.0F)
        );
        poolCluster.add(new Animator()
                .start(get("nsdn_nya"), Utility.vec2h(devWidth, devHeight), -90.0F, 2.0F, 0.0F)
                .next(Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                .next(Animator.Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                .next(Animator.Interpolation.QUADRATIC, 150, Utility.vec2h(devWidth, devHeight), 45.0F, 0.25F, 0.0F)
        );

        poolCluster.add(new Exectuor() {
            @Override
            public Result onUpdate(int t) {
                if (t == 500) {
                    poolCluster.add(new Animator()
                            .start(get("screen_1"), Utility.vec2h(devWidth, devHeight).add(256, 0), 0.0F, 1.5F, 0.0F)
                            .next(Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                            .next(Animator.Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                            .next(Animator.Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight).add(-256, 0), 0.0F, 1.5F, 0.0F)

                            .next(get("screen_2"), Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight).add(256, 0), 0.0F, 1.5F, 0.0F)
                            .next(Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                            .next(Animator.Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                            .next(Animator.Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight).add(-256, 0), 0.0F, 1.5F, 0.0F)

                            .next(get("screen_3"), Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight).add(256, 0), 0.0F, 1.5F, 0.0F)
                            .next(Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                            .next(Animator.Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                            .next(Animator.Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight).add(-256, 0), 0.0F, 1.5F, 0.0F)

                            .next(get("screen_4"), Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight).add(256, 0), 0.0F, 1.5F, 0.0F)
                            .next(Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                            .next(Animator.Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                            .next(Animator.Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight).add(-256, 0), 0.0F, 1.5F, 0.0F)

                            .next(get("screen_5"), Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight).add(256, 0), 0.0F, 1.5F, 0.0F)
                            .next(Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                            .next(Animator.Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                            .next(Animator.Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight).add(-256, 0), 0.0F, 1.5F, 0.0F)
                    );

                    return Result.END;
                }
                return Result.DONE;
            }
        });
        poolCluster.add(new Exectuor() {
            @Override
            public Result onUpdate(int t) {
                if (t == 3000) {
                    poolCluster.add(new Animator(get("nyasama"))
                            .start(Utility.vec2h(devWidth, devHeight), 0.0F, 0.5F, 0.0F)
                            .next(Animator.Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 0.5F, 1.0F)
                            .next(Animator.Interpolation.LINEAR, 300, Utility.vec2h(devWidth, devHeight), 0.0F, 0.5F)
                            .next(Animator.Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 0.5F, 0.0F)
                    );

                    return Result.END;
                }
                return Result.DONE;
            }
        });
        poolCluster.add(new Exectuor() {
            @Override
            public Result onUpdate(int t) {
                if (t == 3600) {
                    exitCallback.run();
                    return Result.END;
                }
                return Result.DONE;
            }
        });
    }

}
