package org.appwork.utils.net.httpconnection;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
//import org.appwork.utils.locale._AWU;
import org.appwork.utils.logging.Log;

public class HTTPProxy {

    public static enum TYPE {
        NONE,
        DIRECT,
        SOCKS4,
        SOCKS5,
        HTTP
    }

    public static final HTTPProxy NONE = new HTTPProxy(TYPE.NONE) {

                                           @Override
                                           public void setLocalIP(final InetAddress localIP) {
                                           }

                                           @Override
                                           public void setPass(final String pass) {
                                           }

                                           @Override
                                           public void setPort(final int port) {
                                           }

                                           @Override
                                           public void setType(final TYPE type) {
                                           }

                                           @Override
                                           public void setUser(final String user) {
                                           }

                                       };

    public static List<HTTPProxy> getFromSystemProperties() {
        final java.util.List<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        try {
            {
                /* try to parse http proxy from system properties */
                final String host = System.getProperties().getProperty("http.proxyHost");
                if (!StringUtils.isEmpty(host)) {
                    int port = 80;
                    final String ports = System.getProperty("http.proxyPort");
                    if (!StringUtils.isEmpty(ports)) {
                        port = Integer.parseInt(ports);
                    }
                    final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.HTTP, host, port);
                    final String user = System.getProperty("http.proxyUser");
                    final String pass = System.getProperty("http.proxyPassword");
                    if (!StringUtils.isEmpty(user)) {
                        pr.setUser(user);
                    }
                    if (!StringUtils.isEmpty(pass)) {
                        pr.setPass(pass);
                    }
                    ret.add(pr);
                }
            }
            {
                /* try to parse socks5 proxy from system properties */
                final String host = System.getProperties().getProperty("socksProxyHost");
                if (!StringUtils.isEmpty(host)) {
                    int port = 1080;
                    final String ports = System.getProperty("socksProxyPort");
                    if (!StringUtils.isEmpty(ports)) {
                        port = Integer.parseInt(ports);
                    }
                    final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.SOCKS5, host, port);
                    ret.add(pr);
                }
            }
        } catch (final Throwable e) {
            Log.exception(e);
        }
        return ret;
    }

    private static String[] getInfo(final String host, final String port) {
        final String[] info = new String[2];
        if (host == null) { return info; }
        final String tmphost = host.replaceFirst("http://", "").replaceFirst("https://", "");
        String tmpport = new org.appwork.utils.Regex(host, ".*?:(\\d+)").getMatch(0);
        if (tmpport != null) {
            info[1] = "" + tmpport;
        } else {
            if (port != null) {
                tmpport = new Regex(port, "(\\d+)").getMatch(0);
            }
            if (tmpport != null) {
                info[1] = "" + tmpport;
            } else {
                Log.L.severe("No proxyport defined, using default 8080");
                info[1] = "8080";
            }
        }
        info[0] = new Regex(tmphost, "(.*?)(:|/|$)").getMatch(0);
        return info;
    }

    private InetAddress localIP          = null;

    private String      user             = null;

    private String      pass             = null;
    private int         port             = 80;

    protected String    host             = null;

    private TYPE        type             = TYPE.DIRECT;

    private boolean     useConnectMethod = false;

    protected HTTPProxy() {
    }

    public HTTPProxy(final InetAddress direct) {
        this.type = TYPE.DIRECT;
        this.localIP = direct;
    }

    public HTTPProxy(final TYPE type) {
        this.type = type;
    }

    public HTTPProxy(final TYPE type, final String host, final int port) {
        this.port = port;
        this.type = type;
        this.host = HTTPProxy.getInfo(host, "" + port)[0];
    }

    protected void cloneProxy(final HTTPProxy proxy) {
        if (proxy == null) { return; }
        this.user = proxy.user;
        this.host = proxy.host;
        this.localIP = proxy.localIP;
        this.pass = proxy.pass;
        this.port = proxy.port;
        this.type = proxy.type;
        this.localIP = proxy.localIP;
        this.useConnectMethod = proxy.useConnectMethod;
    }

    public String getHost() {
        return this.host;
    }

    /**
     * @return the localIP
     */
    public InetAddress getLocalIP() {
        return this.localIP;
    }

    public String getPass() {
        return this.pass;
    }

    public int getPort() {
        return this.port;
    }

    public TYPE getType() {
        return this.type;
    }

    public String getUser() {
        return this.user;
    }

    /**
     * this proxy is DIRECT = using a local bound IP
     * 
     * @return
     */
    public boolean isDirect() {
        return this.type == TYPE.DIRECT;
    }

    public boolean isConnectMethodPrefered() {
        return this.useConnectMethod;
    }

    public boolean isLocal() {
        return this.isDirect() || this.isNone();
    }

    /**
     * this proxy is NONE = uses default gateway
     * 
     * @return
     */
    public boolean isNone() {
        return this.type == TYPE.NONE;
    }

    /**
     * this proxy is REMOTE = using http,socks proxy
     * 
     * @return
     */
    public boolean isRemote() {
        return !this.isDirect() && !this.isNone();
    }

    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * @param localIP
     *            the localIP to set
     */
    public void setLocalIP(final InetAddress localIP) {
        this.localIP = localIP;
    }

    public void setPass(final String pass) {
        this.pass = pass;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setType(final TYPE type) {
        this.type = type;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    @Override
    public String toString() {
//        if (this.type == TYPE.NONE) {
//            return _AWU.T.proxy_none();
//        } else if (this.type == TYPE.DIRECT) {
//            return _AWU.T.proxy_direct(this.localIP.getHostAddress());
//        } else if (this.type == TYPE.HTTP) {
//            return _AWU.T.proxy_http(this.getHost(), this.getPort());
//        } else if (this.type == TYPE.SOCKS5) {
//            return _AWU.T.proxy_socks5(this.getHost(), this.getPort());
//        } else if (this.type == TYPE.SOCKS4) {
//            return _AWU.T.proxy_socks4(this.getHost(), this.getPort());
//        } else {
            return "UNKNOWN";
//        }
    }
}
