package com.nyasama.libgdx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.nyasama.libgdx.utility.IObject;
import com.nyasama.libgdx.utility.Renderer;

import java.util.LinkedList;

/**
 * Created by drzzm on 2017.12.3.
 */

public class Animator implements IObject {

    private Texture tex;

    enum Interpolation {
        LINEAR, QUADRATIC, SINE
    }

    private class State {
        private final Interpolation inter;
        private int length;

        private Vector2 pos;
        private float rotate;
        private float scale;
        private float r, g, b, a;

        private State(Interpolation inter) {
            this.inter = inter;
            pos = Vector2.Zero;
            rotate = scale = 0;
            r = g = b = a = 1.0F;
        }
    }

    private State original, now;
    private LinkedList<State> destinations;

    private int step;

    public Animator(Texture tex) {
        this.tex = tex;

        now = new State(null);
        original = new State(null);
        destinations = new LinkedList<>();
        step = 0;
    }

    public Animator start(Vector2 pos) {
        return start(pos, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public Animator start(Vector2 pos, float rotate, float scale) {
        return start(pos, rotate, scale, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public Animator start(Vector2 pos, float rotate, float scale, float r, float g, float b, float a) {
        original.pos = pos; original.rotate = rotate; original.scale = scale;
        original.r = r; original.g = g; original.b = b; original.a = a; original.length = 0;

        return this;
    }

    public Animator next(Interpolation inter, int length, Vector2 pos, float rotate, float scale) {
        return next(inter, length, pos, rotate, scale, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public Animator next(Interpolation inter, int length, Vector2 pos, float rotate, float scale, float r, float g, float b, float a) {
        State dst = new State(inter);
        dst.pos = pos; dst.rotate = rotate; dst.scale = scale;
        dst.r = r; dst.g = g; dst.b = b; dst.a = a; dst.length = length;
        this.destinations.addLast(dst);

        return this;
    }

    private State interpolation(State original, State destination, int step) {
        State result = new State(null);
        result.pos = interpolation(destination.inter, original.pos, destination.pos, step, destination.length);
        result.rotate = interpolation(destination.inter, original.rotate, destination.rotate, step, destination.length);
        result.scale = interpolation(destination.inter, original.scale, destination.scale, step, destination.length);

        result.r = interpolation(destination.inter, original.r, destination.r, step, destination.length);
        result.g = interpolation(destination.inter, original.g, destination.g, step, destination.length);
        result.b = interpolation(destination.inter, original.b, destination.b, step, destination.length);
        result.a = interpolation(destination.inter, original.a, destination.a, step, destination.length);

        return result;
    }

    private Vector2 interpolation(Interpolation inter, Vector2 original, Vector2 destination, int step, int length) {
        Vector2 result = Vector2.Zero;
        result.x = interpolation(inter, original.x, destination.x, step, length);
        result.y = interpolation(inter, original.y, destination.y, step, length);

        return result;
    }

    private float interpolation(Interpolation inter, float original, float destination, int step, int length) {
        float x = ((float) step - (float) length) / (float) length;
        float k = destination - original;
        switch (inter) {
            case LINEAR: // y = x, x = 0 to 1
                return original + k * x;
            case QUADRATIC: // y = -x^2 + 2x, x = 0 to 1
                return original + k * (-x * x + 2 * x);
            case SINE: // y = 0.5sin((x - 0.5)pi) + 0.5
                return original + k * (float) (0.5 * Math.sin((x - 0.5) * Math.PI) + 0.5);
            default:
                return original + k * x;
        }
    }

    public Result onUpdate(int t) {
        if (destinations.isEmpty()) return Result.END;

        State next = destinations.peekFirst();
        now = interpolation(original, next, step);

        step += 1;
        if (step > next.length) {
            step = 0;
            original = destinations.getFirst();
        }

        return Result.DONE;
    }

    public Result onRender(Renderer renderer) {
        renderer.draw(tex, now.pos.x, now.pos.y, now.rotate, now.scale, now.r, now.g, now.b, now.a);
        return Result.DONE;
    }

}
