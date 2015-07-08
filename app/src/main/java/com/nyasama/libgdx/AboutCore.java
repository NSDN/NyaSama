package com.nyasama.libgdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;

import com.nyasama.libgdx.utility.BulletObject;
import com.nyasama.libgdx.utility.DrawCore;
import com.nyasama.libgdx.utility.Utility;


/**
 * Created by D.zzm on 2014.12.14.
 */

public class AboutCore extends ApplicationAdapter {

    DrawCore DrawCoreObj;
    Texture TexBullet, TexPlayer, TexBack[] = new Texture[2];
    Texture Scene[] = new Texture[10];
    BulletObject[][] Bullets = new BulletObject[51][51];
    public float DeviceWidth, DeviceHeight;
    int CtrlValueA, CtrlValueB;
    double PI = Math.PI;
    int Time, Angle, Miss, Graze;
    long Score, ScoreBase;
    float BackA, BackB, BackAlpha, PlayerX, PlayerY;
    boolean Flag = false;
    Music Music_obj;
    Sound BIU, GRA;

    @Override
    public void create() {
        LoadTexture();
        LoadBullet();
        DrawCoreObj = new DrawCore();
        DeviceWidth = Gdx.graphics.getWidth();
        DeviceHeight = Gdx.graphics.getHeight();
        Time = 0; //DEBUG POS, DEFAULT IS ZERO//
        Angle = 0;
        Miss = 0;
        Graze = 0;
        Score = 0;
        ScoreBase = 0;
        BackA = 0;
        BackB = 0;
        BackAlpha = 0;
        PlayerX = DeviceWidth / 2;
        PlayerY = DeviceHeight / 10;
        Music_obj = Gdx.audio.newMusic(Gdx.files.internal("BGM.ogg"));
        BIU = Gdx.audio.newSound(Gdx.files.internal("BIU.ogg"));
        GRA = Gdx.audio.newSound(Gdx.files.internal("Graze.ogg"));
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

        /*DrawCoreObj.DrawBegin();
        DrawCoreObj.DrawString(50f, 50f, 1f, Color.WHITE, "Time: " + Integer.toString(Time));
        DrawCoreObj.DrawEnd();*/

        JudgeBorder();

        Time++;
        if (Time > 65533) Time = 500;

        Score = ScoreBase + Graze * 5 - Miss * 20;

        if (Angle >= 360) Angle = 0;
        if (BackA <= -256) BackA = 0;
        if (BackB >= 256) BackB = 0;
        Angle++;
        BackA = BackA - 0.5f;
        BackB = BackB + 0.5f;

    }

    private void LoadTexture() {
        TexBullet = new Texture("Bullet.png");
        TexPlayer = new Texture("Player.png");
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
                BackAlpha = Time / 100f;
            } else {
                Music_obj.setVolume(1f);
                BackAlpha = 1.0f;
                if (Time < 200) DrawCoreObj.DrawPic(Scene[1], DeviceWidth / 2, DeviceHeight / 2, (Time - 100) / 100f);
                else if (Time < 300) DrawCoreObj.DrawPic(Scene[1], DeviceWidth / 2, DeviceHeight / 2, 1f);
                else if (Time < 400) DrawCoreObj.DrawPic(Scene[1], DeviceWidth / 2, DeviceHeight / 2, (400 - Time) / 100f);

                else if (Time < 500) DrawCoreObj.DrawPic(Scene[2], DeviceWidth / 2, DeviceHeight / 2, (Time - 400) / 100f);
                else if (Time < 600) DrawCoreObj.DrawPic(Scene[2], DeviceWidth / 2, DeviceHeight / 2, 1f);
                else if (Time < 700) DrawCoreObj.DrawPic(Scene[2], DeviceWidth / 2, DeviceHeight / 2, (700 - Time) / 100f);

                else if (Time < 800) DrawCoreObj.DrawPic(Scene[3], DeviceWidth / 2, DeviceHeight / 2, (Time - 700) / 100f);
                else if (Time < 900) DrawCoreObj.DrawPic(Scene[3], DeviceWidth / 2, DeviceHeight / 2, 1f);
                else if (Time < 1000) DrawCoreObj.DrawPic(Scene[3], DeviceWidth / 2, DeviceHeight / 2, (1000 - Time) / 100f);

                else if (Time < 1100) DrawCoreObj.DrawPic(Scene[4], DeviceWidth / 2, DeviceHeight / 2, (Time - 1000) / 100f);
                else if (Time < 1200) DrawCoreObj.DrawPic(Scene[4], DeviceWidth / 2, DeviceHeight / 2, 1f);
                else if (Time < 1300) DrawCoreObj.DrawPic(Scene[4], DeviceWidth / 2, DeviceHeight / 2, (1300 - Time) / 100f);

                else if (Time < 1400) DrawCoreObj.DrawPic(Scene[5], DeviceWidth / 2, DeviceHeight / 2, (Time - 1300) / 100f);
                else if (Time < 1500) DrawCoreObj.DrawPic(Scene[5], DeviceWidth / 2, DeviceHeight / 2, 1f);
                else if (Time < 1600) DrawCoreObj.DrawPic(Scene[5], DeviceWidth / 2, DeviceHeight / 2, (1600 - Time) / 100f);

                else if (Time < 1700) DrawCoreObj.DrawPic(Scene[0], DeviceWidth / 2, DeviceHeight / 2, (Time - 1600) / 100f);
                else if (Time < 1800) DrawCoreObj.DrawPic(Scene[0], DeviceWidth / 2, DeviceHeight / 2, 1f);
                else if (Time < 1900) {
                    DrawCoreObj.DrawPic(Scene[0], DeviceWidth / 2, DeviceHeight / 2, (1900 - Time) / 100f);
                    BackAlpha = (1900 - Time) / 100f;
                    Music_obj.setVolume(BackAlpha);
                } else if (Time >= 1900) {
                    Time = 0;
                    BackAlpha = 0f;
                    Music_obj.stop();
                    Flag = true;
                }
            }
        } else {
            if (Time < 500) {
                if (Time >= 200 & Time < 300) BackAlpha = (Time - 200) / 100f;
                if (Time == 300) BackAlpha = 1.0f;
                ClearEffect();
                DrawEffect();
            } else {
                if (Time == 500) LoadBullet();
                DrawBullet();
                Control();
                GenBullet();
                JudgeBullet();

                DrawCoreObj.DrawPic(TexPlayer, PlayerX, PlayerY, 0);
                if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT))
                    DrawCoreObj.DrawPic(TexBullet, PlayerX, PlayerY, Angle);

                DrawCoreObj.DrawString(50f, DeviceHeight - 50f, 1.8f, Color.WHITE, "Graze: " + Integer.toString(Graze));
                DrawCoreObj.DrawString(50f, DeviceHeight - 70f, 1.8f, Color.WHITE, "Miss:  " + Integer.toString(Miss));
                DrawCoreObj.DrawString(50f, DeviceHeight - 90f, 1.8f, Color.WHITE, "Score: " + Score);

                if (Time % 10 == 0)
                    ScoreBase++;
            }
        }
    }

    private void DrawBack() {
        if (Flag) {
            for (int i = 0; i <= 10; i++) {
                for (int j = 0; j <= 10; j++) {
                    DrawCoreObj.DrawPic(TexBack[0], BackA + 128f + 256f * i, BackA + 128f + 256f * j, BackAlpha);
                }
            }
        } else {
            for (int i = -1; i <= 10; i++) {
                for (int j = -1; j <= 10; j++) {
                    DrawCoreObj.DrawPic(TexBack[1], BackB + 128f + 256f * i, BackB + 128f + 256f * j, BackAlpha);
                }
            }
        }
    }

    private void LoadBullet() {
        CtrlValueA = 50;
        CtrlValueB = 50;
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
                    Bullets[i][j].IsEnabled = true;
                }
                Bullets[i][j].dx = (Bullets[i][j].x - (DeviceWidth / 2)) / 50;
                Bullets[i][j].dy = (Bullets[i][j].y - (DeviceHeight * 1.25f)) / 50;
                Bullets[i][j].x = Bullets[i][j].x + Bullets[i][j].dx / 2;
                Bullets[i][j].y = Bullets[i][j].y + Bullets[i][j].dy / 2;
            }
        }
    }

    private void GenEffect() {
        CtrlValueA = 20;
        CtrlValueB = 20;
        Double DoubleTmp;
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                if ((!Bullets[i][j].IsEnabled) && (Time % 82 == 0)) {
                    DoubleTmp = Time / 100 * Math.cos(Time / 100) + DeviceWidth / 2;
                    Bullets[i][j].x = DoubleTmp.floatValue();
                    DoubleTmp = Time / 100 * Math.sin(Time / 100) + DeviceHeight + 50f;
                    Bullets[i][j].y = DoubleTmp.floatValue();

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
                    Bullets[i][j].ScaleXY = 0.5f + DoubleTmp.floatValue();
                    Bullets[i][j].Direction = Math.random() > 0.5d;
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

    private void GenBullet() {
        Double DoubleTmp;
        if (Time < 1500) {
            CtrlValueA = 20;
            CtrlValueB = 40;
            for (int i = 0; i <= CtrlValueA; i++) {
                for (int j = 0; j <= CtrlValueB; j++) {
                    if ((!Bullets[i][j].IsEnabled) && (Time % 2 == 0)) {
                        DoubleTmp = Math.cos(i) + i * Math.cos(0.15d * j);
                        Bullets[i][j].x = DoubleTmp.floatValue() + (DeviceWidth / 2);
                        DoubleTmp = Math.sin(i) - i * Math.sin(0.15d * j);
                        Bullets[i][j].y = DoubleTmp.floatValue() + (DeviceHeight * 0.75f);

                        DoubleTmp = Math.random();
                        Bullets[i][j].r = 0.2f + 0.8f * DoubleTmp.floatValue();
                        DoubleTmp = Math.random();
                        Bullets[i][j].g = 0.2f + 0.8f * DoubleTmp.floatValue();
                        DoubleTmp = Math.random();
                        Bullets[i][j].b = 0.2f + 0.8f * DoubleTmp.floatValue();
                        DoubleTmp = Math.random();
                        //Bullets[i][j].a = 0.6f * DoubleTmp.floatValue();
                        Bullets[i][j].a = 1.0f;
                        DoubleTmp = 360d * Math.random();
                        Bullets[i][j].Rotate = DoubleTmp.intValue();
                        DoubleTmp = Math.random();
                        //Bullets[i][j].ScaleXY = 0.5f + DoubleTmp.floatValue();
                        Bullets[i][j].ScaleXY = 1.0f;

                        Bullets[i][j].Direction = Math.random() > 0.5d;

                        Bullets[i][j].IsEnabled = true;
                    }
                    Bullets[i][j].dx = (Bullets[i][j].x - (DeviceWidth / 2)) / 40;
                    Bullets[i][j].dy = (Bullets[i][j].y - (DeviceHeight * 0.75f)) / 40;
                    Bullets[i][j].x = Bullets[i][j].x + Bullets[i][j].dx / 2;
                    Bullets[i][j].y = Bullets[i][j].y + Bullets[i][j].dy / 2;
                }
            }
        } else if (Time < 2000) {
            for (int i = 0; i <= CtrlValueA; i++) {
                for (int j = 0; j <= CtrlValueB; j++) {
                    Bullets[i][j].dx = (Bullets[i][j].x - (DeviceWidth / 2)) / 40;
                    Bullets[i][j].dy = (Bullets[i][j].y - (DeviceHeight * 0.75f)) / 40;
                    Bullets[i][j].x = Bullets[i][j].x + Bullets[i][j].dx / 2;
                    Bullets[i][j].y = Bullets[i][j].y + Bullets[i][j].dy / 2;
                }
            }
        } else {
            CtrlValueA = 20;
            CtrlValueB = 40;
            for (int i = 0; i <= CtrlValueA; i++) {
                for (int j = 0; j <= CtrlValueB; j++) {
                    if ((!Bullets[i][j].IsEnabled) && (Time % 10 == 0)) {
                        DoubleTmp = Math.cos(i) + j * Math.cos(0.15d * j);
                        Bullets[i][j].x = DoubleTmp.floatValue() + (DeviceWidth / 2);
                        DoubleTmp = Math.sin(i) - j * Math.sin(0.15d * j);
                        Bullets[i][j].y = DoubleTmp.floatValue() + (DeviceHeight * 0.75f);

                        DoubleTmp = Math.random();
                        Bullets[i][j].r = 0.2f + 0.8f * DoubleTmp.floatValue();
                        DoubleTmp = Math.random();
                        Bullets[i][j].g = 0.2f + 0.8f * DoubleTmp.floatValue();
                        DoubleTmp = Math.random();
                        Bullets[i][j].b = 0.2f + 0.8f * DoubleTmp.floatValue();
                        DoubleTmp = Math.random();
                        //Bullets[i][j].a = 0.6f * DoubleTmp.floatValue();
                        Bullets[i][j].a = 1.0f;
                        DoubleTmp = 360d * Math.random();
                        Bullets[i][j].Rotate = DoubleTmp.intValue();
                        DoubleTmp = Math.random();
                        //Bullets[i][j].ScaleXY = 0.5f + DoubleTmp.floatValue();
                        Bullets[i][j].ScaleXY = 1.0f;

                        Bullets[i][j].Direction = Math.random() > 0.5d;

                        Bullets[i][j].IsEnabled = true;
                    }
                    Bullets[i][j].dx = (Bullets[i][j].x - (DeviceWidth / 2)) / 20;
                    Bullets[i][j].dy = (Bullets[i][j].y - (DeviceHeight * 0.75f)) / 20;
                    Bullets[i][j].x = Bullets[i][j].x + Bullets[i][j].dx / 2;
                    Bullets[i][j].y = Bullets[i][j].y + Bullets[i][j].dy / 2;
                }
            }
        }


    }

    private void DrawBullet() {
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                if (Bullets[i][j].IsEnabled) {
                    if (Bullets[i][j].Direction) {
                        DrawCoreObj.DrawPic(TexBullet, Bullets[i][j].x, Bullets[i][j].y, Bullets[i][j].Rotate + Angle, Bullets[i][j].ScaleXY, Bullets[i][j].ScaleXY, Bullets[i][j].r, Bullets[i][j].g, Bullets[i][j].b, Bullets[i][j].a);
                    } else {
                        DrawCoreObj.DrawPic(TexBullet, Bullets[i][j].x, Bullets[i][j].y, Bullets[i][j].Rotate - Angle, Bullets[i][j].ScaleXY, Bullets[i][j].ScaleXY, Bullets[i][j].r, Bullets[i][j].g, Bullets[i][j].b, Bullets[i][j].a);
                    }
                }
            }
        }
    }

    public void JudgeBullet() {
        final float Min = 8.0f;
        final float Max = 22.0f;
        for (int i = 0; i <= CtrlValueA; i++)
            for (int j = 0; j <= CtrlValueB; j++) {
                if (Bullets[i][j].IsEnabled && Bullets[i][j].PreJudge(PlayerX, PlayerY, 500)) {
                    if (Bullets[i][j].Judge(PlayerX, PlayerY, Min, Max) == BulletObject.JUDGE_AWAY && !Bullets[i][j].IsGrazed) {
                        Bullets[i][j].IsGrazed = true;
                    }
                    if (Bullets[i][j].Judge(PlayerX, PlayerY, Min, Max) == BulletObject.JUDGE_GRAZE && Bullets[i][j].IsGrazed) {
                        Bullets[i][j].IsGrazed = false;
                        Graze++;
                        GRA.play();
                    }
                    if (Bullets[i][j].Judge(PlayerX, PlayerY, Min, Max) == BulletObject.JUDGE_MISS) {
                        Bullets[i][j].IsEnabled = false;
                        Miss++;
                        BIU.play();
                        PlayerX = DeviceWidth / 2;
                        PlayerY = DeviceHeight / 10;
                    }
                }
            }
    }

    public void Control() {
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) PlayerY = PlayerY + 2;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) PlayerY = PlayerY - 2;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) PlayerX = PlayerX - 2;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) PlayerX = PlayerX + 2;
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) PlayerY = PlayerY + 5;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) PlayerY = PlayerY - 5;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) PlayerX = PlayerX - 5;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) PlayerX = PlayerX + 5;
        }

        if (Gdx.input.isTouched()) {
            PlayerX = PlayerX + Gdx.input.getDeltaX();
            PlayerY = PlayerY - Gdx.input.getDeltaY();
        }
    }

    private void JudgeBorder() {
        for (int i = 0; i <= CtrlValueA; i++) {
            for (int j = 0; j <= CtrlValueB; j++) {
                if (Bullets[i][j].IsEnabled) {
                    if (!Flag) {
                        if (Bullets[i][j].x < -300 || Bullets[i][j].x > DeviceWidth + 300 || Bullets[i][j].y < -100 || Bullets[i][j].y > DeviceHeight + 100)
                            Bullets[i][j].Init();
                    } else {
                        if (Utility.Distance(Bullets[i][j].x, Bullets[i][j].y, DeviceWidth / 2, DeviceHeight / 2) > Math.max(DeviceWidth, DeviceHeight))
                            Bullets[i][j].Init();
                    }
                }
            }
        }

        if (PlayerX > DeviceWidth) PlayerX = DeviceWidth;
        if (PlayerY > DeviceHeight) PlayerY = DeviceHeight;
        if (PlayerX < 0) PlayerX = 0;
        if (PlayerY < 0) PlayerY = 0;
    }
}

