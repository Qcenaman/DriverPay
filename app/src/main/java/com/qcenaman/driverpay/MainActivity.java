package com.qcenaman.driverpay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;
import android.webkit.JsPromptResult;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQ_OPEN = 1;
    private static final int REQ_SAVE = 2;

    private WebView web;
    private ValueCallback<Uri[]> pendingChooser;
    private String pendingContent;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true); // the app keeps everything in localStorage
        s.setAllowFileAccess(true);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return !r.getUrl().toString().startsWith("file:///android_asset/");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView v, String url, String msg, JsResult res) {
                new AlertDialog.Builder(MainActivity.this).setMessage(msg)
                        .setPositiveButton(android.R.string.ok, (d, w) -> res.confirm())
                        .setOnCancelListener(d -> res.confirm()).show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView v, String url, String msg, JsResult res) {
                new AlertDialog.Builder(MainActivity.this).setMessage(msg)
                        .setPositiveButton(android.R.string.ok, (d, w) -> res.confirm())
                        .setNegativeButton(android.R.string.cancel, (d, w) -> res.cancel())
                        .setOnCancelListener(d -> res.cancel()).show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView v, String url, String msg, String def, JsPromptResult res) {
                EditText input = new EditText(MainActivity.this);
                input.setText(def);
                new AlertDialog.Builder(MainActivity.this).setMessage(msg).setView(input)
                        .setPositiveButton(android.R.string.ok, (d, w) -> res.confirm(input.getText().toString()))
                        .setNegativeButton(android.R.string.cancel, (d, w) -> res.cancel())
                        .setOnCancelListener(d -> res.cancel()).show();
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (pendingChooser != null) pendingChooser.onReceiveValue(null);
                pendingChooser = cb;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(i, REQ_OPEN);
                } catch (Exception e) {
                    pendingChooser = null;
                    return false;
                }
                return true;
            }
        });

        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int req, int code, Intent data) {
        super.onActivityResult(req, code, data);
        Uri uri = (code == RESULT_OK && data != null) ? data.getData() : null;
        if (req == REQ_OPEN) {
            if (pendingChooser != null) pendingChooser.onReceiveValue(uri == null ? null : new Uri[]{uri});
            pendingChooser = null;
        } else if (req == REQ_SAVE) {
            String content = pendingContent;
            pendingContent = null;
            if (uri == null || content == null) return;
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.save_failed) + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class Bridge {
        @JavascriptInterface
        public void saveFile(String name, String mime, String content) {
            runOnUiThread(() -> {
                pendingContent = content;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType(FileNames.mimeFor(name, mime));
                i.putExtra(Intent.EXTRA_TITLE, FileNames.sanitize(name));
                startActivityForResult(i, REQ_SAVE);
            });
        }
    }
}
