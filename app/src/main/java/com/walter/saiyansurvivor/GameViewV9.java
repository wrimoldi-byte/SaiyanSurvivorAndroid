package com.walter.saiyansurvivor;

import android.content.Context;
import android.graphics.*;

/** v0.6.1: clearer ground surfaces, stronger enemy contrast and animated walking/running. */
public class GameViewV9 extends GameViewV8 {
    private final Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameViewV9(Context c) {
        super(c);
        banner = "GROUND + WALK ANIMATION";
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    void bg(Canvas c) {
        int biome = ((int)(Math.hypot(worldX, worldY) / 2200.0)) % 3;
        if (biome == 0) drawStonePlain(c);
        else if (biome == 1) drawDesertFloor(c);
        else drawRuinedFloor(c);
    }

    private void drawSky(Canvas c, int a, int b) {
        q.setShader(new LinearGradient(0,0,0,getHeight()*.42f,
                new int[]{a,b},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,getWidth(),getHeight()*.45f,q);
        q.setShader(null);
    }

    private void drawStonePlain(Canvas c) {
        drawSky(c, Color.rgb(57,100,166), Color.rgb(157,190,202));
        float h=getHeight()*.35f;
        drawMountains(c,h,Color.rgb(82,99,111),.16f,210f,95f);
        q.setColor(Color.rgb(178,164,132)); c.drawRect(0,h,getWidth(),getHeight(),q);
        q.setColor(Color.rgb(150,137,110));
        float ox=mod(worldX*.55f,145f), oy=mod(worldY*.35f,96f);
        for(int row=-1;row<8;row++) for(int col=-1;col<12;col++){
            float x=col*145f-ox+(row%2)*55f;
            float y=h+row*96f-oy;
            c.drawOval(new RectF(x,y,x+44,y+17),q);
        }
        q.setStyle(Paint.Style.STROKE); q.setStrokeWidth(2f); q.setColor(Color.argb(70,87,74,58));
        for(float y=h-mod(worldY*.30f,72f);y<getHeight();y+=72f) c.drawLine(0,y,getWidth(),y,q);
        q.setStyle(Paint.Style.FILL);
        label(c,"STONE BATTLE PLAIN");
    }

    private void drawDesertFloor(Canvas c) {
        drawSky(c, Color.rgb(58,86,139), Color.rgb(234,167,111));
        float h=getHeight()*.34f;
        drawMountains(c,h,Color.rgb(122,82,72),.18f,235f,110f);
        q.setColor(Color.rgb(211,169,104)); c.drawRect(0,h,getWidth(),getHeight(),q);
        float ox=mod(worldX*.66f,190f),oy=mod(worldY*.28f,105f);
        for(int i=-1;i<9;i++){
            float x=i*190f-ox;
            float y=h+55f+((i*59+(int)oy)%150+150)%150;
            q.setColor(Color.rgb(150,102,76)); c.drawOval(new RectF(x,y,x+58,y+23),q);
            q.setColor(Color.rgb(182,127,82)); c.drawOval(new RectF(x+9,y+4,x+39,y+14),q);
        }
        q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(2.2f);q.setColor(Color.argb(75,117,79,50));
        for(int i=0;i<7;i++){
            float x=i*185f-mod(worldX*.42f,185f);
            float y=h+80f+((i*71+(int)(worldY*.22f))%150+150)%150;
            c.drawOval(new RectF(x-40,y-11,x+40,y+11),q);
        }
        q.setStyle(Paint.Style.FILL);
        label(c,"DESERT BATTLEFIELD");
    }

    private void drawRuinedFloor(Canvas c) {
        drawSky(c, Color.rgb(30,36,66), Color.rgb(126,83,87));
        float h=getHeight()*.36f;
        drawMountains(c,h,Color.rgb(64,59,67),.15f,225f,100f);
        q.setColor(Color.rgb(126,118,105)); c.drawRect(0,h,getWidth(),getHeight(),q);
        float ox=mod(worldX*.5f,180f),oy=mod(worldY*.22f,120f);
        q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(3f);q.setColor(Color.argb(90,57,52,49));
        for(int i=-1;i<9;i++){
            float x=i*180f-ox+50;
            float y=h+55f+((i*63+(int)oy)%150+150)%150;
            c.drawOval(new RectF(x-35,y-10,x+35,y+10),q);
            c.drawOval(new RectF(x-20,y-6,x+20,y+6),q);
        }
        q.setStyle(Paint.Style.FILL);
        for(int i=-1;i<8;i++){
            float x=i*230f-mod(worldX*.38f,230f)+75;
            float y=h+90f+((i*41+(int)(worldY*.17f))%95+95)%95;
            q.setColor(Color.rgb(79,76,74)); c.drawRect(x-14,y-38,x+14,y,q);
            q.setColor(Color.rgb(57,54,53)); c.drawRect(x-10,y-31,x+8,y-20,q);
        }
        label(c,"RUINED SURFACE");
    }

    private void drawMountains(Canvas c,float base,int color,float parallax,float spacing,float height){
        float off=mod(worldX*parallax,spacing);q.setColor(color);
        for(int i=-2;i<9;i++){
            float x=i*spacing-off;Path m=new Path();
            m.moveTo(x-spacing*.58f,base);m.lineTo(x,base-height-((i&3)*10));m.lineTo(x+spacing*.58f,base);m.close();c.drawPath(m,q);
        }
    }

    private float mod(float a,float b){float x=a%b;return x<0?x+b:x;}
    private void label(Canvas c,String s){t.setTextAlign(Paint.Align.RIGHT);t.setTextSize(15);t.setColor(Color.argb(175,255,255,255));c.drawText(s,getWidth()-22,getHeight()-34,t);}

    @Override
    void drawPlayer(Canvas c) {
        boolean moving = joy && Math.hypot(jx,jy)>5;
        float phase = (float)(System.currentTimeMillis()/95.0);
        float step = moving ? (float)Math.sin(phase)*7f : 0f;
        float bob = moving ? Math.abs((float)Math.sin(phase))*3f : (float)Math.sin(System.currentTimeMillis()/260.0)*.8f;
        float leanX = moving ? jx/70f*3.5f : 0f;

        float cx=px+leanX, cy=py-bob;
        int aura=ssj>0?Color.argb(100,255,225,70):kai>0?Color.argb(95,255,75,75):Color.argb(62,95,195,255);
        q.setColor(aura); c.drawOval(new RectF(cx-38,cy-55,cx+38,cy+42),q);
        q.setColor(Color.argb(85,0,0,0)); c.drawOval(new RectF(px-27,py+29,px+27,py+40),q);

        // animated legs
        q.setStrokeCap(Paint.Cap.ROUND); q.setStrokeWidth(10f); q.setColor(Color.rgb(29,57,148));
        c.drawLine(cx-7,cy+15,cx-9-step*.75f,cy+34,q);
        c.drawLine(cx+7,cy+15,cx+9+step*.75f,cy+34,q);
        q.setStrokeWidth(8f); q.setColor(Color.rgb(29,31,43));
        c.drawLine(cx-9-step*.75f,cy+34,cx-13-step*.95f,cy+37,q);
        c.drawLine(cx+9+step*.75f,cy+34,cx+13+step*.95f,cy+37,q);

        // body
        q.setColor(Color.rgb(241,116,34)); c.drawRoundRect(new RectF(cx-17,cy-9,cx+17,cy+21),8,8,q);
        q.setColor(Color.rgb(33,65,183)); c.drawRect(cx-17,cy+7,cx+17,cy+13,q);

        // animated arms opposite the legs
        q.setStrokeWidth(7f); q.setColor(Color.rgb(245,194,145));
        c.drawLine(cx-14,cy-1,cx-23+step*.6f,cy+10,q);
        c.drawLine(cx+14,cy-1,cx+23-step*.6f,cy+10,q);

        // head
        q.setColor(Color.rgb(245,194,145)); c.drawCircle(cx,cy-21,12,q);
        q.setColor(ssj>0?Color.rgb(255,220,58):Color.rgb(27,29,37));
        Path hair=new Path();hair.moveTo(cx-13,cy-30);hair.lineTo(cx-9,cy-47);hair.lineTo(cx-2,cy-35);hair.lineTo(cx+4,cy-51);hair.lineTo(cx+10,cy-35);hair.lineTo(cx+16,cy-44);hair.lineTo(cx+13,cy-28);hair.close();c.drawPath(hair,q);
        q.setColor(Color.rgb(35,35,45));c.drawCircle(cx-4,cy-22,1.8f,q);c.drawCircle(cx+4,cy-22,1.8f,q);

        // directional speed marks while running
        if(moving){
            line.setStrokeWidth(2.5f);line.setColor(Color.argb(100,255,255,255));
            float l=(float)Math.max(1,Math.hypot(jx,jy)),ux=jx/l,uy=jy/l;
            for(int i=0;i<3;i++){
                float side=(i-1)*12f;
                c.drawLine(cx-ux*28-uy*side,cy-uy*28+ux*side,cx-ux*47-uy*side,cy-uy*47+ux*side,line);
            }
        }
    }

    @Override
    void drawEnemy(Canvas c,E e){
        super.drawEnemy(c,e);
        if(e.ty==0){
            line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(3.5f);line.setColor(Color.argb(220,30,42,28));
            c.drawCircle(e.x,e.y,e.r+2,line);
            line.setStyle(Paint.Style.FILL);
        }
    }

    @Override
    void restart(){super.restart();banner="GROUND + WALK ANIMATION";}
}
