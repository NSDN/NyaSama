package cn.ac.nya.nsgdx;

import cn.ac.nya.nsgdx.entity.Exectuor;
import cn.ac.nya.nsgdx.utility.ObjectPoolCluster;
import cn.ac.nya.nsgdx.utility.Renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.ApplicationAdapter;

import static cn.ac.nya.nsgdx.utility.Renderer.Texture;

import java.util.LinkedHashMap;

/**
 * Created by drzzm on 2017.12.7.
 */

public abstract class NSGDX extends ApplicationAdapter {

	private Renderer renderer;

	protected float devWidth, devHeight;

    protected ObjectPoolCluster poolCluster = new ObjectPoolCluster(2048, 4);

	private LinkedHashMap<String, Texture> textureManager = new LinkedHashMap<>();
    protected LinkedHashMap<String, Sound> soundManager = new LinkedHashMap<>();
    protected LinkedHashMap<String, Music> musicManager = new LinkedHashMap<>();

    protected void load(String name) {
        textureManager.put(name,
                new Texture("textures/" + name + ".png")
        );
    }

    protected Texture get(String name) {
        return textureManager.get(name);
    }

    @Override
	public void create() {
		renderer = new Renderer();
		devWidth = Gdx.graphics.getWidth();
		devHeight = Gdx.graphics.getHeight();

		loadAssets();
		initEntity();
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		poolCluster.tick();
		poolCluster.render(renderer);

		render(renderer);
	}

    @Override
    public void dispose() {
        poolCluster.close();
    }

	protected abstract void loadAssets();

    protected abstract void initEntity();

    protected abstract void render(Renderer renderer);

}
