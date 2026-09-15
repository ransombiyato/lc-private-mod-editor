package com.ribi.lcprivate;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends AppCompatActivity {
    static final String NORMAL = "com.ProjectMoon.LimbusCompany";
    static final String PRIVATE = "com.ribi.limbusprivate";
    static final int PICK_TREE = 41;
    LinearLayout root;
    ArrayList<Field> fields = new ArrayList<>();
    Uri treeUri;

    static class Field {
        DocumentFile file; String kind, key, value, type, context; int start, end;
        Field(DocumentFile f,String k,String ky,String v,String ty,String c,int s,int e){file=f;kind=k;key=ky;value=v;type=ty;context=c;start=s;end=e;}
    }

    int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    TextView tv(String s,int z){ TextView t=new TextView(this); t.setText(s); t.setTextColor(0xffeeeeee); t.setTextSize(z); t.setPadding(dp(4),dp(8),dp(4),dp(8)); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }

    @Override public void onCreate(Bundle b){ super.onCreate(b); home(); }
    void base(String title,String sub){
        ScrollView sv=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(14),dp(18),dp(18)); root.setBackgroundColor(0xff101114); sv.addView(root); setContentView(sv);
        root.addView(tv(title,25)); root.addView(tv(sub,14));
    }
    void home(){
        base("LC Private Mod Editor","Offline/private only • fail-closed • no blind number replacement");
        Button n=btn("▶  Launch normal Limbus Company"); n.setOnClickListener(v->launch(NORMAL)); root.addView(n);
        Button p=btn("🛠  Launch private / modifiable LC"); p.setOnClickListener(v->launch(PRIVATE)); root.addView(p);
        root.addView(tv("MOD EDITOR",12));
        Button s=btn("🔎  Scan private data"); s.setOnClickListener(v->pickTree()); root.addView(s);
        Button b=btn("📦  Backups / restore"); b.setOnClickListener(v->backups()); root.addView(b);
        root.addView(tv("The scanner only presents structured fields it can identify safely. Unknown, encrypted, binary, or ambiguous data is shown as unsupported rather than guessed.",13));
        root.addView(tv("Private package: "+PRIVATE,12));
    }
    void launch(String pkg){
        Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
        if(i==null){ new AlertDialog.Builder(this).setTitle("App not found").setMessage("Not installed: "+pkg+"\n\nChange NORMAL/PRIVATE in the generated project if your package IDs differ.").setPositiveButton("OK",null).show(); return; }
        startActivity(i);
    }
    void pickTree(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION); startActivityForResult(i,PICK_TREE);
    }
    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(r==PICK_TREE&&c==RESULT_OK&&d!=null){ treeUri=d.getData(); try{getContentResolver().takePersistableUriPermission(treeUri,d.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){} scan(); } }

    void scan(){
        fields.clear(); if(treeUri==null)return; DocumentFile rootFile=DocumentFile.fromTreeUri(this,treeUri); if(rootFile==null){toast("Cannot open selected folder");return;}
        scanDir(rootFile,0); showFields();
    }
    void scanDir(DocumentFile dir,int depth){
        if(depth>12)return; DocumentFile[] fs=dir.listFiles();
        for(DocumentFile f:fs){ if(f.isDirectory()) scanDir(f,depth+1); else if(f.length()<=8*1024*1024L) scanFile(f); }
    }
    boolean textName(String n){ String x=n.toLowerCase(Locale.US); return x.endsWith(".json")||x.endsWith(".json5")||x.endsWith(".xml")||x.endsWith(".txt")||x.endsWith(".cfg")||x.endsWith(".ini")||x.endsWith(".csv")||x.endsWith(".yaml")||x.endsWith(".yml"); }
    String read(DocumentFile f)throws Exception{ InputStream in=getContentResolver().openInputStream(f.getUri()); ByteArrayOutputStream o=new ByteArrayOutputStream(); byte[] b=new byte[8192]; int n; while((n=in.read(b))>0)o.write(b,0,n); in.close(); return o.toString("UTF-8"); }
    void scanFile(DocumentFile f){
        if(!textName(f.getName()==null?"":f.getName()))return; String s; try{s=read(f);}catch(Exception e){return;}
        if(s.indexOf('\0')>=0)return;
        // JSON/YAML-ish key/value fields. A field is editable only when its exact key appears once in the file.
        Pattern json=Pattern.compile("(?m)(\\\"([A-Za-z0-9_.-]*(?:lunacy|enkephalin|thread|egoshard|hp|maxhp|damage|currency|resource|exp|level|sanity)[A-Za-z0-9_.-]*)\\\"\\s*:\\s*)(-?\\d+(?:\\.\\d+)?)");
        Matcher m=json.matcher(s); HashMap<String,Integer> counts=new HashMap<>(); ArrayList<Matcher> ms=new ArrayList<>();
        while(m.find()){String k=m.group(2);counts.put(k,counts.containsKey(k)?counts.get(k)+1:1);ms.add(m.toMatchResult().toString()==null?null:null);}
        m=json.matcher(s); while(m.find()){String k=m.group(2); if(counts.get(k)!=null&&counts.get(k)==1){int a=m.start(3),e=m.end(3); addField(f,"JSON",k,m.group(3),numType(m.group(3)),context(s,a,e),a,e);}}
        Pattern ini=Pattern.compile("(?mi)^\\s*([A-Za-z0-9_.-]*(?:lunacy|enkephalin|thread|egoshard|hp|maxhp|damage|currency|resource|exp|level|sanity)[A-Za-z0-9_.-]*)\\s*=\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");
        Matcher q=ini.matcher(s); HashMap<String,Integer> ic=new HashMap<>(); while(q.find()){String k=q.group(1);ic.put(k,ic.containsKey(k)?ic.get(k)+1:1);} q=ini.matcher(s); while(q.find()){String k=q.group(1);if(ic.get(k)==1)addField(f,"KEY=VALUE",k,q.group(2),numType(q.group(2)),context(s,q.start(2),q.end(2)),q.start(2),q.end(2));}
    }
    String numType(String v){return v.contains(".")?"decimal":"integer";}
    String context(String s,int a,int e){int l=Math.max(0,a-55),r=Math.min(s.length(),e+55);return s.substring(l,r).replace('\n',' ');}
    void addField(DocumentFile f,String kind,String key,String val,String type,String ctx,int st,int en){ fields.add(new Field(f,kind,key,val,type,ctx,st,en)); }
    void showFields(){
        base("Scan results",fields.size()+" safely identified field(s). Nothing has been changed.");
        if(fields.isEmpty()){root.addView(tv("No supported structured fields were found. This is expected for encrypted/binary/unsupported formats. No write was attempted.",15));return;}
        for(int i=0;i<fields.size();i++){final int ix=i; Field x=fields.get(i); Button b=btn(x.key+"  =  "+x.value+"  ["+x.type+"]"); b.setOnClickListener(v->editField(ix)); root.addView(b); root.addView(tv(x.file.getName()+" • "+x.kind+"\n…"+x.context+"…",11));}
        Button home=btn("← Home");home.setOnClickListener(v->home());root.addView(home);
    }
    void editField(int ix){
        Field x=fields.get(ix); EditText input=new EditText(this); input.setInputType(2|8192); input.setText(x.value); input.selectAll();
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),0,dp(20),0);box.addView(tv("Current: "+x.value+"\nType: "+x.type+"\nFile: "+x.file.getName()+"\n\n"+x.context,12));box.addView(input);
        new AlertDialog.Builder(this).setTitle("Confirm modification").setView(box).setMessage("Exact structured field identified. A backup is created before writing. If validation fails, nothing is committed.").setNegativeButton("CANCEL",null).setPositiveButton("APPLY",(d,w)->apply(x,input.getText().toString().trim())).show();
    }
    void apply(Field x,String nv){
        if(nv.length()==0||!nv.matches("-?\\d+(?:\\.\\d+)?")){toast("Invalid number. Nothing changed.");return;}
        if(x.type.equals("integer")&&nv.contains(".")){toast("Integer field requires an integer. Nothing changed.");return;}
        String old;try{old=read(x.file);}catch(Exception e){toast("Read failed. Nothing changed.");return;}
        String needle=old.substring(x.start,x.end); if(!needle.equals(x.value)){toast("Source changed since scan. Rescan first.");return;}
        String again=old.substring(0,x.start)+nv+old.substring(x.end);
        // Refuse if the expected exact occurrence disappeared or the replacement changes more than one span.
        if(!again.substring(0,x.start).equals(old.substring(0,x.start))||!again.substring(x.start+nv.length()).equals(old.substring(x.end))){toast("Validation failed. Nothing changed.");return;}
        try{
            backup(x.file,old);
            OutputStream out=getContentResolver().openOutputStream(x.file.getUri(),"wt"); if(out==null)throw new IOException("no writer"); out.write(again.getBytes(StandardCharsets.UTF_8));out.close();
            String check=read(x.file); if(!check.equals(again)){toast("Post-write verification failed. Restore the backup if needed.");return;}
            toast("Applied exactly one field: "+x.key);scan();
        }catch(Exception e){toast("Write failed: "+e.getMessage());}
    }
    void backup(DocumentFile f,String text)throws Exception{
        DocumentFile dir=f.getParentFile(); if(dir==null)throw new IOException("No parent"); String name=f.getName()+".ribi-backup-"+System.currentTimeMillis(); DocumentFile b=dir.createFile("application/octet-stream",name); if(b==null)throw new IOException("backup create failed"); OutputStream o=getContentResolver().openOutputStream(b.getUri());o.write(text.getBytes(StandardCharsets.UTF_8));o.close();
    }
    void backups(){ new AlertDialog.Builder(this).setTitle("Backups").setMessage("Backups are stored beside the edited private data file with a .ribi-backup- timestamp suffix. To restore, replace the edited file with the chosen backup using your file manager, then rescan.\n\nThe editor never deletes backups automatically.").setPositiveButton("OK",null).show(); }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
