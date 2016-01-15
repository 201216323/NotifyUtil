# 判断App是否处于前台的方法

按需求弹出相应的Notification，当用户点击Notification的时候，也要做出相应的处理。我们的App有可能正在前台运行，也有可能在后台运行，这时，我们在处理点击事件的时候不得不做出判断，App到底运行在前台还是在后台呢？看样子貌似很简单，觉得Android SDK应该会提供相应的API供开发者调用，结果是并没有，我们不得不通过其他手段来获取App当前的状态。


方法一：通过RunningTask
-----
当一个App处于前台的时候，会处于RunningTask的这个栈的栈顶，所以我们可以取出RunningTask的栈顶的任务进程，看他与我们的想要判断的App的包名是否相同，来达到效果
``` java 
    /**
     * 方法1：通过getRunningTasks判断App是否位于前台，此方法在5.0以上失效
     *
     * @param context     上下文参数
     * @param packageName 需要检查是否位于栈顶的App的包名
     * @return
     */
    public static boolean getRunningTask(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        return !TextUtils.isEmpty(packageName) && packageName.equals(context.getPackageName());
    }
```

**缺点**
getRunningTasks这个方法在5.0 Lollipop之后就不在对第三方App可用了，因为这样会泄漏用户的信息，为了向后兼容，这个方法只返回一小部分调用者本身的task和其他一些不太敏感的task。

**测试**


下面是在小米的机子上打印的结果，Android的版本是Android4.4.4的，我们是可以拿到全部的正在运行的应用的信息的。其中包名为com.whee.wheetalklollipop的就是我们需要判断是否处于前台的App，如今他位于第一个，说明是处于前台的
![Alt text](./小米打印.PNG)

下面是Android5.0上打印的结果，我们虽然打开了很多诸如新浪微博，网易新闻，QQ子类的App，可是打印出来后却完全看不到了，只有自身的App的信息和一些系统进程的信息，这说明Android5.0的确是做了这么一重限制了，只返回一小部分调用者本身的task和其他一些不太敏感的task。
![Alt text](./2.PNG)

查看具体的说明，到底会除了自身的App的包名，还会返回什么给我们，发现有这么一句话，“possibly some other tasks
     * such as home that are known to not be sensitive.，说明有可能并不会返回一些不敏感的信息，这样就存在只有自身的App的包名的情况，那么即使退回到后台，他仍然会被判断为前台，此方法仍然是有缺陷的
”
``` java
@deprecated As of {@link android.os.Build.VERSION_CODES#LOLLIPOP}, this method
     * is no longer available to third party
     * applications: the introduction of document-centric recents means
     * it can leak person information to the caller.  For backwards compatibility,
     * it will still retu rn a small subset of its data: at least the caller's
     * own tasks, and possibly some other tasks
     * such as home that are known to not be sensitive.
```


方法二：通过RunningProcess
-----
通过runningProcess获取到一个当前正在运行的进程的List，我们遍历这个List中的每一个进程，判断这个进程的一个importance 属性是否是前台进程，并且包名是否与我们判断的APp的包名一样，如果这两个条件都符合，那么这个App就处于前台
``` java
   /**
     * 方法2：通过getRunningAppProcesses的IMPORTANCE_FOREGROUND属性判断是否位于前台，当service需要常驻后台时候，此方法失效,
     * 在小米 Note上此方法无效，在Nexus上正常
     *
     * @param context     上下文参数
     * @param packageName 需要检查是否位于栈顶的App的包名
     * @return
     */
    public static boolean getRunningAppProcesses(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
```

**缺点**：
在聊天类型的App中，常常需要常驻后台来不间断的获取服务器的消息，这就需要我们把Service设置成START_STICKY，kill 后会被重启（等待5秒左右）来保证Service常驻后台。如果Service设置了这个属性，这个App的进程就会被判断是前台，代码上的表现就是appProcess.importance的值永远是 ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND，这样就永远无法判断出到底哪个是前台了。




方法三：通过ActivityLifecycleCallbacks
------
AndroidSDK14在Application类里增加了ActivityLifecycleCallbacks，我们可以通过这个Callback拿到App所有Activity的生命周期回调。
``` java
 public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle savedInstanceState);
        void onActivityStarted(Activity activity);
        void onActivityResumed(Activity activity);
        void onActivityPaused(Activity activity);
        void onActivityStopped(Activity activity);
        void onActivitySaveInstanceState(Activity activity, Bundle outState);
        void onActivityDestroyed(Activity activity);
    }
```
知道这些信息，我们就可以用更官方的办法来解决问题，当然还是利用方案二里的Activity生命周期的特性，我们只需要在Application的onCreat（）里去注册上述接口，然后由Activity回调回来运行状态即可。代码如下：

``` java
public class MyApplication extends Application {
    private int appCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log.d("wenming", "onActivityCreated");
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log.d("wenming", "onActivityStarted");
                appCount++;
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log.d("wenming", "onActivityResumed");

            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log.d("wenming", "onActivityPaused");
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log.d("wenming", "onActivityStopped");
                appCount--;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                Log.d("wenming", "onActivitySaveInstanceState");
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.d("wenming", "onActivityDestroyed");
            }
        });
    }

    public int getAppCount() {
        return appCount;
    }

    public void setAppCount(int appCount) {
        this.appCount = appCount;
    }
}
```

在需要的判断的地方调用以下代码即可：

``` java
 /**
     * 方法3：通过ActivityLifecycleCallbacks来批量统计Activity的生命周期，来做判断，此方法在API 14以上均有效，但是需要在Application中注册此回调接口
     * 必须：
     * 1. 自定义Application并且注册ActivityLifecycleCallbacks接口
     * 2. AndroidManifest.xml中更改默认的Application为自定义
     * 3. 当Application因为内存不足而被Kill掉时，这个方法仍然能正常使用。虽然全局变量的值会因此丢失，但是再次进入App时候会重新统计一次的
     */

    public static boolean getApplicationValue(Context context) {
        return ((MyApplication) ((Service) context).getApplication()).getAppCount() > 0;
    }
```
不管以哪种方式，只要捕捉到APP切到后台的动作，就可以做你需要的事件处理了，其实还是一个比较常见的需求，比如通讯类APP切到后台的时候消息以notification的形式push过来，比如比较私密一点的APP切到后台的时候再次切回来要先输入手势密码等等。


可能还有人在纠结，我用back键切到后台和用Home键切到后台，一样吗？上述方法都适用吗？在Android应用开发中一般认为back键是可以捕获的，而Home键是不能捕获的（除非修改framework）,但是上述方法从Activity生命周期着手解决问题，虽然这两种方式的Activity生命周期并不相同，但是二者都会执行onStop（）；所以并不关心到底是触发了哪个键切入后台的。

方法四:通过使用UsageStatsManager获取
-----
通过使用UsageStatsManager获取，此方法是Android5.0之后提供的新API，可以获取一个时间段内的应用统计信息，但是必须满足一下要求

  必须：
  1. 此方法只在android5.0以上有效 
  2. AndroidManifest中加入此权限<uses-permission xmlns:tools="http://schemas.android.com/tools" android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions" />
  3. 打开手机设置，点击安全-高级，在有权查看使用情况的应用中，为这个App打上勾

![Alt text](./权限.PNG)


``` java

 @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean queryUsageStats(Context context, String packageName) {
        class RecentUseComparator implements Comparator<UsageStats> {
            @Override
            public int compare(UsageStats lhs, UsageStats rhs) {
                return (lhs.getLastTimeUsed() > rhs.getLastTimeUsed()) ? -1 : (lhs.getLastTimeUsed() == rhs.getLastTimeUsed()) ? 0 : 1;
            }
        }
        RecentUseComparator mRecentComp = new RecentUseComparator();
        long ts = System.currentTimeMillis();
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) context.getSystemService("usagestats");
        List<UsageStats> usageStats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, ts - 1000 * 10, ts);
        if (usageStats == null || usageStats.size() == 0) {
            if (HavaPermissionForTest(context) == false) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Toast.makeText(context, "权限不够\n请打开手机设置，点击安全-高级，在有权查看使用情况的应用中，为这个App打上勾", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        Collections.sort(usageStats, mRecentComp);
        String currentTopPackage = usageStats.get(0).getPackageName();
        if (currentTopPackage.equals(packageName)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否拥有PACKAGE_USAGE_STATS权限
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean HavaPermissionForTest(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

``` 












