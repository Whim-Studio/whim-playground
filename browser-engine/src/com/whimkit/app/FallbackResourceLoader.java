package com.whimkit.app;

import com.whimkit.net.ResourceLoader;
import com.whimkit.net.WebResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A minimal HTTP/about/data loader used <em>only</em> when the full
 * {@code com.whimkit.net.http.HttpResourceLoader} subsystem is absent. Enough to
 * fetch a page; the real loader (cookies, cache, gzip, redirects) supersedes it
 * via {@link Subsystems}.
 */
final class FallbackResourceLoader implements ResourceLoader {

    @Override
    public WebResponse load(String url) {
        if (url == null) return WebResponse.failure("", "null url");
        try {
            if (url.startsWith("about:")) {
                byte[] b = ("<html><body><h1>" + url + "</h1></body></html>").getBytes("UTF-8");
                return new WebResponse(url, 200, empty(), "text/html", Charset.forName("UTF-8"), b, null);
            }
            if (url.startsWith("data:")) {
                int comma = url.indexOf(',');
                String meta = url.substring(5, comma < 0 ? url.length() : comma);
                String data = comma < 0 ? "" : url.substring(comma + 1);
                byte[] body = meta.contains("base64")
                        ? java.util.Base64.getMimeDecoder().decode(data)
                        : java.net.URLDecoder.decode(data, "UTF-8").getBytes("UTF-8");
                String ct = meta.split(";")[0];
                if (ct.isEmpty()) ct = "text/plain";
                return new WebResponse(url, 200, empty(), ct, Charset.forName("UTF-8"), body, null);
            }
            URL u = new URL(url);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", "WhimKit/0.1 (Java)");
            c.connect();
            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            byte[] body = in == null ? new byte[0] : readAll(in);
            String ct = c.getContentType();
            String charset = "UTF-8";
            if (ct != null && ct.contains("charset=")) {
                charset = ct.substring(ct.indexOf("charset=") + 8).trim();
            }
            String type = ct == null ? "text/html" : ct.split(";")[0];
            return new WebResponse(c.getURL().toString(), code, empty(), type,
                    safeCharset(charset), body, null);
        } catch (Exception e) {
            return WebResponse.failure(url, e.toString());
        }
    }

    private static Charset safeCharset(String name) {
        try { return Charset.forName(name); } catch (Exception e) { return Charset.forName("UTF-8"); }
    }

    private static Map<String, List<String>> empty() {
        return new TreeMap<String, List<String>>();
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        in.close();
        return out.toByteArray();
    }
}
