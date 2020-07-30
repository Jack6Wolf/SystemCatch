# 为系统崩溃兜底
### 线上经常遇见很多莫名其妙的崩溃，而且很多都是偶线的
#### 诸如
- IllegalArgumentException
- DeadSystemException
- DeadObjectException
- WindowManager$BadTokenException
- ...

很多情况下，这些异常崩溃并不是由应用导致的，从堆栈中看也没有应用的相关代码信息，比如DeadObjectException，一般都是由于提供远程服务的进程挂掉导致，那如果是这种系统导致的崩溃，我们难道就无能为力了？

### 使用

- Add it in your root build.gradle at the end of repositories:

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
- Add the dependency
```
dependencies {
    implementation 'com.github.Jack6Wolf:SystemCatch:1.0.0'
}
```

- Add it in your app Application.onCreate()
```
ActivityThreadHooker.hook("com.jack.demo.MainActivity");
ActivityThreadHooker.addThrowableListener(new CatchThrowable() {
    @Override
    public void throwable(Throwable throwable) {
        Log.e("APP", "ThrowableListener:" + throwable.getMessage());
        throwable.printStackTrace();
        //由用户主动触发退出
        //exit();
    }
});
```
### 注意事项

如果存在多个进程，其他进程也需要兜底。则需要在每个进程下Hook。