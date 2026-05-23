package com.imagezeta.animator;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    int BLUE = Color.rgb(18,105,245), DARK = Color.rgb(10,24,55), BG = Color.rgb(246,249,255);
    LinearLayout root;
    CutView cutView;
    AnimView animView;
    TextView sizeChip;
    static final int PICK_CUT = 1, PICK_ANIM = 2;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        home();
    }

    int dp(int v){ return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    TextView tv(String s,int sp,int color,int style){
        TextView t=new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setTypeface(Typeface.DEFAULT,style);
        return t;
    }

    GradientDrawable bg(int color,int r){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r));
        return g;
    }

    GradientDrawable cardBg(){
        GradientDrawable g=bg(Color.WHITE,22);
        g.setStroke(dp(1),Color.rgb(220,230,245));
        return g;
    }

    Button btn(String s,int color,int textColor){
        Button b=new Button(this);
        b.setText(s); b.setAllCaps(false); b.setTextSize(13); b.setTextColor(textColor);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setBackground(bg(color,16));
        return b;
    }

    void base(){
        ScrollView sc=new ScrollView(this);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(18),dp(16),dp(18));
        root.setBackgroundColor(BG);
        sc.addView(root);
        setContentView(sc);
    }

    void home(){
        base();
        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top);

        top.addView(tv("☰",28,DARK,Typeface.BOLD));
        top.addView(tv("  Image Zeta Animator",20,DARK,Typeface.BOLD),new LinearLayout.LayoutParams(0,-2,1));

        TextView pro=tv("PRO",13,Color.WHITE,Typeface.BOLD);
        pro.setGravity(Gravity.CENTER); pro.setBackground(bg(BLUE,20));
        top.addView(pro,new LinearLayout.LayoutParams(dp(58),dp(34)));

        TextView sub=tv("Prepara imágenes para extraer fondo y animar con calidad profesional.",15,Color.rgb(80,90,115),Typeface.NORMAL);
        sub.setGravity(Gravity.CENTER); sub.setPadding(0,dp(20),0,dp(15));
        root.addView(sub);

        root.addView(big("Extraer fondo Pro+","Quita el fondo sin perder tamaño ni calidad.","PNG",BLUE,v->cut()));
        root.addView(big("Animar imagen o fondo","Crea movimiento con puntos, flechas y anclas.","▶",Color.rgb(130,70,255),v->anim()));

        root.addView(small("100% calidad original","Sin compresión. Tamaño y calidad intactos.","HD"));
        root.addView(small("Sin marca de agua","Exporta limpio y profesional.","✓"));

        TextView nav=tv("Inicio        Proyectos        Ajustes",13,BLUE,Typeface.BOLD);
        nav.setGravity(Gravity.CENTER); nav.setPadding(0,dp(20),0,0);
        root.addView(nav);
    }

    LinearLayout big(String title,String desc,String icon,int color,View.OnClickListener click){
        LinearLayout c=new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL); c.setPadding(dp(16),dp(16),dp(16),dp(16));
        c.setBackground(cardBg()); c.setOnClickListener(click);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(145));
        lp.setMargins(0,dp(8),0,dp(10)); c.setLayoutParams(lp);

        TextView ic=tv(icon,22,Color.WHITE,Typeface.BOLD);
        ic.setGravity(Gravity.CENTER); ic.setBackground(bg(color,20));
        c.addView(ic,new LinearLayout.LayoutParams(dp(82),dp(82)));

        LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(14),0,0,0);
        info.addView(tv(title,19,color,Typeface.BOLD));
        TextView d=tv(desc,14,Color.rgb(55,65,85),Typeface.NORMAL); d.setPadding(0,dp(6),0,0);
        info.addView(d);
        c.addView(info,new LinearLayout.LayoutParams(0,-2,1));

        TextView ar=tv("➜",24,Color.WHITE,Typeface.BOLD);
        ar.setGravity(Gravity.CENTER); ar.setBackground(bg(color,25));
        c.addView(ar,new LinearLayout.LayoutParams(dp(44),dp(44)));
        return c;
    }

    LinearLayout small(String title,String desc,String icon){
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(14),dp(12),dp(14),dp(12));
        c.setBackground(cardBg());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(105));
        lp.setMargins(0,dp(8),0,0); c.setLayoutParams(lp);
        c.addView(tv(icon,18,BLUE,Typeface.BOLD));
        c.addView(tv(title,15,DARK,Typeface.BOLD));
        c.addView(tv(desc,12,Color.rgb(95,105,130),Typeface.NORMAL));
        return c;
    }

    void cut(){
        base();
        bar("Extraer fondo",v->home());

        LinearLayout chips=new LinearLayout(this);
        chips.setPadding(0,dp(10),0,dp(10)); root.addView(chips);
        sizeChip=chip("Tamaño original: abre imagen");
        chips.addView(sizeChip,new LinearLayout.LayoutParams(0,dp(42),1));
        chips.addView(chip("Calidad: original"),new LinearLayout.LayoutParams(0,dp(42),1));

        cutView=new CutView(this);
        cutView.setBackground(cardBg());
        root.addView(cutView,new LinearLayout.LayoutParams(-1,dp(420)));

        row("Auto","Punto","Borde");
        row("Borrador suave","Borrador duro","Precisión");
        row("Restaurar","Zoom","");

        Button open=btn("Abrir imagen",Color.WHITE,BLUE);
        open.setOnClickListener(v->pick(PICK_CUT));
        root.addView(open,new LinearLayout.LayoutParams(-1,dp(52)));

        Button save=btn("Exportar PNG transparente",BLUE,Color.WHITE);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));
        lp.setMargins(0,dp(10),0,0); root.addView(save,lp);
        save.setOnClickListener(v->savePng());
    }

    void anim(){
        base();
        bar("Animar",v->home());

        LinearLayout tools=new LinearLayout(this);
        tools.setPadding(0,dp(10),0,dp(10)); root.addView(tools);
        String[] names={"Movimiento","Puntos","Ancla","Velocidad","Máscara"};
        for(String n:names) tools.addView(chip(n),new LinearLayout.LayoutParams(0,dp(52),1));

        animView=new AnimView(this);
        animView.setBackground(cardBg());
        root.addView(animView,new LinearLayout.LayoutParams(-1,dp(430)));

        Button open=btn("Abrir fondo o imagen",Color.WHITE,BLUE);
        open.setOnClickListener(v->pick(PICK_ANIM));
        root.addView(open,new LinearLayout.LayoutParams(-1,dp(52)));

        TextView time=tv("▶   00:03 / 00:10      ━━━━━●━━━━",14,DARK,Typeface.BOLD);
        time.setGravity(Gravity.CENTER); time.setBackground(cardBg());
        root.addView(time,new LinearLayout.LayoutParams(-1,dp(55)));

        Button export=btn("Exportar video  MP4 / GIF",BLUE,Color.WHITE);
        root.addView(export,new LinearLayout.LayoutParams(-1,dp(58)));
    }

    void bar(String title,View.OnClickListener backClick){
        LinearLayout b=new LinearLayout(this); b.setGravity(Gravity.CENTER_VERTICAL); root.addView(b);
        TextView back=tv("‹",34,DARK,Typeface.BOLD); back.setOnClickListener(backClick);
        b.addView(back,new LinearLayout.LayoutParams(dp(42),-2));
        TextView t=tv(title,20,DARK,Typeface.BOLD); t.setGravity(Gravity.CENTER);
        b.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        b.addView(tv("?",20,DARK,Typeface.BOLD),new LinearLayout.LayoutParams(dp(36),-2));
    }

    TextView chip(String s){
        TextView c=tv(s,11,BLUE,Typeface.BOLD);
        c.setGravity(Gravity.CENTER); c.setBackground(cardBg());
        return c;
    }

    void row(String a,String b,String c){
        LinearLayout r=new LinearLayout(this);
        r.setPadding(0,dp(4),0,dp(4)); root.addView(r);
        r.addView(tool(a),new LinearLayout.LayoutParams(0,dp(66),1));
        r.addView(tool(b),new LinearLayout.LayoutParams(0,dp(66),1));
        if(!c.equals("")) r.addView(tool(c),new LinearLayout.LayoutParams(0,dp(66),1));
    }

    Button tool(String n){
        Button b=btn(n,Color.WHITE,DARK); b.setTextSize(11);
        b.setOnClickListener(v->{
            if(cutView==null)return;
            if(n.equals("Auto"))cutView.autoRemove();
            else if(n.equals("Punto"))cutView.setTool(true,22);
            else if(n.equals("Borde"))cutView.setTool(true,12);
            else if(n.equals("Borrador suave"))cutView.setTool(true,54);
            else if(n.equals("Borrador duro"))cutView.setTool(true,30);
            else if(n.equals("Precisión"))cutView.setTool(true,9);
            else if(n.equals("Restaurar"))cutView.setTool(false,35);
            else if(n.equals("Zoom"))cutView.zoomChange();
        });
        return b;
    }

    void pick(int code){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,code);
    }

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(res!=RESULT_OK||data==null)return;
        try{
            Bitmap bm=load(data.getData());
            if(req==PICK_CUT&&cutView!=null){
                cutView.setBitmap(bm);
                sizeChip.setText("Tamaño original: "+bm.getWidth()+" × "+bm.getHeight());
            }
            if(req==PICK_ANIM&&animView!=null) animView.setBitmap(bm);
        }catch(Exception e){ Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }

    Bitmap load(Uri uri)throws Exception{
        InputStream in=getContentResolver().openInputStream(uri);
        BitmapFactory.Options o=new BitmapFactory.Options();
        o.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap b=BitmapFactory.decodeStream(in,null,o);
        if(in!=null)in.close();
        return b.copy(Bitmap.Config.ARGB_8888,true);
    }

    void savePng(){
        if(cutView==null||cutView.bitmap()==null){Toast.makeText(this,"Primero abre una imagen.",Toast.LENGTH_SHORT).show();return;}
        try{
            String name="ImageZeta_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".png";
            ContentValues v=new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME,name);
            v.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
            if(Build.VERSION.SDK_INT>=29){
                v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+"/Image Zeta Animator");
                v.put(MediaStore.Images.Media.IS_PENDING,1);
            }
            Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);
            OutputStream os=getContentResolver().openOutputStream(uri);
            cutView.bitmap().compress(Bitmap.CompressFormat.PNG,100,os);
            if(os!=null)os.close();
            if(Build.VERSION.SDK_INT>=29){
                v.clear(); v.put(MediaStore.Images.Media.IS_PENDING,0);
                getContentResolver().update(uri,v,null,null);
            }
            Toast.makeText(this,"PNG guardado sin cambiar tamaño original.",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"Error al guardar PNG.",Toast.LENGTH_LONG).show();}
    }

    class CutView extends View{
        Bitmap original,work;
        Canvas workCanvas;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        Paint clear=new Paint(Paint.ANTI_ALIAS_FLAG);
        Matrix m=new Matrix(),inv=new Matrix();
        boolean erase=true;
        float brush=30,zoom=1;

        CutView(Activity a){super(a);setLayerType(View.LAYER_TYPE_SOFTWARE,null);clear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));}

        void setBitmap(Bitmap b){original=b.copy(Bitmap.Config.ARGB_8888,true);work=b.copy(Bitmap.Config.ARGB_8888,true);workCanvas=new Canvas(work);invalidate();}
        Bitmap bitmap(){return work;}
        void setTool(boolean e,int s){erase=e;brush=s;}
        void zoomChange(){zoom=zoom==1?2:zoom==2?3:1;invalidate();}

        void autoRemove(){
            if(work==null)return;
            int w=work.getWidth(),h=work.getHeight();
            int c=work.getPixel(0,0);
            int r=Color.red(c),g=Color.green(c),b=Color.blue(c);
            int[] px=new int[w*h]; work.getPixels(px,0,w,0,0,w,h);
            for(int i=0;i<px.length;i++){
                int q=px[i];
                int d=Math.abs(Color.red(q)-r)+Math.abs(Color.green(q)-g)+Math.abs(Color.blue(q)-b);
                if(d<120)px[i]=Color.TRANSPARENT;
            }
            work.setPixels(px,0,w,0,0,w,h); workCanvas=new Canvas(work); invalidate();
        }

        @Override protected void onDraw(Canvas c){
            checker(c);
            if(work==null){Paint t=new Paint(Paint.ANTI_ALIAS_FLAG);t.setColor(Color.GRAY);t.setTextAlign(Paint.Align.CENTER);t.setTextSize(dp(16));c.drawText("Abre una imagen",getWidth()/2,getHeight()/2,t);return;}
            matrix(); c.drawBitmap(work,m,p);
        }

        void checker(Canvas c){
            Paint a=new Paint(),b=new Paint();a.setColor(Color.rgb(235,238,245));b.setColor(Color.WHITE);
            int s=dp(18);
            for(int y=0;y<getHeight();y+=s)for(int x=0;x<getWidth();x+=s)c.drawRect(x,y,x+s,y+s,((x/s+y/s)%2==0)?a:b);
        }

        void matrix(){
            m.reset();
            float scale=Math.min(getWidth()/(float)work.getWidth(),getHeight()/(float)work.getHeight())*zoom;
            float dx=(getWidth()-work.getWidth()*scale)/2,dy=(getHeight()-work.getHeight()*scale)/2;
            m.postScale(scale,scale);m.postTranslate(dx,dy);m.invert(inv);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(work==null)return true;
            float[] pt={e.getX(),e.getY()}; matrix(); inv.mapPoints(pt);
            if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){draw(pt[0],pt[1]);return true;}
            return true;
        }

        void draw(float x,float y){
            if(x<0||y<0||x>=work.getWidth()||y>=work.getHeight())return;
            float r=Math.max(6,brush*(work.getWidth()/1080f));
            if(erase)workCanvas.drawCircle(x,y,r,clear);
            else{Path path=new Path();path.addCircle(x,y,r,Path.Direction.CW);workCanvas.save();workCanvas.clipPath(path);workCanvas.drawBitmap(original,0,0,p);workCanvas.restore();}
            invalidate();
        }
    }

    class AnimView extends View{
        Bitmap bm; Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        AnimView(Activity a){super(a);}
        void setBitmap(Bitmap b){bm=b;invalidate();}

        @Override protected void onDraw(Canvas c){
            if(bm==null){Paint x=new Paint();x.setColor(Color.rgb(45,130,230));c.drawRect(0,0,getWidth(),getHeight(),x);}
            else{
                float s=Math.max(getWidth()/(float)bm.getWidth(),getHeight()/(float)bm.getHeight());
                Matrix m=new Matrix();m.postScale(s,s);m.postTranslate((getWidth()-bm.getWidth()*s)/2,(getHeight()-bm.getHeight()*s)/2);
                c.drawBitmap(bm,m,p);
            }
            guide(c);
        }

        void guide(Canvas c){
            Paint l=new Paint(Paint.ANTI_ALIAS_FLAG);l.setColor(Color.rgb(0,180,255));l.setStrokeWidth(dp(3));l.setStyle(Paint.Style.STROKE);
            c.drawLine(getWidth()*.15f,getHeight()*.65f,getWidth()*.35f,getHeight()*.45f,l);
            c.drawLine(getWidth()*.85f,getHeight()*.65f,getWidth()*.65f,getHeight()*.45f,l);
            Paint pt=new Paint(Paint.ANTI_ALIAS_FLAG);pt.setColor(Color.rgb(35,220,120));
            c.drawCircle(getWidth()*.25f,getHeight()*.20f,dp(8),pt);
            c.drawCircle(getWidth()*.50f,getHeight()*.28f,dp(8),pt);
            c.drawCircle(getWidth()*.72f,getHeight()*.18f,dp(8),pt);
        }
    }
                           }
