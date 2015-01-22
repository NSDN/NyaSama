package com.nyasama.libgdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

;

/**
 * Created by D.zzm on 2014.12.14.
 */
public class AboutCore extends ApplicationAdapter {

    DrawCore DrawCoreObj;
    Texture TexBullet, TexBack[] = new Texture[2];
    Texture Scene[] = new Texture[10];
    BulletObject[][] Bullets = new BulletObject[101][1001];
    public float DeviceWidth, DeviceHeight;
    int CtrlValueA = 10;
    int CtrlValueB = 50;
    double PI = 3.141592653;
    int Time, Angle;
    float BackA, BackB;
    boolean Flag = false;
    Music Music_obj;

    @Override
    public void create() {
        LoadTexture();
        LoadBullet();
        DrawCoreObj = new DrawCore();
        DeviceWidth = Gdx.graphics.getWidth();
        DeviceHeight = Gdx.graphics.getHeight();
        Time = 0;
        Angle = 0;
        BackA = 0;
        BackB = 0;
        Music_obj = Gdx.audio.newMusic(Gdx.files.internal("BGM.ogg"));
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        DrawCoreObj.DrawBegin();
        DrawBack();
        DrawCoreObj.DrawEnd();

        DrawCoreObj.DrawBegin();
        MainShow();
        DrawCoreObj.DrawEnd();

        JudgeBullet();

        Time++;

        if (Angle >= 360) Angle = 0;
        if (BackA <= -256) BackA = 0;
        if (BackB >= 256) BackB = 0;
        Angle++;
        BackA = BackA - 0.5f;
        BackB = BackB + 0.5f;
    }

    private void LoadTexture() {
        TexBullet = new Texture("Bullet.png");
        TexBack[0] = new Texture("Back01.png");
        TexBack[1] = new Texture("Back02.png");
        Scene[0] = new Texture("Nyasama.png");
        for (int i = 1; i <= 5; i++) {
            Scene[i] = new Texture(Integer.toString(i) + ".png");

        }
    }

    private void MainShow() {
        if (!Flag) {
            if (Time < 5) {
                PreGenEffect();
            }
            GenEffect();
            DrawEffect();

            if (Time < 100) {
                if (Time == 0) {
                    Music_obj.play();
                    Music_obj.setLooping(true);
                    Music_obj.setVolume(0f);
                }
                Music_obj.setVolume(Time / 100f);
            } else {
                Music_obj.setVolume(1f);
                if (Time >= 100 & Time < 200) {
                    DrawCoreObj.DrawPic(Scene[1], DeviceWidth / 2, DeviceHeight / 2, (Time - 100) / 100f);
                } else {
                    if (Time >= 200 & Time < 300) {
                        DrawCoreObj.DrawPic(Scene[1], DeviceWidth / 2, DeviceHeight / 2, 1f);
                    } else {
                        if (Time >= 300 & Time < 400) {
                            DrawCoreObj.DrawPic(Scene[1], DeviceWidth / 2, DeviceHeight / 2, (400 - Time) / 100f);
                        } else {
                            if (Time >= 400 & Time < 500) {
                                DrawCoreObj.DrawPic(Scene[2], DeviceWidth / 2, DeviceHeight / 2, (Time - 400) / 100f);
                            } else {
                                if (Time >= 500 & Time < 600) {
                                    DrawCoreObj.DrawPic(Scene[2], DeviceWidth / 2, DeviceHeight / 2, 1f);
                                } else {
                                    if (Time >= 600 & Time < 700) {
                                        DrawCoreObj.DrawPic(Scene[2], DeviceWidth / 2, DeviceHeight / 2, (700 - Time) / 100f);
                                    } else {
                                        if (Time >= 700 & Time < 800) {
                                            DrawCoreObj.DrawPic(Scene[3], DeviceWidth / 2, DeviceHeight / 2, (Time - 700) / 100f);
                                        } else {
                                            if (Time >= 800 & Time < 900) {
                                                DrawCoreObj.DrawPic(Scene[3], DeviceWidth / 2, DeviceHeight / 2, 1f);
                                            } else {
                                                if (Time >= 900 & Time < 1000) {
                                                    DrawCoreObj.DrawPic(Scene[3], DeviceWidth / 2, DeviceHeight / 2, (1000 - Time) / 100f);
                                                } else {
                                                    if (Time >= 1000 & Time < 1100) {
                                                        DrawCoreObj.DrawPic(Scene[4], DeviceWidth / 2, DeviceHeight / 2, (Time - 1000) / 100f);
                                                    } else {
                                                        if (Time >= 1100 & Time < 1200) {
                                                            DrawCoreObj.DrawPic(Scene[4], DeviceWidth / 2, DeviceHeight / 2, 1f);
                                                        } else {
                                                            if (Time >= 1200 & Time < 1300) {
                                                                DrawCoreObj.DrawPic(Scene[4], DeviceWidth / 2, DeviceHeight / 2, (1300 - Time) / 100f);
                                                            } else {
                                                                if (Time >= 1300 & Time < 1400) {
                                                                    DrawCoreObj.DrawPic(Scene[5], DeviceWidth / 2, DeviceHeight / 2, (Time - 1300) / 100f);
                                                                } else {
                                                                    if (Time >= 1400 & Time < 1500) {
                                                                        DrawCoreObj.DrawPic(Scene[5], DeviceWidth / 2, DeviceHeight / 2, 1f);
                                                                    } else {
                                                                        if (Time >= 1500 & Time < 1600) {
                                                                            DrawCoreObj.DrawPic(Scene[5], DeviceWidth / 2, DeviceHeight / 2, (1600 - Time) / 100f);
                                                                        } else {
                                                                            if (Time >= 1600 & Time < 1700) {
                                                                                DrawCoreObj.DrawPic(Scene[0], DeviceWidth / 2, DeviceHeight / 2, (Time - 1600) / 100f);
                                                                            } else {
                                                                                if (Time >= 1700 & Time < 1800) {
                                                                                    DrawCoreObj.DrawPic(Scene[0], DeviceWidth / 2, DeviceHeight / 2, 1f);
                                                                                } else {
                                                                                    if (Time >= 1800 & Time < 1900) {
                                                                                        DrawCoreObj.DrawPic(Scene[0], DeviceWidth / 2, DeviceHeight / 2, (1900 - Time) / 100f);
                                                                                    } else {
                                                                                        if (Time >= 1900) {
                                                                                            Time = 0;
                                                                                            Flag = true;
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (Time < 500) ClearEffect();
            else {
                if (Time == 500) LoadBullet();
                GenBullet();
                DrawBullet();
            }
        }
    }

    private void DrawBack() {
        /*for (int i = 0; i <= 10; i++) {
         *   for (int j = 0; j <= 10; j++) {
         *       DrawCoreObj.DrawPic(TexBack[0], BackA + 128f + 256f * i, BackA + 128f + 256f * j, 0);
         *   }
        }*/

        for (int i = -1; i <= 10; i++) {
            for (int j = -1; j <= 10; j++) {
                DrawCoreObj.DrawPic(TexBack[1], BackB + 128f + 256f * i, BackB + 128f + 256f * j, 0);
            }
        }
    }

    private void LoadBullet() {
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                Bullets[i][j] = new BulletObject();
            }
        }
    }

    private void PreGenEffect() {
        Double DoubleTmp;
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                if (!Bullets[i][j].IsEnabled) {
                    DoubleTmp = Math.random() * DeviceWidth;
                    Bullets[i][j].x = DoubleTmp.floatValue();
                    DoubleTmp = Math.random() * DeviceHeight;
                    Bullets[i][j].y = DoubleTmp.floatValue();
                    //DoubleTmp = Math.random();
                    //Bullets[i][j].r = DoubleTmp.floatValue();
                    //DoubleTmp = Math.random();
                    //Bullets[i][j].g = DoubleTmp.floatValue();
                    //DoubleTmp = Math.random();
                    //Bullets[i][j].b = DoubleTmp.floatValue();
                    //DoubleTmp = Math.random();
                    //Bullets[i][j].a = 0.6f * DoubleTmp.floatValue();
                    //DoubleTmp = 360d * Math.random();
                    //Bullets[i][j].Rotate = DoubleTmp.intValue();
                    //DoubleTmp = Math.random();
                    //Bullets[i][j].ScaleXY = 1.5f * DoubleTmp.floatValue();
                    //if (Math.random() > 0.5d) Bullets[i][j].Direction = true;
                    //else Bullets[i][j].Direction = false;
                    Bullets[i][j].IsEnabled = true;
                }
                Bullets[i][j].dx = (Bullets[i][j].x - (DeviceWidth / 2)) / 50;
                Bullets[i][j].dy = (Bullets[i][j].y - (DeviceHeight * 1.25f)) / 50;
                Bullets[i][j].x = Bullets[i][j].x + Bullets[i][j].dx / 2;
                Bullets[i][j].y = Bullets[i][j].y + Bullets[i][j].dy / 2;
            }
        }
    }

    private void GenBullet() {
        Double DoubleTmp;
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                if (!Bullets[i][j].IsEnabled) {
                    DoubleTmp = 5 * Math.cos(j) * Math.cos(0.2 * PI * i + j);
                    Bullets[i][j].x = DoubleTmp.floatValue() + (DeviceWidth / 2);
                    DoubleTmp = 5 * Math.cos(j) * Math.sin(0.2 * PI * i + j);
                    Bullets[i][j].y = DoubleTmp.floatValue() + (DeviceHeight / 2);

                    DoubleTmp = Math.random();
                    Bullets[i][j].r = DoubleTmp.floatValue();
                    DoubleTmp = Math.random();
                    Bullets[i][j].g = DoubleTmp.floatValue();
                    DoubleTmp = Math.random();
                    Bullets[i][j].b = DoubleTmp.floatValue();
                    DoubleTmp = Math.random();
                    Bullets[i][j].a = 0.6f * DoubleTmp.floatValue();
                    DoubleTmp = 360d * Math.random();
                    Bullets[i][j].Rotate = DoubleTmp.intValue();
                    DoubleTmp = Math.random();
                    Bullets[i][j].ScaleXY = 1.5f * DoubleTmp.floatValue();

                    if (Math.random() > 0.5d) Bullets[i][j].Direction = true;
                    else Bullets[i][j].Direction = false;

                    Bullets[i][j].IsEnabled = true;
                }
                Bullets[i][j].dx = (Bullets[i][j].x - (DeviceWidth / 2)) / 40;
                Bullets[i][j].dy = (Bullets[i][j].y - (DeviceHeight / 2)) / 40;
                Bullets[i][j].x = Bullets[i][j].x + Bullets[i][j].dx / 2;
                Bullets[i][j].y = Bullets[i][j].y + Bullets[i][j].dy / 2;
            }
        }
    }

    private void GenEffect() {
        Double DoubleTmp;
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                if ((!Bullets[i][j].IsEnabled) && (Time % 80 == 0)) {
                    Bullets[i][j].x = DeviceWidth / 2;
                    Bullets[i][j].y = DeviceHeight + 50f;
                    DoubleTmp = Math.random();
                    Bullets[i][j].dx = DoubleTmp.floatValue() - 0.5f;
                    DoubleTmp = Math.random();
                    Bullets[i][j].dy = -DoubleTmp.floatValue();

                    DoubleTmp = Math.random();
                    Bullets[i][j].r = DoubleTmp.floatValue();
                    DoubleTmp = Math.random();
                    Bullets[i][j].g = DoubleTmp.floatValue();
                    DoubleTmp = Math.random();
                    Bullets[i][j].b = DoubleTmp.floatValue();
                    DoubleTmp = Math.random();
                    Bullets[i][j].a = 0.6f * DoubleTmp.floatValue();
                    DoubleTmp = 360d * Math.random();
                    Bullets[i][j].Rotate = DoubleTmp.intValue();
                    DoubleTmp = Math.random();
                    Bullets[i][j].ScaleXY = 1.5f * DoubleTmp.floatValue();
                    if (Math.random() > 0.5d) Bullets[i][j].Direction = true;
                    else Bullets[i][j].Direction = false;
                    Bullets[i][j].IsEnabled = true;
                }
                Bullets[i][j].x = Bullets[i][j].x + Bullets[i][j].dx * 50;
                Bullets[i][j].y = Bullets[i][j].y + Bullets[i][j].dy * 50;
            }
        }
    }

    private void ClearEffect() {
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                Bullets[i][j].x = Bullets[i][j].x + Bullets[i][j].dx * 50;
                Bullets[i][j].y = Bullets[i][j].y + Bullets[i][j].dy * 50;
            }
        }
    }

    private void DrawEffect() {
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                if (Bullets[i][j].IsEnabled)
                    if (Bullets[i][j].Direction) {
                        DrawCoreObj.DrawPic(TexBullet, Bullets[i][j].x, Bullets[i][j].y, Bullets[i][j].Rotate + Angle, Bullets[i][j].ScaleXY, Bullets[i][j].ScaleXY, Bullets[i][j].r, Bullets[i][j].g, Bullets[i][j].b, Bullets[i][j].a);
                    } else {
                        DrawCoreObj.DrawPic(TexBullet, Bullets[i][j].x, Bullets[i][j].y, Bullets[i][j].Rotate - Angle, Bullets[i][j].ScaleXY, Bullets[i][j].ScaleXY, Bullets[i][j].r, Bullets[i][j].g, Bullets[i][j].b, Bullets[i][j].a);
                    }
            }
        }
    }

    private void DrawBullet() {
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                if (Bullets[i][j].IsEnabled)
                    if (Bullets[i][j].Direction) {
                        //DrawCoreObj.DrawPic(TexBullet, Bullets[i][j].x, Bullets[i][j].y, 0);
                        if (Bullets[i][j].Direction) {
                            DrawCoreObj.DrawPic(TexBullet, Bullets[i][j].x, Bullets[i][j].y, Bullets[i][j].Rotate + Angle, Bullets[i][j].ScaleXY, Bullets[i][j].ScaleXY, Bullets[i][j].r, Bullets[i][j].g, Bullets[i][j].b, Bullets[i][j].a);
                        } else {
                            DrawCoreObj.DrawPic(TexBullet, Bullets[i][j].x, Bullets[i][j].y, Bullets[i][j].Rotate - Angle, Bullets[i][j].ScaleXY, Bullets[i][j].ScaleXY, Bullets[i][j].r, Bullets[i][j].g, Bullets[i][j].b, Bullets[i][j].a);
                        }
                    }
            }
        }
    }

    private void JudgeBullet() {
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                if (Bullets[i][j].x < -300 || Bullets[i][j].x > DeviceWidth + 300 || Bullets[i][j].y < -100 || Bullets[i][j].y > DeviceHeight + 100)
                    Bullets[i][j].Init();
            }
        }
    }
}

class DrawCore {
    private SpriteBatch Batch;

    public void DrawBegin() {
        Batch.begin();
    }

    public void DrawEnd() {
        Batch.end();
    }

    public void DrawBack(Texture Tex) {
        Batch.draw(Tex, 0, 0, Tex.getWidth(), Tex.getHeight());
    }

    public void DrawPic(Texture Tex, float x, float y, int Rotate) {
        int Width = Tex.getWidth(), Height = Tex.getHeight();
        Batch.draw(Tex, x - Width / 2, y - Height / 2, Width / 2, Height / 2, Width, Height, 1, 1, Rotate, 0, 0, Width, Height, false, false);
    }

    public void DrawPic(Texture Tex, float x, float y, float alpha) {
        int Width = Tex.getWidth(), Height = Tex.getHeight();
        Batch.setColor(1, 1, 1, alpha);
        Batch.draw(Tex, x - Width / 2, y - Height / 2, Width / 2, Height / 2, Width, Height, 1, 1, 0, 0, 0, Width, Height, false, false);
        Batch.setColor(1, 1, 1, 1);
    }

    public void DrawPic(Texture Tex, float x, float y, float Rotate, float ScX, float ScY) {
        int Width = Tex.getWidth(), Height = Tex.getHeight();
        Batch.draw(Tex, x - Width / 2 * ScX, y - Height / 2 * ScY, Width / 2, Height / 2, Width, Height, ScX, ScY, Rotate, 0, 0, Width, Height, false, false);
    }

    public void DrawPic(Texture Tex, float x, float y, float Rotate, float ScX, float ScY, float r, float g, float b, float a) {
        int Width = Tex.getWidth(), Height = Tex.getHeight();
        Batch.setColor(r, g, b, a);
        Batch.draw(Tex, x - Width / 2 * ScX, y - Height / 2 * ScY, Width / 2, Height / 2, Width, Height, ScX, ScY, Rotate, 0, 0, Width, Height, false, false);
        Batch.setColor(1, 1, 1, 1);
    }

    public DrawCore() {
        Batch = new SpriteBatch();
    }
}

class BulletObject {
    public float x;
    public float y;
    public float dx;
    public float dy;
    public int Rotate;
    public boolean Direction;
    public boolean IsEnabled;
    public Texture Tex;
    public float ScaleXY;
    public float r;
    public float g;
    public float b;
    public float a;

    public BulletObject() {
        x = 0;
        y = 0;
        dx = 0;
        dy = 0;
        Rotate = 0;
        Direction = true;
        IsEnabled = false;
        Tex = null;
        ScaleXY = 0;
        r = 0;
        g = 0;
        b = 0;
        a = 0;
    }

    public void Init() {
        x = 0;
        y = 0;
        dx = 0;
        dy = 0;
        Rotate = 0;
        Direction = true;
        IsEnabled = false;
        Tex = null;
        ScaleXY = 0;
        r = 0;
        g = 0;
        b = 0;
        a = 0;
    }
}

