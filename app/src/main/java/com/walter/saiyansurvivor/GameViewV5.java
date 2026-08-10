package com.walter.saiyansurvivor;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public class GameViewV5 extends View {
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG), t=new Paint(Paint.ANTI_ALIAS_FLAG);
    final Random r=new Random(); final AudioEngine audio=new AudioEngine();
    final ArrayList<E> es=new ArrayList<>(); final ArrayList<B> bs=new ArrayList<>();
    final ArrayList<O> os=new ArrayList<>(); final ArrayList<C> cs=new ArrayList<>(); final ArrayList<U> us=new ArrayList<>();
    float px,py,jx,jy,jbx,jby,hp=120,mhp=120,xp=0,nxp=12,spd=340,dmg=1,pick=110,worldX,worldY;
    int lv=1,kills=0,ki=1,kame,disc,bomb,kai,ssj,radar; boolean init,joy,dead,choose;
    long start,frame,lastSpawn,lastChest,lastBoss,lki,lkame,ldisc,lbomb,bannerUntil; String banner="FREE CAMERA";

    public GameViewV5(Context c){super(c);t.setColor(Color.WHITE);audio.start();}
    @Override protected void onDetachedFromWindow(){audio.stop();super.onDetachedFromWindow();}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){if(!init){px=w*.5f;py=h*.56f;start=System.currentTimeMillis();frame=System.nanoTime();bannerUntil=System.currentTimeMillis()+1600;init=true;}}
    @Override protected void onDraw(Canvas c){if(!init)return;long n=System.nanoTime();float dt=Math.min(.033f,(n-frame)/1e9f);frame=n;if(!dead&&!choose)update(dt);render(c);invalidate();}

    void update(float dt){long now=System.currentTimeMillis(),el=now-start;movePlayer(dt);float intensity=Math.min(1f,el/145000f+Math.min(.55f,es.size()/75f));for(E e:es)if(e.boss)intensity=1;audio.setIntensity(intensity);
        long cd=Math.max(120,650-el/210);if(now-lastSpawn>cd){spawn(false,el);if(el>65000&&r.nextFloat()<.25f)spawn(false,el);lastSpawn=now;}
        if(el>35000&&now-lastBoss>52000){spawn(true,el);audio.sfx(AudioEngine.SFX_BOSS);show("BOSS APPROACHING!");lastBoss=now;}
        if(now-lastChest>25000){double q=r.nextDouble()*Math.PI*2;float d=Math.max(getWidth(),getHeight())*.72f;cs.add(new C(px+(float)Math.cos(q)*d,py+(float)Math.sin(q)*d));lastChest=now;}
        fire(now);bullets(dt);enemies(dt);collect(dt);trim();}

    void movePlayer(float dt){float l=(float)Math.hypot(jx,jy);if(l<=1)return;float dx=jx/l*spd*dt,dy=jy/l*spd*dt;float left=getWidth()*.22f,right=getWidth()*.78f,top=getHeight()*.20f,bottom=getHeight()*.78f;
        float nx=px+dx,ny=py+dy,camX=0,camY=0;
        if(nx<left){camX=left-nx;nx=left;}else if(nx>right){camX=right-nx;nx=right;}
        if(ny<top){camY=top-ny;ny=top;}else if(ny>bottom){camY=bottom-ny;ny=bottom;}
        px=nx;py=ny;if(camX!=0||camY!=0){shift(camX,camY);worldX-=camX;worldY-=camY;}}

    void shift(float dx,float dy){for(E e:es){e.x+=dx;e.y+=dy;}for(B b:bs){b.x+=dx;b.y+=dy;}for(O o:os){o.x+=dx;o.y+=dy;}for(C c:cs){c.x+=dx;c.y+=dy;}}
    void spawn(boolean boss,long el){float m=boss?170:100,x,y;int edge=r.nextInt(4);if(edge==0){x=-m;y=r.nextFloat()*getHeight();}else if(edge==1){x=getWidth()+m;y=r.nextFloat()*getHeight();}else if(edge==2){x=r.nextFloat()*getWidth();y=-m;}else{x=r.nextFloat()*getWidth();y=getHeight()+m;}float sc=1+el/100000f;int ty=boss?4:(el<40000?0:el<85000?(r.nextBoolean()?0:1):el<130000?r.nextInt(3):r.nextInt(4));es.add(new E(x,y,ty,boss?280*sc:(34+ty*18)*sc,boss?40:18+ty*2,boss?76:108-ty*10,boss?18:7+ty*2,boss));}
    void fire(long n){if(n-lki>Math.max(220,620-(ki-1)*65)){shot(0,1+(ki>=3?1:0)+(ki>=5?1:0));audio.sfx(AudioEngine.SFX_KI);lki=n;}if(kame>0&&n-lkame>Math.max(1700,4300-kame*430)){shot(1,1);audio.sfx(AudioEngine.SFX_KAME);show("KAMEHAMEHA!");lkame=n;}if(disc>0&&n-ldisc>Math.max(1200,3200-disc*280)){shot(2,disc>=4?2:1);audio.sfx(AudioEngine.SFX_DISC);ldisc=n;}if(bomb>0&&n-lbomb>Math.max(2500,6200-bomb*420)){shot(3,1);audio.sfx(AudioEngine.SFX_BOMB);lbomb=n;}}
    void shot(int ty,int amount){E q=near();if(q==null)return;float an=(float)Math.atan2(q.y-py,q.x-px);for(int i=0;i<amount;i++){float aa=an+(i-(amount-1)/2f)*.15f,sp=ty==3?260:ty==1?620:700,rr=ty==3?20+bomb*3:ty==1?16+kame*3:ty==2?15+disc*2:9+ki,dd=(ty==3?45+bomb*16:ty==1?30+kame*13:ty==2?18+disc*8:10+ki*4)*dmg;bs.add(new B(ty,px,py,(float)Math.cos(aa)*sp,(float)Math.sin(aa)*sp,rr,dd,ty==2?3+disc:ty==1?2+kame/2:ty==3?99:Math.max(0,ki-4)));}}
    E near(){E q=null;float d=Float.MAX_VALUE;for(E e:es){float z=dist(px,py,e.x,e.y);if(z<d){d=z;q=e;}}return q;}
    void bullets(float dt){for(int i=bs.size()-1;i>=0;i--){B b=bs.get(i);b.x+=b.vx*dt;b.y+=b.vy*dt;b.life-=dt;if(b.life<0||dist(px,py,b.x,b.y)>1600){bs.remove(i);continue;}boolean rm=false;for(int j=es.size()-1;j>=0;j--){E e=es.get(j);if(dist(b.x,b.y,e.x,e.y)<b.r+e.r){e.hp-=b.d;b.h++;if(e.hp<=0)kill(j,e);if(b.h>b.p)rm=true;if(rm)break;}}if(rm)bs.remove(i);}}
    void enemies(float dt){for(E e:es){float dx=px-e.x,dy=py-e.y,d=(float)Math.max(1,Math.hypot(dx,dy));e.x+=dx/d*e.sp*dt;e.y+=dy/d*e.sp*dt;e.cd-=dt;if(d<e.r+22&&e.cd<=0){hp-=e.touch;e.cd=.7f;audio.sfx(AudioEngine.SFX_HIT);if(hp<=0){hp=0;dead=true;}}}}
    void collect(float dt){for(int i=os.size()-1;i>=0;i--){O o=os.get(i);float d=dist(px,py,o.x,o.y);if(d<pick){o.x+=(px-o.x)*Math.min(1,dt*9);o.y+=(py-o.y)*Math.min(1,dt*9);}if(d<28){xp+=o.v;if(r.nextFloat()<.35f)audio.sfx(AudioEngine.SFX_PICKUP);os.remove(i);if(xp>=nxp){xp-=nxp;lv++;nxp=nxp*1.24f+5;present();}}}for(int i=cs.size()-1;i>=0;i--){C c=cs.get(i);if(dist(px,py,c.x,c.y)<40){audio.sfx(AudioEngine.SFX_CHEST);apply(rand(),true);cs.remove(i);}}}
    void kill(int i,E e){es.remove(i);kills++;for(int n=0;n<(e.boss?18:1+(e.ty>=2?1:0));n++)os.add(new O(e.x+r.nextInt(20)-10,e.y+r.nextInt(20)-10,e.boss?3:1));if(e.boss){cs.add(new C(e.x,e.y));show("BOSS DEFEATED!");}}
    void trim(){float m=2200;es.removeIf(e->!e.boss&&dist(px,py,e.x,e.y)>m);os.removeIf(o->dist(px,py,o.x,o.y)>m);cs.removeIf(c->dist(px,py,c.x,c.y)>m*1.2f);}
    String rand(){String[] q={"ki","kame","disc","bomb","kai","senzu","radar","ssj"};return q[r.nextInt(q.length)];}
    void present(){choose=true;audio.sfx(AudioEngine.SFX_LEVEL);us.clear();ArrayList<String> q=new ArrayList<>(Arrays.asList("ki","kame","disc","bomb","kai","senzu","radar","ssj"));Collections.shuffle(q,r);for(int i=0;i<3;i++)us.add(new U(q.get(i)));}
    void apply(String id,boolean chest){String m;if(id.equals("ki")){ki=Math.min(6,ki+1);m="Ki Blast Lv "+ki;}else if(id.equals("kame")){kame=Math.min(5,kame+1);m="Kamehameha Lv "+kame;}else if(id.equals("disc")){disc=Math.min(5,disc+1);m="Kienzan Lv "+disc;}else if(id.equals("bomb")){bomb=Math.min(4,bomb+1);m="Genkidama Lv "+bomb;}else if(id.equals("kai")){kai++;spd+=25;m="Kaioken Lv "+kai;}else if(id.equals("senzu")){mhp+=18;hp=Math.min(mhp,hp+38);m="Senzu Bean";}else if(id.equals("radar")){radar++;pick+=38;m="Dragon Radar Lv "+radar;}else{ssj++;dmg+=.22f;spd+=16;audio.sfx(AudioEngine.SFX_TRANSFORM);m=ssj==1?"SUPER SAIYAN!":"Super Saiyan Lv "+ssj;}choose=false;us.clear();show((chest?"CHEST: ":"LEVEL UP: ")+m);}
    void show(String s){banner=s;bannerUntil=System.currentTimeMillis()+1700;}

    void render(Canvas c){bg(c);for(C q:cs)drawChest(c,q);for(O o:os){p.setColor(Color.CYAN);c.drawCircle(o.x,o.y,7,p);}for(B b:bs)drawBullet(c,b);for(E e:es)drawEnemy(c,e);drawPlayer(c);hud(c);if(System.currentTimeMillis()<bannerUntil)drawBanner(c);if(choose)choices(c);if(dead)over(c);}
    void bg(Canvas c){p.setShader(new LinearGradient(0,0,0,getHeight(),new int[]{Color.rgb(18,35,82),Color.rgb(80,145,170),Color.rgb(132,188,102)},null,Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),p);p.setShader(null);p.setColor(Color.argb(150,240,255,215));c.drawCircle(getWidth()*.82f,getHeight()*.15f,42,p);float gy=getHeight()*.64f;p.setColor(Color.rgb(111,170,92));c.drawRect(0,gy,getWidth(),getHeight(),p);float sx=((worldX*.18f)%190+190)%190,sy=((worldY*.08f)%70+70)%70;for(int i=-2;i<8;i++){float x=i*190-sx;p.setColor(Color.rgb(54,88,100));Path m=new Path();m.moveTo(x-90,gy);m.lineTo(x,gy-100-(i&2)*11-sy*.2f);m.lineTo(x+90,gy);m.close();c.drawPath(m,p);}float dx=((worldX*.55f)%150+150)%150;for(int i=-1;i<10;i++){float x=i*150-dx,y=gy+45+((i*37+(int)(worldY*.22f))%95);p.setColor(Color.rgb(102,112,120));c.drawOval(new RectF(x,y,x+38,y+22),p);}}
    void drawEnemy(Canvas c,E e){int[] co={Color.rgb(88,185,76),Color.rgb(130,82,190),Color.rgb(175,180,188),Color.rgb(220,118,182),Color.rgb(112,54,170)};p.setColor(co[e.ty]);if(e.ty==2)c.drawRoundRect(new RectF(e.x-e.r,e.y-e.r,e.x+e.r,e.y+e.r),7,7,p);else c.drawCircle(e.x,e.y,e.r,p);p.setColor(e.ty==0?Color.BLACK:Color.YELLOW);c.drawCircle(e.x-6,e.y-5,3,p);c.drawCircle(e.x+6,e.y-5,3,p);if(e.boss){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(Color.RED);c.drawCircle(e.x,e.y,e.r+9,p);p.setStyle(Paint.Style.FILL);}}
    void drawBullet(Canvas c,B b){if(b.ty==0){p.setColor(Color.CYAN);c.drawCircle(b.x,b.y,b.r,p);}else if(b.ty==1){float q=(float)Math.atan2(b.vy,b.vx);p.setStrokeWidth(b.r);p.setColor(Color.rgb(120,220,255));c.drawLine(b.x-(float)Math.cos(q)*110,b.y-(float)Math.sin(q)*110,b.x,b.y,p);}else if(b.ty==2){p.setColor(Color.YELLOW);c.drawCircle(b.x,b.y,b.r,p);}else{p.setColor(Color.WHITE);c.drawCircle(b.x,b.y,b.r*1.5f,p);}}
    void drawPlayer(Canvas c){p.setColor(ssj>0?Color.argb(110,255,225,70):kai>0?Color.argb(100,255,75,75):Color.argb(80,100,210,255));c.drawCircle(px,py-5,36+ssj*3,p);p.setColor(Color.rgb(242,196,142));c.drawCircle(px,py-15,11,p);p.setColor(Color.rgb(242,122,38));c.drawRect(px-15,py-4,px+15,py+18,p);p.setColor(Color.rgb(38,70,200));c.drawRect(px-11,py+18,px-2,py+34,p);c.drawRect(px+2,py+18,px+11,py+34,p);}
    void drawChest(Canvas c,C q){p.setColor(Color.rgb(112,56,28));c.drawRoundRect(new RectF(q.x-18,q.y-13,q.x+18,q.y+13),6,6,p);p.setColor(Color.YELLOW);c.drawRect(q.x-3,q.y-13,q.x+3,q.y+13,p);}
    void hud(Canvas c){p.setColor(Color.argb(180,0,0,0));c.drawRoundRect(new RectF(14,12,520,108),16,16,p);t.setTextAlign(Paint.Align.LEFT);t.setTextSize(24);t.setColor(Color.WHITE);c.drawText("LV "+lv+"  Kills "+kills+"  "+time(),28,40,t);t.setTextSize(17);c.drawText("FREE CAMERA • Distance "+(int)(Math.hypot(worldX,worldY)/100)+" m",28,64,t);p.setColor(Color.DKGRAY);c.drawRect(28,76,470,88,p);p.setColor(Color.RED);c.drawRect(28,76,28+442*(hp/mhp),88,p);p.setColor(Color.DKGRAY);c.drawRect(14,getHeight()-18,getWidth()-14,getHeight()-8,p);p.setColor(Color.CYAN);c.drawRect(14,getHeight()-18,14+(getWidth()-28)*(xp/nxp),getHeight()-8,p);float x=joy?jbx:110,y=joy?jby:getHeight()-110;p.setColor(Color.argb(70,255,255,255));c.drawCircle(x,y,70,p);p.setColor(Color.argb(150,255,255,255));c.drawCircle(x+jx,y+jy,30,p);}
    void drawBanner(Canvas c){p.setColor(Color.argb(210,0,0,0));c.drawRoundRect(getWidth()/2f-250,25,getWidth()/2f+250,88,18,18,p);t.setTextAlign(Paint.Align.CENTER);t.setColor(Color.YELLOW);t.setTextSize(29);c.drawText(banner,getWidth()/2f,66,t);}
    void choices(Canvas c){p.setColor(Color.argb(220,0,0,0));c.drawRect(0,0,getWidth(),getHeight(),p);float w=220,h=170,g=28,s=(getWidth()-(w*3+g*2))/2,y=getHeight()*.34f;t.setTextAlign(Paint.Align.CENTER);t.setColor(Color.WHITE);t.setTextSize(32);c.drawText("Choose upgrade",getWidth()/2f,90,t);for(int i=0;i<us.size();i++){U u=us.get(i);float l=s+i*(w+g);u.rc=new RectF(l,y,l+w,y+h);p.setColor(Color.rgb(24,34,54));c.drawRoundRect(u.rc,18,18,p);t.setTextSize(21);c.drawText(name(u.id),l+w/2,y+80,t);}}
    String name(String x){if(x.equals("ki"))return"Ki Blast";if(x.equals("kame"))return"Kamehameha";if(x.equals("disc"))return"Kienzan";if(x.equals("bomb"))return"Genkidama";if(x.equals("kai"))return"Kaioken";if(x.equals("senzu"))return"Senzu Bean";if(x.equals("radar"))return"Dragon Radar";return"Super Saiyan";}
    void over(Canvas c){p.setColor(Color.argb(225,0,0,0));c.drawRect(0,0,getWidth(),getHeight(),p);t.setTextAlign(Paint.Align.CENTER);t.setColor(Color.WHITE);t.setTextSize(52);c.drawText("GAME OVER",getWidth()/2f,getHeight()/2f-20,t);t.setTextSize(24);c.drawText("Tap to restart",getWidth()/2f,getHeight()/2f+35,t);}
    String time(){long s=(System.currentTimeMillis()-start)/1000;return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
    static float dist(float ax,float ay,float bx,float by){return(float)Math.hypot(ax-bx,ay-by);}

    @Override public boolean onTouchEvent(MotionEvent e){if(dead&&e.getActionMasked()==MotionEvent.ACTION_DOWN){restart();return true;}if(choose&&e.getActionMasked()==MotionEvent.ACTION_DOWN){for(U u:us)if(u.rc!=null&&u.rc.contains(e.getX(),e.getY())){apply(u.id,false);return true;}return true;}switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:joy=true;jbx=e.getX();jby=e.getY();jx=jy=0;break;case MotionEvent.ACTION_MOVE:float dx=e.getX()-jbx,dy=e.getY()-jby,d=(float)Math.max(1,Math.hypot(dx,dy)),m=Math.min(70,d);jx=dx/d*m;jy=dy/d*m;break;case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:joy=false;jx=jy=0;break;}return true;}
    void restart(){es.clear();bs.clear();os.clear();cs.clear();us.clear();px=getWidth()*.5f;py=getHeight()*.56f;hp=mhp=120;xp=0;nxp=12;spd=340;dmg=1;pick=110;worldX=worldY=0;lv=1;kills=0;ki=1;kame=disc=bomb=kai=ssj=radar=0;start=System.currentTimeMillis();lastSpawn=lastChest=lastBoss=lki=lkame=ldisc=lbomb=0;dead=choose=false;show("FREE CAMERA");}

    static class E{float x,y,hp,r,sp,touch,cd;int ty;boolean boss;E(float x,float y,int ty,float hp,float r,float sp,float touch,boolean boss){this.x=x;this.y=y;this.ty=ty;this.hp=hp;this.r=r;this.sp=sp;this.touch=touch;this.boss=boss;}}
    static class B{int ty,p,h;float x,y,vx,vy,r,d,life=2.5f;B(int ty,float x,float y,float vx,float vy,float r,float d,int p){this.ty=ty;this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.r=r;this.d=d;this.p=p;}}
    static class O{float x,y,v;O(float x,float y,float v){this.x=x;this.y=y;this.v=v;}}
    static class C{float x,y;C(float x,float y){this.x=x;this.y=y;}}
    static class U{String id;RectF rc;U(String id){this.id=id;}}
}
