package com.leomerlubo.clockcalendar;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    static final String PREFS = "atelier_clock";
    android.content.SharedPreferences prefs;

    int bg, fg, accent;
    boolean analog, month, seconds, twentyFour;
    float brightness;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        load();
        if (!prefs.getBoolean("configured", false)) showSetup(); else showClock();
    }

    void load() {
        bg = prefs.getInt("bg", Color.rgb(5, 5, 6));
        fg = prefs.getInt("fg", Color.rgb(235, 234, 230));
        accent = prefs.getInt("accent", Color.rgb(117, 142, 255));
        analog = prefs.getBoolean("analog", false);
        month = prefs.getBoolean("month", true);
        seconds = prefs.getBoolean("seconds", false);
        twentyFour = prefs.getBoolean("twentyFour", false);
        brightness = prefs.getFloat("brightness", .48f);
    }

    void save() {
        prefs.edit().putBoolean("configured", true).putInt("bg", bg).putInt("fg", fg)
            .putInt("accent", accent).putBoolean("analog", analog).putBoolean("month", month)
            .putBoolean("seconds", seconds).putBoolean("twentyFour", twentyFour)
            .putFloat("brightness", brightness).apply();
    }

    GradientDrawable panel(int color, float radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    TextView text(String value, float size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL); t.setFontFeatureSettings("kern"); return t;
    }
    void pad(View v, int h, int vert) { v.setPadding(dp(h), dp(vert), dp(h), dp(vert)); }
    void space(LinearLayout root, int h) { Space s = new Space(this); root.addView(s, new LinearLayout.LayoutParams(1, dp(h))); }

    public void showSetup() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setStatusBarColor(Color.rgb(8,8,10));
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.rgb(8,8,10)); pad(page, 22, 18);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(page);

        TextView overline = text("ATELIER  /  DESK DISPLAY", 10, Color.rgb(130,130,136));
        overline.setLetterSpacing(.18f); page.addView(overline);
        TextView title = text("Clock & Calendar", 31, Color.WHITE); title.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        page.addView(title); space(page, 4);
        TextView sub = text("A quieter place for time.", 15, Color.rgb(150,150,158)); page.addView(sub); space(page, 24);

        ClockView preview = new ClockView(this, true); preview.setBackground(panel(bg, 22, Color.rgb(40,40,44)));
        page.addView(preview, new LinearLayout.LayoutParams(-1, dp(205))); space(page, 25);

        addLabel(page, "CLOCK"); LinearLayout clockChoice = segmented("Digital", "Analog", !analog, v -> { analog = v == 1; preview.invalidate(); }); page.addView(clockChoice);
        space(page, 18); addLabel(page, "CALENDAR"); LinearLayout calChoice = segmented("Full month", "Today only", month, v -> { month = v == 0; preview.invalidate(); }); page.addView(calChoice);
        space(page, 22); addLabel(page, "PALETTE");
        LinearLayout palettes = new LinearLayout(this); palettes.setGravity(Gravity.CENTER); palettes.setOrientation(LinearLayout.HORIZONTAL);
        int[][] themes = {{0xff050506,0xffebeae6,0xff758eff},{0xff0b1012,0xffe4eee9,0xff66c7a4},{0xff15110e,0xfff0e8dc,0xffc99b68},{0xff111016,0xffeeeaf7,0xffb091ee}};
        for (int[] theme : themes) { View sw = new View(this); GradientDrawable sd = panel(theme[0], 30, 0xff35353a); sw.setBackground(sd);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(48),dp(48)); sp.setMargins(dp(5),0,dp(5),0); palettes.addView(sw,sp);
            sw.setOnClickListener(v -> { bg=theme[0]; fg=theme[1]; accent=theme[2]; preview.setBackground(panel(bg,22,0xff28282c)); preview.invalidate(); }); }
        page.addView(palettes); space(page, 12);
        page.addView(colorRow("Background", () -> bg, c -> {bg=c; preview.setBackground(panel(bg,22,0xff28282c)); preview.invalidate();}));
        page.addView(colorRow("Text", () -> fg, c -> {fg=c; preview.invalidate();}));
        page.addView(colorRow("Accent", () -> accent, c -> {accent=c; preview.invalidate();}));
        space(page, 18); addLabel(page, "DETAILS");
        page.addView(toggleRow("Show seconds", seconds, b -> {seconds=b; preview.invalidate();}));
        page.addView(toggleRow("24 hour time", twentyFour, b -> {twentyFour=b; preview.invalidate();}));
        TextView brightLabel = text("Display brightness", 14, 0xffd8d8dc); page.addView(brightLabel);
        SeekBar bright = new SeekBar(this); bright.setMax(100); bright.setProgress((int)(brightness*100)); bright.setOnSeekBarChangeListener(new SimpleSeek(){public void onProgressChanged(SeekBar s,int p,boolean f){brightness=Math.max(.05f,p/100f);}}); page.addView(bright);
        space(page, 20);
        TextView start = text("Enter focus display", 15, Color.BLACK); start.setGravity(Gravity.CENTER); start.setTypeface(Typeface.DEFAULT_BOLD);
        start.setBackground(panel(0xfff0f0ee, 14, 0)); pad(start,16,16); page.addView(start);
        start.setOnClickListener(v -> { save(); showClock(); }); space(page, 24);
        setContentView(scroll);
    }

    interface IntPick { void set(int v); } interface IntGet { int get(); } interface BoolPick { void set(boolean v); }
    abstract class SimpleSeek implements SeekBar.OnSeekBarChangeListener { public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){} }
    void addLabel(LinearLayout p,String s){TextView v=text(s,10,0xff85858c);v.setLetterSpacing(.16f);p.addView(v);space(p,8);}

    LinearLayout segmented(String a,String b,boolean first,IntPick pick){
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(dp(3),dp(3),dp(3),dp(3)); row.setBackground(panel(0xff151517,14,0xff29292d));
        TextView x=text(a,14,Color.WHITE), y=text(b,14,Color.WHITE); x.setGravity(17);y.setGravity(17); row.addView(x,new LinearLayout.LayoutParams(0,dp(46),1));row.addView(y,new LinearLayout.LayoutParams(0,dp(46),1));
        final boolean[] selectedFirst={first};
        Runnable draw=()->{x.setBackground(panel((selectedFirst[0]?0xff303034:Color.TRANSPARENT),11,0));y.setBackground(panel((!selectedFirst[0]?0xff303034:Color.TRANSPARENT),11,0));};
        draw.run(); x.setOnClickListener(v->{selectedFirst[0]=true;draw.run();pick.set(0);}); y.setOnClickListener(v->{selectedFirst[0]=false;draw.run();pick.set(1);}); return row;
    }

    LinearLayout toggleRow(String label, boolean value, BoolPick pick){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL); TextView t=text(label,14,0xffd8d8dc); Switch s=new Switch(this);s.setChecked(value);s.setOnCheckedChangeListener((b,c)->pick.set(c));row.addView(t,new LinearLayout.LayoutParams(0,dp(52),1));row.addView(s);return row;
    }
    LinearLayout colorRow(String label, IntGet get, IntPick pick){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView t=text(label,14,0xffd8d8dc);View chip=new View(this);chip.setBackground(panel(get.get(),20,0xff45454a));row.addView(t,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(chip,new LinearLayout.LayoutParams(dp(34),dp(34)));
        row.setOnClickListener(v->showColorDialog(label,get.get(),c->{pick.set(c);chip.setBackground(panel(c,20,0xff45454a));}));return row;
    }
    void showColorDialog(String name,int initial,IntPick done){
        float[] hsv=new float[3];Color.colorToHSV(initial,hsv);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);pad(box,18,8);
        TextView sample=text("Aa  12:48",30,initial);sample.setGravity(17);box.addView(sample,new LinearLayout.LayoutParams(-1,dp(74)));
        String[] labs={"Hue","Saturation","Brightness"};int[] max={360,100,100};int[] vals={(int)hsv[0],(int)(hsv[1]*100),(int)(hsv[2]*100)};
        for(int i=0;i<3;i++){TextView l=text(labs[i],12,0xff55555a);box.addView(l);SeekBar s=new SeekBar(this);s.setMax(max[i]);s.setProgress(vals[i]);final int n=i;s.setOnSeekBarChangeListener(new SimpleSeek(){public void onProgressChanged(SeekBar q,int p,boolean f){hsv[n]=n==0?p:p/100f;sample.setTextColor(Color.HSVToColor(hsv));}});box.addView(s);}
        new AlertDialog.Builder(this).setTitle(name+" color").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Use color",(d,w)->done.set(Color.HSVToColor(hsv))).show();
    }

    void showClock(){
        save(); Window w=getWindow();w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);WindowManager.LayoutParams lp=w.getAttributes();lp.screenBrightness=brightness;w.setAttributes(lp);
        immersive(); setContentView(new ClockView(this,false));
    }
    void immersive(){getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);}
    @Override public void onWindowFocusChanged(boolean h){super.onWindowFocusChanged(h);if(h&&prefs.getBoolean("configured",false)&&getWindow().getDecorView().getSystemUiVisibility()!=0)immersive();}

    class ClockView extends View {
        Paint p=new Paint(3); Calendar now=Calendar.getInstance(); boolean preview; Handler timer=new Handler(Looper.getMainLooper()); float downX,downY; long downAt; int shiftX,shiftY;
        Runnable tick=new Runnable(){public void run(){now=Calendar.getInstance();int slot=(int)(System.currentTimeMillis()/300000L);shiftX=(slot%5)-2;shiftY=((slot/5)%5)-2;invalidate();timer.postDelayed(this,1000);}};
        ClockView(Context c,boolean preview){super(c);this.preview=preview;setBackgroundColor(bg);if(!preview)timer.post(tick);setOnTouchListener((v,e)->{if(preview)return true;if(e.getAction()==0&&e.getX()>getWidth()-dp(70)&&e.getY()<dp(70)){downAt=System.currentTimeMillis();return true;}if(e.getAction()==1&&downAt>0){if(System.currentTimeMillis()-downAt>700)exitMenu();downAt=0;return true;}return true;});}
        @Override protected void onDetachedFromWindow(){timer.removeCallbacks(tick);super.onDetachedFromWindow();}
        void font(float size,int color,Paint.Align align){p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(size);p.setColor(color);p.setTextAlign(align);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);now=Calendar.getInstance();c.save();if(!preview)c.translate(dp(shiftX),dp(shiftY));boolean land=getWidth()>getHeight();float split=land?getWidth()*.52f:getHeight()*.49f;
            if(land){drawClock(c,0,0,split,getHeight());drawCalendar(c,split,0,getWidth()-split,getHeight());line(c,split,getHeight()*.14f,split,getHeight()*.86f);}else{drawClock(c,0,0,getWidth(),split);drawCalendar(c,0,split,getWidth(),getHeight()-split);line(c,getWidth()*.12f,split,getWidth()*.88f,split);}if(!preview){font(dp(20),blend(fg,bg,.22f),Paint.Align.CENTER);c.drawText("×",getWidth()-dp(30),dp(38),p);}c.restore();}
        void line(Canvas c,float a,float b,float x,float y){p.setColor(blend(fg,bg,.11f));p.setStrokeWidth(dp(1));c.drawLine(a,b,x,y,p);}
        void drawClock(Canvas c,float x,float y,float w,float h){if(analog)drawAnalog(c,x,y,w,h);else drawDigital(c,x,y,w,h);}
        void drawDigital(Canvas c,float x,float y,float w,float h){String pattern=twentyFour?(seconds?"HH:mm:ss":"HH:mm"):(seconds?"h:mm:ss":"h:mm");String time=new SimpleDateFormat(pattern,Locale.getDefault()).format(now.getTime());float size=Math.min(w/(time.length()*.57f),h*.40f);font(size,fg,Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.create(Typeface.DEFAULT,Typeface.NORMAL).getStyle()));c.drawText(time,x+w/2,y+h*.54f,p);String am=twentyFour?"":new SimpleDateFormat("a",Locale.getDefault()).format(now.getTime());font(Math.max(dp(9),size*.105f),blend(fg,bg,.48f),Paint.Align.CENTER);p.setLetterSpacing(.14f);c.drawText(am,x+w/2,y+h*.68f,p);p.setLetterSpacing(0);}
        void drawAnalog(Canvas c,float x,float y,float w,float h){float cx=x+w/2,cy=y+h/2,r=Math.min(w,h)*.31f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(blend(fg,bg,.2f));c.drawCircle(cx,cy,r,p);for(int i=0;i<60;i++){double a=i*Math.PI/30-Math.PI/2;float in=r*(i%5==0?.88f:.95f);p.setColor(i%5==0?blend(fg,bg,.65f):blend(fg,bg,.22f));p.setStrokeWidth(i%5==0?dp(2):dp(1));c.drawLine(cx+(float)Math.cos(a)*in,cy+(float)Math.sin(a)*in,cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,p);}float min=now.get(Calendar.MINUTE)+now.get(Calendar.SECOND)/60f;float hr=now.get(Calendar.HOUR)+min/60f;hand(c,cx,cy,r*.54f,hr*Math.PI/6-Math.PI/2,fg,dp(4));hand(c,cx,cy,r*.76f,min*Math.PI/30-Math.PI/2,fg,dp(3));if(seconds)hand(c,cx,cy,r*.76f,now.get(Calendar.SECOND)*Math.PI/30-Math.PI/2,accent,dp(1));p.setStyle(Paint.Style.FILL);p.setColor(accent);c.drawCircle(cx,cy,dp(4),p);}
        void hand(Canvas c,float x,float y,float len,double a,int color,float sw){p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(sw);p.setColor(color);c.drawLine(x,y,x+(float)Math.cos(a)*len,y+(float)Math.sin(a)*len,p);p.setStrokeCap(Paint.Cap.BUTT);}
        void drawCalendar(Canvas c,float x,float y,float w,float h){if(month)drawMonth(c,x,y,w,h);else drawToday(c,x,y,w,h);}
        void drawToday(Canvas c,float x,float y,float w,float h){String week=new SimpleDateFormat("EEEE",Locale.getDefault()).format(now.getTime()).toUpperCase();String mon=new SimpleDateFormat("MMMM",Locale.getDefault()).format(now.getTime()).toUpperCase();font(Math.min(dp(13),w*.032f),blend(fg,bg,.48f),Paint.Align.CENTER);p.setLetterSpacing(.22f);c.drawText(week,x+w/2,y+h*.30f,p);p.setLetterSpacing(0);font(Math.min(w*.24f,h*.34f),fg,Paint.Align.CENTER);c.drawText(String.valueOf(now.get(Calendar.DAY_OF_MONTH)),x+w/2,y+h*.61f,p);font(Math.min(dp(13),w*.034f),accent,Paint.Align.CENTER);p.setLetterSpacing(.18f);c.drawText(mon+"  "+now.get(Calendar.YEAR),x+w/2,y+h*.76f,p);p.setLetterSpacing(0);}
        void drawMonth(Canvas c,float x,float y,float w,float h){Calendar first=(Calendar)now.clone();first.set(Calendar.DAY_OF_MONTH,1);String title=new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(now.getTime());float top=y+h*.17f; font(Math.min(dp(20),w*.055f),fg,Paint.Align.LEFT);c.drawText(title,x+w*.12f,top,p);String[] days={"S","M","T","W","T","F","S"};float left=x+w*.12f,right=x+w*.88f,cw=(right-left)/7f;float head=y+h*.30f;for(int i=0;i<7;i++){font(Math.min(dp(10),cw*.27f),blend(fg,bg,.35f),Paint.Align.CENTER);c.drawText(days[i],left+cw*(i+.5f),head,p);}int start=first.get(Calendar.DAY_OF_WEEK)-1,max=first.getActualMaximum(Calendar.DAY_OF_MONTH);float rowH=h*.105f;for(int d=1;d<=max;d++){int pos=start+d-1,col=pos%7,row=pos/7;float cx=left+cw*(col+.5f),cy=head+rowH*(row+1);if(d==now.get(Calendar.DAY_OF_MONTH)){p.setColor(accent);p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy-dp(4),Math.min(dp(16),cw*.34f),p);font(Math.min(dp(12),cw*.34f),bg,Paint.Align.CENTER);}else font(Math.min(dp(12),cw*.34f),fg,Paint.Align.CENTER);c.drawText(String.valueOf(d),cx,cy,p);}}
        int blend(int a,int b,float t){return Color.rgb((int)(Color.red(b)+(Color.red(a)-Color.red(b))*t),(int)(Color.green(b)+(Color.green(a)-Color.green(b))*t),(int)(Color.blue(b)+(Color.blue(a)-Color.blue(b))*t));}
    }
    void exitMenu(){
        immersive(); final String[] items={"Adjust display","Exit Clock & Calendar"};
        new AlertDialog.Builder(this).setTitle("Focus display").setItems(items,(d,w)->{if(w==0)showSetup();else finishAndRemoveTask();}).setNegativeButton("Continue focusing",null).show();
    }
}
