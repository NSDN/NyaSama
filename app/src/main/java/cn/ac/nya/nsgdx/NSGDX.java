package cn.ac.nya.nsgdx;

import cn.ac.nya.nsgdx.entity.Bullet;
import cn.ac.nya.nsgdx.entity.Exectuor;
import cn.ac.nya.nsgdx.entity.Player;
import cn.ac.nya.nsgdx.utility.IObject;
import cn.ac.nya.nsgdx.utility.Utility;
import cn.ac.nya.nsgdx.entity.Animator;
import cn.ac.nya.nsgdx.utility.Renderer;
import cn.ac.nya.nsgdx.entity.Animator.Interpolation;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.ApplicationAdapter;

import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by drzzm on 2017.12.7.
 */

public class NSGDX extends ApplicationAdapter {

    private Runnable exitCallback;

    public NSGDX setOnExitCallback(Runnable exitCallback) {
        this.exitCallback = exitCallback;
        return this;
    }

	private Renderer renderer;
	private float devWidth, devHeight;
	private int counter = 0;

	private LinkedList<IObject> objectPool = new LinkedList<>();
    private LinkedList<IObject> cachePool = new LinkedList<>();
	private ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1);

	private Bullet theBullet;
	private Player thePlayer;

	private LinkedHashMap<String, Texture> textureManager = new LinkedHashMap<>();
	private Music testMusic;
	private Sound testSound;

	@Override
	public void create() {
		renderer = new Renderer();
		devWidth = Gdx.graphics.getWidth();
		devHeight = Gdx.graphics.getHeight();

		loadAssets();
		initEntity();

		threadPoolExecutor.scheduleWithFixedDelay(() -> {
		    cachePool.clear(); cachePool.addAll(objectPool);
            for (IObject i : cachePool) {
                if (i.onUpdate(counter) == IObject.Result.END) {
                    objectPool.remove(i);
                }
            }
            counter += 1;
		}, 1000, 10, TimeUnit.MILLISECONDS);

	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		renderer.begin();
        for (IObject i : objectPool) {
            i.onRender(renderer);
        }
		renderer.end();

		renderer.begin();
		renderer.drawString(4, 16, 1, Color.WHITE, "counter: " + counter);
		renderer.end();

	}

    @Override
    public void dispose() {
        threadPoolExecutor.shutdown();
    }

	private void Tld(String name) {
	    textureManager.put(name,
                new Texture("textures/" + name + ".png")
        );
    }

    private Texture Tget(String name) {
        return textureManager.get(name);
    }

	private void loadAssets() {
        Tld("nsdn_base"); Tld("nsdn_nya"); Tld("nyasama");
        for (int i = 1; i <= 5; i++) Tld("screen_" + i);
        testMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/bgm.ogg"));
        testSound = Gdx.audio.newSound(Gdx.files.internal("sounds/biu.ogg"));
    }

    private void initEntity() {
        theBullet = new Bullet(null, null, null) {
            @Override
            public Result onUpdate(int t) {
                if (pos.x < 0) pos.x = 0;
                if (pos.x > devWidth) pos.x = devWidth;
                if (pos.y < 0) pos.y = 0;
                if (pos.y > devHeight) pos.y = devHeight;

                return super.onUpdate(t);
            }
        };

        thePlayer = new Player(null) {
            private void doControl(Player player) {
                Vector2 vel = Utility.vec2(0, 0);

                if (Gdx.input.isKeyPressed(Input.Keys.UP)) vel.y = 1;
                if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) vel.y = -1;
                if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) vel.x = -1;
                if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) vel.x = 1;

                if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT))
                    vel.scl(2.0F);
                else
                    vel.scl(5.0F);

                if (Gdx.input.isTouched()) {
                    vel.x = Gdx.input.getDeltaX();
                    vel.y = -Gdx.input.getDeltaY();
                }

                player.move(vel);
            }

            @Override
            public Result onUpdate(int t) {
                doControl(this);

                if (pos.x < 0) pos.x = 0;
                if (pos.x > devWidth) pos.x = devWidth;
                if (pos.y < 0) pos.y = 0;
                if (pos.y > devHeight) pos.y = devHeight;

                return super.onUpdate(t);
            }
        };

        //objectPool.add(theBullet);
        //objectPool.add(thePlayer);

        objectPool.add(new Animator()
                .start(Utility.vec2h(devWidth, devHeight), 180.0F, 2.0F, 0.0F)
                .next(Tget("nsdn_base"), Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 180.0F, 2.0F, 0.0F)
                .next(Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                .next(Interpolation.LINEAR, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                .next(Interpolation.QUADRATIC, 150, Utility.vec2h(devWidth, devHeight), -90.0F, 0.25F, 0.0F)
        );
        objectPool.add(new Animator()
                .start(Tget("nsdn_nya"), Utility.vec2h(devWidth, devHeight), -90.0F, 2.0F, 0.0F)
                .next(Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                .next(Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                .next(Interpolation.QUADRATIC, 150, Utility.vec2h(devWidth, devHeight), 45.0F, 0.25F, 0.0F)
        );

        objectPool.add(new Exectuor() {
            @Override
            public Result onUpdate(int t) {
                if (t == 500) {
                    objectPool.add(new Animator()
                            .start(Tget("screen_1"), Utility.vec2h(devWidth, devHeight).add(devWidth / 2, 0), 0.0F, 1.5F, 0.0F)
                            .next(Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                            .next(Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                            .next(Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight).add(-devWidth / 2, 0), 0.0F, 1.5F, 0.0F)

                            .next(Tget("screen_2"), Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight).add(devWidth / 2, 0), 0.0F, 1.5F, 0.0F)
                            .next(Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                            .next(Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                            .next(Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight).add(-devWidth / 2, 0), 0.0F, 1.5F, 0.0F)

                            .next(Tget("screen_3"), Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight).add(devWidth / 2, 0), 0.0F, 1.5F, 0.0F)
                            .next(Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                            .next(Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                            .next(Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight).add(-devWidth / 2, 0), 0.0F, 1.5F, 0.0F)

                            .next(Tget("screen_4"), Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight).add(devWidth / 2, 0), 0.0F, 1.5F, 0.0F)
                            .next(Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                            .next(Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                            .next(Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight).add(-devWidth / 2, 0), 0.0F, 1.5F, 0.0F)

                            .next(Tget("screen_5"), Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight).add(devWidth / 2, 0), 0.0F, 1.5F, 0.0F)
                            .next(Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F, 1.0F)
                            .next(Interpolation.LINEAR, 200, Utility.vec2h(devWidth, devHeight), 0.0F, 1.0F)
                            .next(Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight).add(-devWidth / 2, 0), 0.0F, 1.5F, 0.0F)
                    );

                    return Result.END;
                }
                return Result.DONE;
            }
        });
        objectPool.add(new Exectuor() {
            @Override
            public Result onUpdate(int t) {
                if (t == 3000) {
                    objectPool.add(new Animator(Tget("nyasama"))
                            .start(Utility.vec2h(devWidth, devHeight), 0.0F, 0.5F, 0.0F)
                            .next(Interpolation.SINE, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 0.5F, 1.0F)
                            .next(Interpolation.LINEAR, 300, Utility.vec2h(devWidth, devHeight), 0.0F, 0.5F)
                            .next(Interpolation.QUADRATIC, 100, Utility.vec2h(devWidth, devHeight), 0.0F, 0.5F, 0.0F)
                    );

                    return Result.END;
                }
                return Result.DONE;
            }
        });
        objectPool.add(new Exectuor() {
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
