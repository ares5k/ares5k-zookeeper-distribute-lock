# ares5k-zookeeper-distribute-lock
ares5k-zookeeper-distribute-lock是基于 curator实现的 zookeeper分布式锁 Aop注解组件

互斥锁: 公平锁, 已规避羊群效应, 同一线程同一节点可重入 

写锁: 公平锁, 同一线程同一节点写和读可重入 , 不同线程或不同节点的写和读阻塞,  写锁释放时, 所有读锁全部唤醒

读锁: 公平锁, 都是读锁的情况下非阻塞。与写锁互斥

# **联系方式**
QQ: 16891544<br/>
邮箱: 16891544@qq.com

# **说明**
**1.项目只是为了满足作者实际需求所写, 所以并非完美, 进行过压测实验, 但没有在超高并发下使用过, 希望可以给想用注解实现分布式锁的朋友提供个思路**<br/>

**2.下载后修改Curator类的zookeeper服务器地址, 然后 mvn clean package，就可以在其他项目里引用了。**<br/>



# **使用**案例

 互斥锁-未获取到锁时无限等待直到获取锁成功-同一线程同一节点不可重入

```java
@Lock(bizPath = "/product", waitTime = -1, reentrant = false)
public void mutex() throws InterruptedException {
    System.out.println("获取互斥锁");
    Thread.sleep(Long.MAX_VALUE);
}
```



互斥锁- 未获取到锁时无限等待直到获取锁成功-同一线程同一节点可重入

```java
@Lock(bizPath = "/product", waitTime = -1)
public void mutex(boolean over) throws InterruptedException {
    System.out.println("获取互斥锁");
    if (over) {
        return;
    }
    ((Test)AopContext.currentProxy()).mutex(true);
    Thread.sleep(Long.MAX_VALUE);
}
```



互斥锁-等待5秒-5秒后仍为获取锁时的处理

```java
public void errorProcess() throws InterruptedException {
    try{
        //调用需要分布式锁的方法
        ((Test)AopContext.currentProxy()).mutex();
    }catch (LockException e){
        if (e.getLockErrorCode() == LockException.LockErrorCode.ERROR_IN_TIME) {
            System.out.println("获取锁超时了");
        }
    }
}
@Lock(bizPath = "/product", waitTime = 5000)
public void mutex() throws InterruptedException, LockException {
        System.out.println("获取互斥锁");
    Thread.sleep(Long.MAX_VALUE);
}
```



写锁:  同一线程同一节点-可重入写和读, 不同线程同一节点时读和写都阻塞, 写锁释放后, 所有排队的读锁都被唤醒

```java
@WriteLock(bizPath = "/product", waitTime = -1)
public void write(boolean over) throws InterruptedException, LockException {
    System.out.println("获取写锁");
    if (over) {
        return;
    }
    ((Test)AopContext.currentProxy()).write(true);
    Thread.sleep(Long.MAX_VALUE);
}
```



读锁-如果写锁没有占用, 读锁之间是非阻塞的

```java
@ReadLock(bizPath = "/product", waitTime = -1)
public void read() throws InterruptedException, LockException {
    System.out.println("获取读锁");
    Thread.sleep(Long.MAX_VALUE);
}
```



# 源码地址

- GitHub:https://github.com/ares5k/ares5k-zookeeper-distribute-lock
- 码云：https://gitee.com/ares5k/ares5k-zookeeper-distribute-lock
