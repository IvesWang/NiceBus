### NiceBus优点
1. 使用String作为事件名，不以方法参数类型作为事件名
2. 支持向订阅的方法传入一定数量的参数
3. 支持跨进程（待实现）
4. 自动代码注入，减少机械代码和内存泄露可能（待实现）
### 集成步骤
###### 1. 添加依赖
在根目录的build.gradle文件添加以下代码：
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

添加以下依赖：
```
dependencies {
	implementation 'com.github.IvesWang:nicebus:0.9-alpha1'
}
```

###### 2. 注册监听类
将你想要接收事件的类实例注册到NiceBus，一般在activity的onCreate或fragment的onViewCreated方法里添加：</br>
```
比如想在当前类里添加接收事件的方法
NiceBus.getDefault().register(this)
```
记得在对应的activity或fragment声明周期结束时调用unregister()方法解除注册</br>


###### 3. 订阅需要接收事件的方法
注解举例：</br>

```
@NiceEvent(threadMode = ThreadMode.ASYNC, events = {"event1", "event2"})
private void listenHere(String event,int age) {
.....
}
```
这样会订阅"event1""event2"这两个事件</br>
<b>方法的第一个参数必须提供，且是String类型。</b>


###### 4. 发送事件
```
NiceBus.getDefault().post("event1");
也可以发送带参数的事件：
NiceBus.getDefault().post("event1", 3);
这样订阅者方法的参数列表里有对应参数申明则会收到3，没有该参数的收到的事件和不带参数一样。
但如果如例一不带参数，则带参数的订阅者方法会收到一个默认值。（后期版本会添加必须有参才接收的设置）
```


###### 解除注册
在监听对象生命周期结束的时候，需要手动解除注册，以免内存泄露。
```
NiceBus.getDefault().unregister(this)
```

License
-------
    Copyright (C) 2021 wangziguang && iveswang

    Licensed under the Apache License, Version 2.0 (the "License");
    You can know a detail description at http://www.apache.org/licenses/LICENSE-2.0

