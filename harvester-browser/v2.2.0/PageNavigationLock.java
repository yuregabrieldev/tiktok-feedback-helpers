package com.harvester.browser.core;

/** Pure function used to decide whether a top-level WebView navigation leaves a pinned document. */
public final class PageNavigationLock {
    private PageNavigationLock() {}
    public static boolean blocks(String pinnedUrl,String destinationUrl) {
        if(pinnedUrl==null || pinnedUrl.trim().isEmpty())return false;
        if(destinationUrl==null || destinationUrl.trim().isEmpty())return true;
        return !withoutFragment(pinnedUrl).equals(withoutFragment(destinationUrl));
    }
    public static String withoutFragment(String url) {
        if(url==null)return "";
        int i=url.indexOf('#');
        return i>=0 ? url.substring(0,i) : url;
    }
}
