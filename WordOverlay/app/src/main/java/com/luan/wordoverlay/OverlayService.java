package com.luan.wordoverlay;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.*;

public class OverlayService extends Service {
    public static final String ACTION_START = "com.luan.wordoverlay.START";
    private static final String ACTION_CAPTURE = "com.luan.wordoverlay.CAPTURE";
    private static final int NOTIF_ID = 101;

    private WindowManager wm;
    private TextView bubble;
    private View resultPanel;
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private TextRecognizer recognizer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int screenW, screenH, density;

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW=dm.widthPixels; screenH=dm.heightPixels; density=dm.densityDpi;
        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, buildNotification("Toque na bolinha para resolver"), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION); else startForeground(NOTIF_ID, buildNotification("Toque na bolinha para resolver"));
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            int result = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra("data");
            if (data != null && result == Activity.RESULT_OK) startProjection(result, data);
            showBubble();
        } else if (intent != null && ACTION_CAPTURE.equals(intent.getAction())) captureAndSolve();
        return START_STICKY;
    }

    private void startProjection(int result, Intent data) {
        MediaProjectionManager mpm=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        projection=mpm.getMediaProjection(result,data);
        if (projection==null) return;
        reader=ImageReader.newInstance(screenW,screenH,android.graphics.PixelFormat.RGBA_8888,2);
        display=projection.createVirtualDisplay("WordOverlayCapture",screenW,screenH,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
        projection.registerCallback(new MediaProjection.Callback(){ @Override public void onStop(){ if(display!=null) display.release(); } },handler);
    }

    private void showBubble() {
        if (bubble != null) return;
        bubble = new TextView(this); bubble.setText("✦"); bubble.setTextColor(Color.WHITE); bubble.setTextSize(22); bubble.setGravity(Gravity.CENTER);
        GradientDrawable bg=new GradientDrawable(); bg.setShape(GradientDrawable.OVAL); bg.setColor(Color.rgb(168,85,247)); bg.setStroke(2,Color.WHITE); bubble.setBackground(bg);
        bubble.setElevation(12f); bubble.setOnClickListener(v -> captureAndSolve());
        bubble.setOnTouchListener(new View.OnTouchListener(){ float dx,dy; boolean moved;
            public boolean onTouch(View v, android.view.MotionEvent e){
                WindowManager.LayoutParams lp=(WindowManager.LayoutParams)v.getLayoutParams();
                if(e.getAction()==MotionEvent.ACTION_DOWN){dx=e.getRawX()-lp.x;dy=e.getRawY()-lp.y;moved=false;return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){lp.x=(int)(e.getRawX()-dx);lp.y=(int)(e.getRawY()-dy);wm.updateViewLayout(v,lp);moved=true;return true;}
                if(e.getAction()==MotionEvent.ACTION_UP){if(!moved)v.performClick();return true;} return false;
            }});
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(58,58,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,android.graphics.PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP|Gravity.START; lp.x=screenW-90; lp.y=220;
        wm.addView(bubble,lp);
    }

    private void captureAndSolve() {
        if (reader==null) { showResult("Permissão de captura não está ativa.", Collections.emptyMap()); return; }
        handler.postDelayed(() -> {
            Image image=null;
            try { image=reader.acquireLatestImage(); } catch(Exception ignored){}
            if(image==null){ handler.postDelayed(this::captureAndSolve,180); return; }
            Bitmap full=imageToBitmap(image); image.close();
            if(full==null) return;
            Set<Integer> lengths=detectWordLengths(full);
            Bitmap crop=cropWheel(full);
            InputImage input=InputImage.fromBitmap(crop,0);
            recognizer.process(input).addOnSuccessListener(result -> {
                String letters=extractLetters(result);
                Map<Integer,List<String>> solved=WordSolver.solve(letters,lengths);
                showResult(letters.isEmpty()?"Não consegui ler as letras. Tente tocar novamente.":"Letras: "+letters,solved);
            }).addOnFailureListener(e -> showResult("OCR falhou. Tente novamente.",Collections.emptyMap()));
        },220);
    }

    private Bitmap imageToBitmap(Image image){
        Image.Plane p=image.getPlanes()[0]; ByteBuffer b=p.getBuffer(); int ps=p.getPixelStride(), rs=p.getRowStride(); int pad=rs-ps*screenW;
        Bitmap bmp=Bitmap.createBitmap(screenW+pad/ps,screenH,Bitmap.Config.ARGB_8888); bmp.copyPixelsFromBuffer(b); return Bitmap.createBitmap(bmp,0,0,screenW,screenH);
    }

    private Bitmap cropWheel(Bitmap b){
        int x=(int)(b.getWidth()*0.10), y=(int)(b.getHeight()*0.48), w=(int)(b.getWidth()*0.80), h=(int)(b.getHeight()*0.32);
        x=Math.max(0,Math.min(x,b.getWidth()-1)); y=Math.max(0,Math.min(y,b.getHeight()-1)); w=Math.min(w,b.getWidth()-x); h=Math.min(h,b.getHeight()-y);
        return Bitmap.createBitmap(b,x,y,w,h);
    }

    private String extractLetters(Text result){
        ArrayList<String> tokens=new ArrayList<>();
        for(Text.TextBlock block:result.getTextBlocks()) for(Text.Line line:block.getLines()) {
            String raw=line.getText();
            for(String tok:raw.split("\\s+")){ String n=WordSolver.normalize(tok); if(!n.isEmpty()) tokens.add(n.toUpperCase(Locale.ROOT)); }
        }
        // The wheel normally contains isolated one-letter glyphs. Prefer those.
        StringBuilder out=new StringBuilder();
        for(String t:tokens) if(t.length()==1 && t.charAt(0)>='A'&&t.charAt(0)<='Z') out.append(t);
        // OCR may merge the wheel into one compact token such as ROAFG.
        if(out.length()<3){
            for(String t:tokens) if(t.length()>=3 && t.length()<=8){
                StringBuilder candidate=new StringBuilder();
                for(char c:t.toCharArray()) if(c>='A'&&c<='Z') candidate.append(c);
                if(candidate.length()>=3){ out.setLength(0); out.append(candidate); break; }
            }
        }
        return out.toString();
    }

    private Set<Integer> detectWordLengths(Bitmap b){
        HashSet<Integer> lengths=new HashSet<>();
        try {
            int w=b.getWidth(), h=b.getHeight(); int y0=(int)(h*0.08), y1=(int)(h*0.52);
            boolean[][] seen=new boolean[h-y0][w]; ArrayList<Rect> cells=new ArrayList<>();
            for(int y=y0;y<y1;y++) for(int x=0;x<w;x++){
                if(seen[y-y0][x] || !isWhite(b.getPixel(x,y))) continue;
                int minX=x,maxX=x,minY=y,maxY=y,count=0; ArrayDeque<Point> q=new ArrayDeque<>(); q.add(new Point(x,y)); seen[y-y0][x]=true;
                while(!q.isEmpty()){
                    Point p=q.removeFirst(); count++; minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minY=Math.min(minY,p.y);maxY=Math.max(maxY,p.y);
                    int[][] ds={{1,0},{-1,0},{0,1},{0,-1}};
                    for(int[] d:ds){int nx=p.x+d[0],ny=p.y+d[1]; if(nx>=0&&nx<w&&ny>=y0&&ny<y1&&!seen[ny-y0][nx]&&isWhite(b.getPixel(nx,ny))){seen[ny-y0][nx]=true;q.add(new Point(nx,ny));}}
                }
                int cw=maxX-minX+1,ch=maxY-minY+1; if(count>1200 && cw>45 && ch>45 && cw<180 && ch<180) cells.add(new Rect(minX,minY,maxX+1,maxY+1));
            }
            if(cells.size()<3) return lengths;
            ArrayList<Integer> xs=new ArrayList<>(),ys=new ArrayList<>(); for(Rect r:cells){xs.add(r.centerX());ys.add(r.centerY());}
            int tol=35; ArrayList<ArrayList<Rect>> rows=cluster(cells,true,tol), cols=cluster(cells,false,tol);
            for(ArrayList<Rect> row:rows){Collections.sort(row,(a,bx)->Integer.compare(a.left,bx.left)); addRuns(row,true,lengths);}
            for(ArrayList<Rect> col:cols){Collections.sort(col,(a,bx)->Integer.compare(a.top,bx.top)); addRuns(col,false,lengths);}
        } catch(Exception ignored){}
        return lengths;
    }

    private static ArrayList<ArrayList<Rect>> cluster(ArrayList<Rect> cells,boolean row,int tol){
        ArrayList<ArrayList<Rect>> groups=new ArrayList<>();
        for(Rect r:cells){int c=row?r.centerY():r.centerX(); ArrayList<Rect> best=null; for(ArrayList<Rect> g:groups){int avg=0;for(Rect q:g)avg+=row?q.centerY():q.centerX();avg/=g.size();if(Math.abs(c-avg)<=tol){best=g;break;}} if(best==null){best=new ArrayList<>();groups.add(best);}best.add(r);} return groups;
    }
    private static void addRuns(ArrayList<Rect> list,boolean horizontal,Set<Integer> out){ if(list.isEmpty())return; int run=1; for(int i=1;i<list.size();i++){Rect a=list.get(i-1),b=list.get(i);int gap=(horizontal?b.left-a.right:b.top-a.bottom);int size=horizontal?a.width():a.height();if(gap<Math.max(25,size*0.55))run++;else{if(run>=3)out.add(run);run=1;}}if(run>=3)out.add(run); }
    private static boolean isWhite(int c){int r=Color.red(c),g=Color.green(c),b=Color.blue(c);return r>238&&g>238&&b>238;}

    private void showResult(String header, Map<Integer,List<String>> solved){
        if(resultPanel!=null){try{wm.removeView(resultPanel);}catch(Exception ignored){}}
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(22,18,22,18);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(0xEE161616); bg.setCornerRadius(24); bg.setStroke(2,0x66A855F7); box.setBackground(bg); box.setElevation(20f);
        TextView h=new TextView(this); h.setText("✦ WORD OVERLAY  •  "+header); h.setTextColor(Color.WHITE); h.setTextSize(14); box.addView(h);
        int shown=0;
        for(Map.Entry<Integer,List<String>> e:solved.entrySet()){
            if(e.getValue().isEmpty())continue;
            TextView t=new TextView(this); StringBuilder line=new StringBuilder(e.getKey()+" letras: ");
            int lim=Math.min(12,e.getValue().size()); for(int i=0;i<lim;i++){if(i>0)line.append("  ");line.append(e.getValue().get(i));} if(e.getValue().size()>lim)line.append("  +").append(e.getValue().size()-lim);
            t.setText(line);t.setTextColor(0xFFEFEFEF);t.setTextSize(16);t.setPadding(0,8,0,0);box.addView(t);shown++;
        }
        if(shown==0){TextView t=new TextView(this);t.setText("Nenhuma palavra encontrada no dicionário local.\nToque novamente para tentar outra leitura.");t.setTextColor(0xFFFFCC80);t.setTextSize(15);t.setPadding(0,10,0,0);box.addView(t);}
        resultPanel=box;
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(Math.min(screenW-32,850),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,android.graphics.PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;lp.y=70;wm.addView(box,lp);
        handler.postDelayed(()->{if(resultPanel==box){try{wm.removeView(box);}catch(Exception ignored){}resultPanel=null;}},8500);
    }

    private Notification buildNotification(String text){return new Notification.Builder(this,"word_overlay").setContentTitle("Word Overlay").setContentText(text).setSmallIcon(android.R.drawable.ic_menu_search).setOngoing(true).build();}
    private void createNotificationChannel(){ if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel("word_overlay","Word Overlay",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(c);} }

    @Override public void onDestroy(){
        if(bubble!=null){try{wm.removeView(bubble);}catch(Exception ignored){}}
        if(resultPanel!=null){try{wm.removeView(resultPanel);}catch(Exception ignored){}}
        if(reader!=null)reader.close(); if(display!=null)display.release(); if(projection!=null)projection.stop(); if(recognizer!=null)recognizer.close(); super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
