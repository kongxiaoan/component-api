---
theme: channing-cyan
highlight: a11y-light
---
### 一、代码中心化问题

> 将一个大型的项目拆分成多个Module或者新开的组件化项目,想要的预期是这些module之间是平级的关系．这样一来就可以使得业务相对集中，每个人都可以专注在一件事上。同时，代码的耦合度也会随之降低，达到高度解耦状态，因为同级的module不存在依赖关系，在编译上就是隔离的，这会让组件间的依赖非常清楚，同时也具有更高的重用性，**组件强调复用，模块强调职责划分。** 他们没有非常严格的划分。

> 达到可复用要求的模块，那么这个模块就是组件。每个组件的可替代性、热插拔、独立编译都将可行，

#### 1.1 代码中心化在Android组件化中的问题体现

貌似Android的组件化是非常简单且可行的，AS提供的module创建方式加gradle.properies 自定义属性可读，或者ext全局可配置的project属性亦或kotlin dsl 中kotlin的语法糖都为我们提供了application和library的切换。

然后将代码放在不同的仓库位置最好是单独git 仓库级别的管理隔离，就能达到我们想要解决的一系列问题。

然而事情并不是想象的那么简单...

一些列的问题接踵而至，于我而言影响最深的就是应用设计时使用映射型数据库，导致集成模式和组件模式中复用出现问题；最终使用注解配合Java特性生成代码，虽然不完美但是依然解决了此问题。正当我为了胜利欢呼的时刻，一片[《微信Android模块化架构重构实践》](https://mp.weixin.qq.com/s/6Q818XA5FaHd7jJMFBG60w)文章进入我的眼帘。

随即闪现出了一个重要且紧急的问题，**代码中心化的问题**

这个问题是怎么出现的呢？在微信Android模块化架构重构实践中是这样描述的

"""

然而随着代码继续膨胀，一些问题开始突显出来。首先出问题的是基础工程libnetscene和libplugin。基础工程一直处于不断膨胀的状态，同时主工程也在不断变大。**同时基础工程存在中心化问题**，许多业务Storage类被附着在一个核心类上面，久而久之这个类已经没法看了。此外当初为了平滑切换到gradle避免结构变化太大以及太多module，我们将所有工程都对接到一个module上。缺少了编译上的隔离，模块间的代码边界出现一些劣化。虽然紧接着开发了工具来限制模块间的错误依赖，但这段时间里的影响已经产生。在上面各种问题之下，许多模块已经称不上“独立”了。所以当我们重新审视代码架构时，以前良好模块化的架构设计已经逐渐变了样。

"""

再看他们分析问题的原因：

"""

翻开基础工程的代码，我们看到除了符合设计初衷的存储、网络等支持组件外，还有相当多的业务相关代码。这些代码是膨胀的来源。但代码怎么来的，非要放这？一切不合理皆有背后的逻辑。在之前的架构中，我们大量适用Event事件总线作为模块间通信的方式，也基本是唯一的方式。使用Event作为通信的媒介，自然要有定义它的地方，好让模块之间都能知道Event结构是怎样的。这时候基础工程好像就成了存放Event的唯一选择——Event定义被放在基础工程中；接着，遇到某个模块A想使用模块B的数据结构类，怎么办？把类下沉到基础工程；遇到模块A想用模块B的某个接口返回个数据，Event好像不太适合？那就把代码下沉到基础工程吧……

就这样越来越多的代码很“自然的”被下沉到基础工程中。

我们再看看主工程，它膨胀的原因不一样。分析一下基本能确定的是，首先作为主干业务一直还有需求在开发，膨胀在所难免，缺少适当的内部重构但暂时不是问题的核心。另一部分原因，则是因为模块的生命周期设计好像已经不满足使用需要。之前的模块生命周期是从“Account初始化”到“Account已注销”，所以可以看出在这时机之外肯定还有逻辑。放在以前这不是个大问题，刚启动还不等“Account初始化”就要执行的逻辑哪有那么多。而现在不一样，再简单的逻辑堆积起来也会变复杂。此时，在模块生命周期外的逻辑基本上只能放主工程。

此外的问题，模块边界破坏、基础工程中心化，都是代码持续劣化的帮凶...

"""

看完之后就陷入了沉思，这个问题不就是我们面临的问题吗？不仅是在组件化中，在很多形成依赖关系的场景中都有此类问题。

假设有user组建和分享组件，分享组件需要user组件提供数据。

具体是怎么体现的呢，我们来看一组图：

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/99e948abf45f4ad19996103e74629048~tplv-k3u1fbpfcp-zoom-1.image)

##### 1.1.1 图1

> 解决方式为分享组件依赖user组件，能解决问题，假设，有一个组件A，需要引用分享组件，就必须依赖分享组件和user组件，这就一举打破了组件编译隔离的愿景，组件化将失去香味儿。

##### 1.1.2 图2

> 将user组件中的公共数据部分下沉到base组件，分享组件依赖base组件即可实现数据提供，然而当非常多的组件需要互相提供数据时，将出现中心化问题，只需要分享组件的B组件不得不依赖base组件，引入其他数据。也就造成了代码中心化下沉失去组件化的意义。

>

### 二、 怎么解决代码中心化问题

微信面对这个痛心疾首的问题时发出了“君有疾在腠理，不治将恐深” 的感慨，但也出具了非常厉害的操作-.api 化

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/09064d1185a5483bbea87f3fde728efa~tplv-k3u1fbpfcp-zoom-1.image)

这个操作非常高级，做法非常腾讯，但是此文档中只提到了精髓，没有具体的操作步骤，对我们来讲依然存在挑战，

#### 2.1 什么是代码中心化问题的.api方案

先看一下具体的操作过程是什么样的，

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6a1958cfe0a04d248e4a5cd31e81abea~tplv-k3u1fbpfcp-watermark.image?)

上图3中，我们使用某种技术将user组件中需要共享数据的部分抽象成接口，利用AS对文件类型的配置将（kotlin）后拽修改为.api ，然后再创建一个同包名的module-api 组件用来让其他组件依赖，

分享组件和其他组件以及自身组件在module模式下均依赖该组件，这样就能完美的将需要共享的数据单独出去使用了，

##### 2.1.1 SPI 方式实现

这个有点类似SPI(Service Provider Interface)机制，具体可参考：https://www.jianshu.com/p/46b42f7f593c
![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/295220e953bd4771bb259b157cd88dda~tplv-k3u1fbpfcp-zoom-1.image)

（来源上面的文档）

大概就是说我们可以将要共享的数据先抽象到接口中形成标准服务接口，然后在具体的实现中，然后在对应某块中实现该接口，当服务提供者提供了接口的一种具体实现后，在jar包的META-INF/services目录下创建一个以“接口全限定名”为命名的文件，内容为实现类的全限定名；

然后利用 ServiceLoader 来加载配置文件中指定的实现，此时我们在不同组件之间通过ServiceLoader加载需要的文件了

##### 2.1.2 利用ARouter

利用ARouter 在组件间传递数据的方式+ gralde 自动生成module-api 组件，形成中心化问题的.api 化

假设我们满足上述的所有关系，并且构建正确，那我们怎么处理组件间的通信，

Arouter 阿里通信路由

```kotlin
@Route(path = "/test/activity")
public class YourActivity extend Activity {
    ...
}

跳转：

ARouter.getInstance().build("/test/activity").withLong("key1", 666L).navigation()
```

```kotlin
// 声明接口,其他组件通过接口来调用服务

public interface HelloService extends IProvider {

   String sayHello(String name);

}

// 实现接口

@Route(path = "/yourservicegroupname/hello", name = "测试服务")

public class HelloServiceImpl implements HelloService {

    @Override

    public String sayHello(String name) {

        return "hello, " + name;

    }

    @Override

    public void init(Context context) {

    }

}

//测试

public class Test {

    @Autowired

    HelloService helloService;

    @Autowired(name = "/yourservicegroupname/hello")

    HelloService helloService2;

    HelloService helloService3;

    HelloService helloService4;

    public Test() {
    
        ARouter.getInstance().inject(this);

    }

    public void testService() {

    // 1. (推荐)使用依赖注入的方式发现服务,通过注解标注字段,即可使用，无需主动获取

    // Autowired注解中标注name之后，将会使用byName的方式注入对应的字段，不设置name属性，会默认使用byType的方式发现服务(当同一接口有多个实现的时候，必须使用byName的方式发现服务)

    helloService.sayHello("Vergil");

    helloService2.sayHello("Vergil");

    // 2. 使用依赖查找的方式发现服务，主动去发现服务并使用，下面两种方式分别是byName和byType

    helloService3 = ARouter.getInstance().navigation(HelloService.class);

    helloService4 = (HelloService)ARouter.getInstance().build("/yourservicegroupname/hello").navigation();

    helloService3.sayHello("Vergil");

    helloService4.sayHello("Vergil");
    }

}
```
假如user组件的用户信息需要给支付组件使用，那我们怎么处理？

ARouter 可以通过上面的IProvider 注入服务的方式通信,或者使用EventBus这种方式

```kotlin
data class UserInfo(val uid: Int, val name: String)
/**
*@author kpa

*@date 2021/7/21 2:15 下午

*@email billkp@yeah.net

*@description 用户登录、获取信息等

*/

interface IAccountService : IProvider {
    //获取账号信息 提供信息*
    fun getUserEntity(): UserInfo?
}

//注入服务

@Route(path = "/user/user-service")
class UserServiceImpl : IAccountService {

    //...

}
```

在支付组件中

```kotlin
IAccountService accountService = ARouter.getInstance().navigation(IAccountService.class);

UserInfo bean = accountService. getUserEntity();
```

问题就暴露在了我们眼前，支付组件中的IAccountService 和UserInfo 从哪里来？

这也就是module-api 需要解决的问题，在原理方面：

1.  将需要共享的数据和初始化数据的类文件设计为.api文件

打开AS-> Prefernces -> File Types 找到kotlin （Java）选中 在File name patterns 里面添加"*.api"（注意这个后缀随意开心的话都可以设置成*.kpa）

举例：

UserInfo.api

```kotlin
data class UserInfo(val userName: String, val uid: Int)
```

UserService.api
```kotlin
interface UserService {

fun getUserInfo(): UserInfo

}
```

2.  生成包含共享的数据和初始化数据的类文件的module-api 组件

这步操作有以下实现方式，

-   自己手动创建一个module-api 组件 显然这是不可取但是可行的
-   使用脚本语言shell 、python 等扫描指定路径生成对应module-api
-   利用Android 编译环境及语言groovy，编写gradle脚本，优势在于不用考虑何时编译，不打破编译环境，书写也简单

### 三、module-api 脚本
找到这些问题出现的原理及怎么去实现之后，从github上找到了优秀的人提供的脚本，完全符合我们的使用预期
```groovy
def includeWithApi(String moduleName) {

def packageName = "com/xxx/xxx"

    //先正常加载这个模块

    include(moduleName)

    //找到这个模块的路径
    String originDir = project(moduleName).projectDir
    //这个是新的路径
    String targetDir = "${originDir}-api"
    //原模块的名字
    String originName = project(moduleName).name
    //新模块的名字
    def sdkName = "${originName}-api"
    //这个是公共模块的位置，我预先放了一个 新建的api.gradle 文件进去
    String apiGradle = project(":apilibrary").projectDir
    // 每次编译删除之前的文件
    deleteDir(targetDir)

    //复制.api文件到新的路径
    copy() {
        from originDir
        into targetDir
        exclude '**/build/'
        exclude '**/res/'
        include '**/*.api'
    }
    //直接复制公共模块的AndroidManifest文件到新的路径，作为该模块的文件
    copy() {
        from "${apiGradle}/src/main/AndroidManifest.xml"
        into "${targetDir}/src/main/"
    }
    //复制 gradle文件到新的路径，作为该模块的gradle
    copy() {
        from "${apiGradle}/api.gradle"
        into "${targetDir}/"
    }

    //删除空文件夹
    deleteEmptyDir(*new* File(targetDir))
    //todo 替换成自己的包名
    //为AndroidManifest新建路径，路径就是在原来的包下面新建一个api包，作为AndroidManifest里面的包名
    String packagePath = "${targetDir}/src/main/java/" + packageName + "${originName}/api"
    //todo 替换成自己的包名，这里是apilibrary模块拷贝的AndroidManifest，替换里面的包名
    //修改AndroidManifest文件包路径
    fileReader("${targetDir}/src/main/AndroidManifest.xml", "commonlibrary", "${originName}.api")

    new File(packagePath).mkdirs()
    //重命名一下gradle
    def build = new* File(targetDir + "/api.gradle")

    if(build.exists()) {
        build.renameTo(new File(targetDir + "/build.gradle"))
    }
    // 重命名.api文件，生成正常的.java文件
    renameApiFiles(targetDir, '.api', '.java')
    //正常加载新的模块
    include ":$sdkName"
  }

private void deleteEmptyDir(File dir) {

    if(dir.isDirectory()) {
        File[] fs = dir.listFiles()
        if(fs != null && fs.length > 0) {
            for (int i = 0; i < fs.length; i++) {
                File tmpFile = fs[i]
                if (tmpFile.isDirectory() {
                    deleteEmptyDir(tmpFile)
                }
                if (tmpFile.isDirectory() && tmpFile.listFiles().length <= 0){
                    tmpFile.delete()
                }
          }
       }
   if (dir.isDirectory() && dir.listFiles().length == 0) {
        dir.delete()
   }
 }

private void deleteDir(String targetDir) {

    FileTree targetFiles = fileTree(targetDir)

    targetFiles.exclude "*.iml"

    targetFiles.each { File file ->

        file.delete()

    }

}

/**

* rename api files(java, kotlin...)

**/

private def renameApiFiles(root_dir, String suffix, String replace) {

    FileTree* files = fileTree(root_dir).include("**/*$suffix")

    files.each {

        File file ->

        file.renameTo(*new* File(file.absolutePath.replace(suffix, replace)))

    }

}

//替换AndroidManifest里面的字段*

def fileReader(path, name, sdkName) {

    def readerString = ""

    def hasReplace = false

    file(path).withReader('UTF-8') { reader ->

        reader.eachLine {

            if (it.find(name)) {
                it = it.replace(name, sdkName)
                hasReplace = true
            }
            readerString <<= it
            readerString << '\n'

        }
        if (hasReplace) {

            file(path).withWriter('UTF-8') {
                within ->
                within.append(readerString)
            }

        }

    return readerString

    }

}
```

使用：

```groovy
includeWithApi ":user"
```

Demo[component-api](https://github.com/kongxiaoan/component-api.git)

参考文献：

[《微信Android重构模块化架构重构实践》](https://mp.weixin.qq.com/s/6Q818XA5FaHd7jJMFBG60w)

[《gradle实现API方案》](https://github.com/tyhjh/module_api)

[《高级开发必须理解的Java中SPI机制》](https://www.jianshu.com/p/46b42f7f593c)
