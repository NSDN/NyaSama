package cn.ac.nya.nsgdx.utility;

/**
 * Created by D.zzm on 2018.7.26.
 */
public class RenderUtil {

    public static class Color4 {

        public float r, g, b, a;

        public static Color4 get(float r, float g, float b, float a) {
            Color4 color = new Color4();
            color.r = r;
            color.g = g;
            color.b = b;
            color.a = a;
            return color;
        }

        public Color4(Utility.Color3 color) {
            r = color.r;
            g = color.g;
            b = color.b;
            a = 1.0F;
        }

        public Color4(Color4 color) {
            r = color.r;
            g = color.g;
            b = color.b;
            a = color.a;
        }

        private Color4() {  }

    }

    public static Color4 color4(float r, float g, float b, float a) {
        return Color4.get(r, g, b, a);
    }

    public interface IDrawable {  }

    public interface IRenderer {
        void begin();
        void end();
        void drawString(float x, float y, float scale, Color4 color, String str);
        void draw(IDrawable drawable, float x, float y, float rotate, float scale, Color4 color);
    }

}
