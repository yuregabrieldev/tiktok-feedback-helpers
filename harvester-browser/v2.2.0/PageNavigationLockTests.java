import com.harvester.browser.core.PageNavigationLock;

public final class PageNavigationLockTests {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        String fixed="https://example.com/video?id=1";
        check(!PageNavigationLock.blocks(null,"https://other.com"),"disabled lock");
        check(!PageNavigationLock.blocks(fixed,fixed),"same-page");
        check(!PageNavigationLock.blocks(fixed,fixed+"#player"),"anchor allowed");
        check(PageNavigationLock.blocks(fixed,"https://example.com/video?id=2"),"query change blocked");
        check(PageNavigationLock.blocks(fixed,"https://example.com/other"),"same-host change blocked");
        check(PageNavigationLock.blocks(fixed,"https://ads.example/landing"),"new domain blocked");
        check(PageNavigationLock.blocks(fixed,"http://example.com/video?id=1"),"scheme change blocked");
        check(PageNavigationLock.blocks(fixed,"https://example.com/video?id=1/"),"path change blocked");
        check(PageNavigationLock.blocks(fixed,"about:blank"),"unrelated scheme blocked");
        System.out.println("PageNavigationLockTests passed");
    }
}
