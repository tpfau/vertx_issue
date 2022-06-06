package com.example.starter;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.ext.web.client.spi.CookieStore;

/**
 * @author <a href="mailto:tommaso.nolli@gmail.com">Tommaso Nolli</a>
 */
public class DebugCookieStore implements CookieStore {

  private final ConcurrentHashMap<Key, Cookie> noDomainCookies;
  private final ConcurrentSkipListMap<Key, Cookie> domainCookies;

  public DebugCookieStore() {
    noDomainCookies = new ConcurrentHashMap<>();
    domainCookies = new ConcurrentSkipListMap<>();
  }

  @Override
  public Iterable<Cookie> get(Boolean ssl, String domain, String path) {
    assert domain != null && domain.length() > 0;
    System.out.println("Got a request for a cookie" + ssl + "/" + domain + "/" + path);
    String cleanPath;
    {
      String uri = HttpUtils.removeDots(path);
      // Remoe query params if present
      int pos = uri.indexOf('?');
      if (pos > -1) {
        uri = uri.substring(0, pos);
      }

      // Remoe frament identifier if present
      pos = uri.indexOf('#');
      if (pos > -1) {
        uri = uri.substring(0, pos);
      }
      cleanPath = uri;
    }

    TreeMap<String, Cookie> matches = new TreeMap<>();

    Consumer<Cookie> adder = c -> {
      if (ssl != Boolean.TRUE && c.isSecure()) {
        System.out.println("Consumer is secure, but ssl is unsecure");
        return;
      }
      if (c.path() != null && !cleanPath.equals(c.path())) {
        String cookiePath = c.path();
        if (!cookiePath.endsWith("/")) {
          cookiePath += '/';
        }
        if (!cleanPath.startsWith(cookiePath)) {
          System.out.println("Clean path did not match");
          return;
        }
      }
      System.out.println("Found Match, adding " + c);
      matches.put(c.name(), c);
    };
    
    for (Cookie c : noDomainCookies.values()) {
      System.out.println("Accepting: " + c);
      adder.accept(c);
    }

    Key key = new Key(domain, "", "");
    String prefix = key.domain.substring(0, 1);
    for (Entry<Key, Cookie> entry : domainCookies.tailMap(new Key(prefix, "", ""), true).entrySet()) {
      if (entry.getKey().domain.compareTo(key.domain) > 0) {
        System.out.println("Domains didn't match1:" + entry.getKey().domain + " / " + key.domain );
        break;
      }
      if (!key.domain.startsWith(entry.getKey().domain)) {
        System.out.println("Domains didn't match2:" + entry.getKey().domain + " / " + key.domain );
        continue;
      }
      System.out.println("Accepting Cookie:" + key );
      adder.accept(entry.getValue());
    }
    System.out.println(matches.values() );
    return matches.values();
  }

  @Override
  public CookieStore put(Cookie cookie) {
	  System.out.println("Got a new Cookie: " +  cookie.name() + " / " +  cookie.domain() + " / " +  cookie.value() + " / " +  cookie.isSecure() );
    Key key = new Key(cookie.domain(), cookie.path(), cookie.name());
    if (key.domain.equals(Key.NO_DOMAIN)) {
      System.out.println("Cookie had no domain. Adding it.");
      noDomainCookies.put(key, cookie);
      return this;
    }
    System.out.println("Cookie had a domain. Adding it.");
    domainCookies.put(key, cookie);
    return this;
  }

  @Override
  public CookieStore remove(Cookie cookie) {
    Key key = new Key(cookie.domain(), cookie.path(), cookie.name());
    if (key.domain.equals(Key.NO_DOMAIN)) {
      noDomainCookies.remove(key);
    } else {
      domainCookies.remove(key);
    }
    return this;
  }

  private static class Key implements Comparable<Key> {
    private static final String NO_DOMAIN = "";

    private final String domain;
    private final String path;
    private final String name;

    public Key(String domain, String path, String name) {
      if (domain == null || domain.length() == 0) {
        this.domain = NO_DOMAIN;
      } else {
        while (domain.charAt(0) == '.') {
          domain = domain.substring(1);
        }
        while (domain.charAt(domain.length() - 1) == '.') {
          domain = domain.substring(0, domain.length() - 1);
        }
        String[] tokens = domain.split("\\.");
        String tmp;
        for (int i = 0, j = tokens.length - 1; i < tokens.length / 2; ++i, --j) {
          tmp = tokens[j];
          tokens[j] = tokens[i];
          tokens[i] = tmp;
        }
        this.domain = String.join(".", tokens);
      }
      this.path = path == null ? "" : path;
      this.name = name;
    }

    @Override
    public int compareTo(Key o) {
      int ret = domain.compareTo(o.domain);
      if (ret == 0)
        ret = path.compareTo(o.path);
      if (ret == 0)
        ret = name.compareTo(o.name);
      return ret;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((domain == null) ? 0 : domain.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((path == null) ? 0 : path.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Key other = (Key) obj;
      if (domain == null) {
        if (other.domain != null)
          return false;
      } else if (!domain.equals(other.domain))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      if (path == null) {
        return other.path == null;
      } else return path.equals(other.path);
    }
  }
}
