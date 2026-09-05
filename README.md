# Legacy Portal Dupe

这是mincraft java版26.2的一个mod，其功能为恢复java版1.21~1.21.1版本中的跨纬度药水复制特性，该特性最初由bilibili@萌萌de小公举发现 注：本mod为兼容自 Java 版 1.21.2 起引入的末影珍珠区块加载（Ender Pearl Chunk Loading）机制，仅恢复了旧版药水跨维度残留执行时序，由于煮包技术不精，大量采用了gpt老师提供的代码，球球大家轻点骂qwq

简介

在 Minecraft 1.21/1.21.1 中，喷溅药水与下界传送门之间曾存在一种特殊的跨维度行为。

在特定的传送门、运动与碰撞条件下，喷溅药水完成跨维度传送后，原维度中的旧药水实体虽然已经被移除，但当前的投射物 Tick 调用仍可能继续执行剩余的碰撞处理。

因此可能出现：

新的药水实体已经进入目标维度；
原维度中的旧实体仍完成了一次残留碰撞；
药水在原维度产生破碎与喷溅效果。
 
Minecraft 后续版本调整了实体、投射物以及传送门相关代码的执行顺序，使这一行为不再能够按照原来的方式发生。

Legacy Portal Dupe尝试在 Minecraft 26.2 中重新实现这一旧版机制。

---

功能

- 在 Minecraft 26.2 中恢复旧版喷溅药水跨维度残留行为
- 目前仅针对喷溅药水进行处理
- 重新构造旧版本相关的投射物 / 传送门执行时序
- 使用 Minecraft 原有的投射物碰撞与药水命中逻辑
- 不通过直接生成第二个药水实体的方式实现“复制”
- 基于 Fabric 与 SpongePowered Mixin

---

原理

这个模组的核心并不是单纯的检测到药水进入传送门，然后额外生成一瓶药水，而是尝试重新构造旧版本中已经消失的执行顺序

简化后的过程如下：

喷溅药水进入传送门
        ↓
记录并延迟相关 Portal 处理
        ↓
恢复旧版 discovery tick 的部分运动行为
        ↓
下一 Tick 进行跨维度处理
        ↓
目标维度创建新的药水实体 B
        ↓
原维度中的旧药水实体 A 被移除
        ↓
但旧实体 A 所在的 Java 调用仍继续执行
        ↓
使用 A 原有的位置与速度继续进行残留碰撞检测
        ↓
如果命中方块
        ↓
调用 Minecraft 原有的投射物命中逻辑
        ↓
喷溅药水在原维度破碎

因此，从游戏现象来看可能类似于：
                    ┌──→ 原维度：旧实体 A 残留碰撞 → 药水破碎
药水 → 下界传送门 ──┤
                    └──→ 目标维度：新实体 B → 正常继续存在


需要注意的是，A 与 B 并不是同一个 Java 对象。跨维度过程中，Minecraft 会在目标维度创建新的实体对象，而原维度中的实体会被移除

这个模组所恢复的关键行为，就是原实体虽然已经因为跨维度而被移除，但当前尚未结束的投射物处理仍能够完成一次旧版式的残留碰撞

---

实现方式：

模组主要通过以下 Mixin 实现：

LegacyPotionPortalDelayMixin

负责处理传送门发现阶段的延迟以及旧版运动时序补偿

EntityMixin

负责记录跨维度状态以及相关实体状态

LegacyThrowableProjectileMixin

负责重新构造关键的旧版投射物执行顺序，并在跨维度之后执行残留碰撞检测

ProjectileInvoker

用于调用 Minecraft 原有的投射物碰撞与命中处理方法

---

环境要求

目前版本针对：

- Minecraft Java Edition **26.2**
- Fabric Loader 0.19.3 或更高版本
- Fabric API
- Java 25 或更高版本

其他 Minecraft 版本暂未保证兼容

---

安装

1. 安装适用于 Minecraft 26.2 的 Fabric Loader
2. 安装 Fabric API
3. 从本项目的 Releases 页面下载最新版本的 `.jar`
4. 将 `.jar` 放入 Minecraft 实例的 `mods` 文件夹
5. 使用 Fabric 启动游戏

---

从源码构建

克隆本仓库后，在项目目录执行：

Windows

```powershell
.\gradlew.bat build
```

Linux / macOS

```bash
./gradlew build
```

构建完成后的文件位于：

```text
build/libs/
```

---

主要源码

主要实现位于 src/main/java/com/example/

Mixin 配置 src/main/resources/legacyportaldupe.mixins.json

Fabric 模组信息：
src/main/resources/fabric.mod.json

兼容性说明

本项目的目标是恢复Minecraft 1.21/1.21.1 中相关机制的核心行为，而不是完整移植旧版本的整个投射物物理系统。由于 Minecraft 后续版本对实体、传送门以及投射物的处理方式进行了较大修改，因此在部分边缘情况下，本模组的行为可能与旧版本存在差异。与其他修改实体、投射物或下界传送门机制的模组同时使用时，也可能产生兼容性问题

License

本项目采用 CC0-1.0许可证。


作者：

gihub:[xuanNyachan0712qwq](https://github.com/xuanNyachan0712qwq)
bilibili:不想起床の轩轩酱qwqhttps://space.bilibili.com/1972764626
v次元:轩轩https://bbs.lty.fan/user/1980
喜欢的话点个star再走吧qwq
