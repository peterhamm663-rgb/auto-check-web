package py.autocheck.taller;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.content.Context;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.Uri;
import androidx.webkit.WebViewAssetLoader;
import java.io.OutputStream;

public class MainActivity extends Activity {
 private static final String ORIGIN = "https://appassets.androidplatform.net/assets/index.html";
 private static final int FILE_PICKER=21, BACKUP_SAVE=22;
 private WebView web, printWeb;
 private ValueCallback<Uri[]> fileCallback;
 private String backupPayload;
 private final Handler main = new Handler(Looper.getMainLooper());
 @Override public void onCreate(Bundle state){super.onCreate(state);
  web=new WebView(this);setContentView(web);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
  s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setJavaScriptCanOpenWindowsAutomatically(false);
  s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return loader.shouldInterceptRequest(request.getUrl());}
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !request.getUrl().toString().startsWith("https://appassets.androidplatform.net/assets/");}
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> cb,FileChooserParams params){
    if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=cb;
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(i,FILE_PICKER);return true;
   }
  });
  web.addJavascriptInterface(new Bridge(),"AutoCheckAndroid");
  web.loadUrl(ORIGIN);
 }
 private class Bridge {
  @JavascriptInterface public void printDocument(String bodyHtml){main.post(()->{
   if(printWeb!=null){printWeb.destroy();printWeb=null;}
   printWeb=new WebView(MainActivity.this);
   printWeb.getSettings().setDefaultTextEncodingName("UTF-8");
   printWeb.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String url){
    PrintManager pm=(PrintManager)getSystemService(Context.PRINT_SERVICE);
    pm.print("Auto Check - Documento interno",v.createPrintDocumentAdapter("AutoCheck"),new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
   }});
   String page="<!doctype html><html><head><meta charset='utf-8'><style>body{font-family:Arial,sans-serif;padding:18px}table{width:100%}</style></head><body>"+bodyHtml+"</body></html>";
   printWeb.loadDataWithBaseURL("https://appassets.androidplatform.net/",page,"text/html","UTF-8",null);
  });}
  @JavascriptInterface public void saveBackup(String base64Utf8){main.post(()->{
   backupPayload=base64Utf8;
   Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"autocheck-respaldo-cifrado.json");startActivityForResult(i,BACKUP_SAVE);
  });}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
  if(request==FILE_PICKER){if(fileCallback!=null){fileCallback.onReceiveValue(result==RESULT_OK&&data!=null?new Uri[]{data.getData()}:null);fileCallback=null;}}
  if(request==BACKUP_SAVE&&result==RESULT_OK&&data!=null&&backupPayload!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
   out.write(Base64.decode(backupPayload,Base64.DEFAULT));out.flush();
  }catch(Exception e){android.widget.Toast.makeText(this,"No se pudo guardar el respaldo",android.widget.Toast.LENGTH_LONG).show();}finally{backupPayload=null;}}
 }
 @Override public void onBackPressed(){web.evaluateJavascript("(function(){if(document.getElementById('printArea')?.style.display==='block'){closeDoc();return true}return false})()",v->{if(!"true".equals(v))super.onBackPressed();});}
 @Override protected void onDestroy(){if(printWeb!=null)printWeb.destroy();web.destroy();super.onDestroy();}
}
