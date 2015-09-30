package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.appwork.utils.net.httpconnection.HTTPConnectionImpl;
import org.appwork.utils.net.httpconnection.HTTPProxy;

public class URLConnectionAdapterDirectImpl extends HTTPConnectionImpl implements URLConnectionAdapter {

    private Request request;

    public URLConnectionAdapterDirectImpl(final URL url) {
        super(url);
    }

    public URLConnectionAdapterDirectImpl(final URL url, final HTTPProxy proxy) {
        super(url, proxy);
    }

    @Override
    public InputStream getErrorStream() {
        try {
            return super.getInputStream();
        } catch (final IOException e) {
            return null;
        }
    }

    @Override
    public long getLongContentLength() {
        return this.getContentLength();
    }

    @Override
    public Request getRequest() {
        return this.request;
    }

    @Override
    public void setRequest(final Request request) {
        this.request = request;
    }
}
