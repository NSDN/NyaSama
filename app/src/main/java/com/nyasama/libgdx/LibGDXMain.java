package com.nyasama.libgdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;

import com.badlogic.gdx.math.Vector2;
import com.nyasama.libgdx.utility.IObject;
import com.nyasama.libgdx.utility.Renderer;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by D.zzm on 2014.12.14.
 */

public class LibGDXMain extends ApplicationAdapter {

    private Renderer renderer;
    private float devWidth, devHeight;
    private int counter = 0;

    private Timer theTimer = new Timer();
    private LinkedList<IObject> objectPool = new LinkedList<>();

    private Bullet theBullet;
    private Player thePlayer;

    // Texture tex = new Texture("tex.png");
    // Music music = Gdx.audio.newMusic(Gdx.files.internal("music.ogg"));
    // Sound sound = Gdx.audio.newSound(Gdx.files.internal("sound.ogg"));

    @Override
    public void create() {
        renderer = new Renderer();
        devWidth = Gdx.graphics.getWidth();
        devHeight = Gdx.graphics.getHeight();

        theTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (objectPool.isEmpty()) return;
                for (IObject i : objectPool) {
                    if (i.onUpdate(counter) == IObject.Result.END) {
                        objectPool.remove(i);
                    }
                }
            }
        }, 0, 10);

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
            @Override
            public Result onUpdate(int t) {
                if (pos.x < 0) pos.x = 0;
                if (pos.x > devWidth) pos.x = devWidth;
                if (pos.y < 0) pos.y = 0;
                if (pos.y > devHeight) pos.y = devHeight;

                return super.onUpdate(t);
            }
        };

        objectPool.add(theBullet);
        objectPool.add(thePlayer);
    }

    @Override
    public void render() {
        doControl(thePlayer);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.begin();
        for (IObject i : objectPool) {
            i.onRender(renderer);
        }
        renderer.end();

        counter += 1;
    }

    private void doControl(Player player) {
        Vector2 vel = Vector2.Zero;

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
}

