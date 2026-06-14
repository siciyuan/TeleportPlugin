# TeleportPlugin

轻量级 Minecraft 传送插件，支持模块化设计、数据库存储和丰富的经济系统。

## 功能特性

### 传送系统
- `/spawn` - 传送至主城
- `/setspawn` - 设置主城位置（管理员）
- `/sethome <名称>` - 设置家
- `/home <名称>` - 传送回家
- `/delhome <名称>` - 删除家
- `/homes` - 查看所有家

### 随机传送
- `/rtp [半径]` - 随机传送到指定范围
- `/rtpinfo` - 查看RTP信息（免费次数、价格）

### TPA 系统
- `/tpa <玩家>` - 请求传送到玩家
- `/tpahere <玩家>` - 请求玩家传送到你
- `/tpaccept` - 接受传送请求
- `/tpdeny` - 拒绝传送请求

### 经济系统
- `/balance` 或 `/bal` - 查看余额
- `/pay <玩家> <金额>` - 转账给玩家（支持税率）
- `/givemoney <玩家> <金额>` - 发放金币（管理员）

### 矿物兑换系统
- `/deposit <物品> <数量>` - 存入矿物兑换货币（无税）
- `/withdraw <物品> <数量>` - 提取货币兑换矿物（有税）
- `/exchange` 或 `/exchangeinfo` - 查看兑换比例

#### 可兑换物品（完整列表）
| 物品 | 物品ID | 金币 |
|------|--------|------|
| 金锭 | gold_ingot | 100 |
| 金粒 | gold_nugget | 11.11 |
| 金块 | gold_block | 900 |
| 原金 | raw_gold | 50 |
| 铁锭 | iron_ingot | 20 |
| 铁粒 | iron_nugget | 2.22 |
| 铁块 | iron_block | 180 |
| 原铁 | raw_iron | 10 |
| 铜锭 | copper_ingot | 10 |
| 铜粒 | copper_nugget | 1.11 |
| 铜块 | copper_block | 90 |
| 原铜 | raw_copper | 5 |
| 钻石 | diamond | 500 |
| 钻石块 | diamond_block | 4500 |
| 绿宝石 | emerald | 300 |
| 绿宝石块 | emerald_block | 2700 |
| 下界合金锭 | netherite_ingot | 2000 |
| 下界合金块 | netherite_block | 18000 |
| 煤炭 | coal | 10 |
| 木炭 | charcoal | 8 |
| 煤炭块 | coal_block | 90 |
| 青金石 | lapis_lazuli | 5 |

### 公告系统
- `/announce <消息>` - 发送全局公告（带标题）
- `/broadcast <消息>` - 同上
- `/setmotd <消息>` - 设置服务器MOTD（玩家列表上方显示）
- `/registersign` - 注册告示牌为公告板
- `/unregistersign` - 取消注册告示牌
- `/signlist` - 查看已注册告示牌
- `/setsign <行1> [行2] [行3] [行4]` - 设置告示牌内容（支持颜色代码）
- `/addannouncement <内容>` - 添加公告内容
- `/removeannouncement <序号>` - 删除公告
- `/announcementlist` - 查看公告列表

#### 颜色代码
使用 `&` 符号表示颜色：
- `&0` 黑色 | `&1` 深蓝 | `&2` 深绿 | `&3` 深青
- `&4` 深红 | `&5` 深紫 | `&6` 金色 | `&7` 灰色
- `&8` 深灰 | `&9` 蓝色 | `&a` 绿色 | `&b` 青色
- `&c` 红色 | `&d` 粉色 | `&e` 黄色 | `&f` 白色

### 世界主城保护系统
- `/prot add <玩家>` - 添加豁免玩家
- `/prot remove <玩家>` - 移除豁免玩家
- `/prot list` - 查看豁免列表
- `/prot info` - 查看保护信息
- `/prot setradius <半径>` - 设置保护半径
- `/prot toggle <类型>` - 切换保护开关
- `/prot enable` - 启用保护
- `/prot disable` - 禁用保护
- `/prot reload` - 重载配置

#### 保护类型
| 类型 | 说明 |
|------|------|
| `block-break` | 破坏方块 |
| `block-place` | 放置方块 |
| `pvp` | PvP战斗 |
| `entity-damage` | 生物伤害 |
| `chest-access` | 打开箱子 |
| `door-interact` | 使用门/按钮 |
| `bucket-use` | 使用桶/容器 |
| `explosion-damage` | 爆炸伤害 |

### 领地系统（Land System）
领地系统允许玩家购买和管理自己的土地，支持多级权限管理。

#### 领地命令
| 命令 | 说明 | 权限要求 |
|------|------|---------|
| `/land buy [r=半径]` | 购买领地（默认半径5格，最大50格） | 普通玩家 |
| `/land sell` | 出售当前所在领地（返还50%价格） | 地主 |
| `/land list` | 查看拥有的和作为成员的领地 | 普通玩家 |
| `/land info` | 查看当前领地详细信息 | 成员 |
| `/land set <名称>` | 设置领地名称 | 地主 |
| `/land add <玩家> [类型]` | 添加成员（owner/member/guest） | 地主 |
| `/land remove <玩家>` | 移除成员 | 地主 |
| `/land perm <玩家> <权限> <true/false>` | 设置成员权限 | 地主 |
| `/land tp <名称>` | 传送到领地 | 成员 |
| `/land help` | 查看帮助信息 | 所有人 |

#### 成员类型
| 类型 | 说明 | 权限级别 | 默认权限 |
|------|------|---------|---------|
| `owner` | 地主 | 100 | 拥有所有权限 |
| `member` | 成员 | 50 | 建设、破坏、交互、箱子、动物、物品 |
| `guest` | 访客 | 10 | 无特殊权限（需单独配置） |

#### 领地权限列表
| 权限ID | 说明 | 默认开放（成员） |
|--------|------|----------------|
| `build` | 放置方块 | ✓ |
| `destroy` | 破坏方块 | ✓ |
| `interact` | 交互（门、按钮等） | ✓ |
| `chest` | 打开箱子/容器 | ✓ |
| `pvp` | PVP战斗 | ✗ |
| `animals` | 动物交互（繁殖、驯服等） | ✓ |
| `itemdrop` | 掉落物品 | ✓ |
| `itempickup` | 拾取物品 | ✓ |

#### 领地价格计算
领地价格基于面积计算：
- 单价：0.5 金币/格
- 最小价格：100 金币
- 计算公式：`价格 = max(100, 面积 × 0.5)`
- 出售返还：购买价格的50%

### 地标系统（Waypoint System）
地标系统允许玩家创建公共传送点，支持免费次数和收费模式。每个地标可以独立配置价格和权限。

#### 地标命令
| 命令 | 说明 | 权限要求 |
|------|------|---------|
| `/warp <名称>` | 传送到指定地标 | 普通玩家 |
| `/warp create <名称>` | 在当前位置创建地标 | 普通玩家 |
| `/warp delete <名称>` | 删除指定地标 | 创建者/管理员 |
| `/warp list` | 查看所有可用地标 | 普通玩家 |
| `/warp info <名称>` | 查看地标详细信息（价格、免费次数等） | 普通玩家 |
| `/warp help` | 显示帮助信息 | 所有人 |

#### 管理命令（管理员）
这些命令用于**修改已存在的地标属性**，`<名称>` 参数指定要修改的地标：

| 命令 | 说明 | 示例 |
|------|------|------|
| `/warp setcreatecount <名称> <次数>` | 设置该地标允许免费创建的次数 | `/warp setcreatecount 主城 5` |
| `/warp settpcount <名称> <次数>` | 设置该地标每天免费传送次数 | `/warp settpcount 钻石矿 3` |
| `/warp setcreateprice <名称> <价格>` | 设置创建该地标所需费用 | `/warp setcreateprice 私人领地 100` |
| `/warp settpprice <名称> <价格>` | 设置传送至该地标所需费用 | `/warp settpprice 末地传送门 50` |
| `/warp setpermission <名称> <true/false>` | 设置该地标是否需要权限才能使用 | `/warp setpermission VIP传送点 true` |

#### 权限管理命令
| 命令 | 说明 | 权限要求 |
|------|------|---------|
| `/warp permit <名称>` | 申请使用需要权限的地标 | 普通玩家 |
| `/warp approve <名称> <玩家>` | 批准玩家使用申请 | 地标创建者 |
| `/warp deny <名称> <玩家>` | 拒绝玩家使用申请 | 地标创建者 |

**详细流程说明：**

1. **申请权限**（玩家使用）
   - 玩家使用 `/warp permit <地标名称>` 命令
   - 系统检查地标是否需要权限
   - 将玩家添加到待批准列表中
   - 通知地标创建者

2. **批准/拒绝**（创建者使用）
   - 创建者使用 `/warp approve <地标名称> <玩家名称>` 批准
   - 创建者使用 `/warp deny <地标名称> <玩家名称>` 拒绝
   - 系统从待批准列表中移除玩家

3. **待批准列表**（内部机制）
   - 所有待批准玩家存储在 `pendingPermissions` 集合中
   - 列表按申请顺序存储，第一个申请者会被优先处理

#### 地标设置示例
```bash
# 创建地标
/warp create 钻石矿

# 设置该地标每天3次免费传送
/warp settpcount 钻石矿 3

# 设置收费传送（每次100金币）
/warp settpprice 钻石矿 100

# 设置需要权限的地标（仅授权玩家可使用）
/warp setpermission VIP传送点 true
```

#### 地标默认设置
| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| 创建免费次数 | 0 | 创建地标时无免费次数限制 |
| 传送免费次数 | 3 | 默认每天3次免费传送 |
| 创建费用 | 0 | 默认免费创建 |
| 传送费用 | 0 | 默认免费传送 |
| 需要权限 | false | 默认所有玩家均可使用 |

#### 命令参数说明
- **`<名称>`**：目标地标的名字，用于指定要操作的地标
- **`<次数>`**：免费使用次数（整数，≥0）
- **`<价格>`**：金币价格（正数）
- **`<true/false>`**：是否启用该设置

### 管理命令
- `/teleportadmin` - 显示管理帮助
- `/teleportadmin info` - 查看当前配置
- `/teleportadmin reload` - 重载配置文件
- `/teleportadmin migrate` - 强制重新迁移数据
- `/teleportadmin status` - 查看插件状态
- `/setrtpprice <价格>` - 设置RTP价格
- `/setrtpfree <次数>` - 设置每日免费RTP次数
- `/setmaxhomes <数量>` - 设置最大家数量
- `/settax <税率>` - 设置转账税率（0.00-1.00）

## 权限节点

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| `teleport.admin` | 管理员权限（所有命令） | OP |
| `teleport.home` | 使用家传送 | 所有人 |
| `teleport.sethome` | 设置/删除家 | 所有人 |
| `teleport.spawn` | 传送到主城 | 所有人 |
| `teleport.rtp` | 使用随机传送 | 所有人 |
| `teleport.tpa` | 发送传送请求 | 所有人 |
| `teleport.tpahere` | 请求传送到你 | 所有人 |
| `teleport.tpaccept` | 接受传送请求 | 所有人 |
| `teleport.tpdeny` | 拒绝传送请求 | 所有人 |
| `teleport.balance` | 查看余额 | 所有人 |
| `teleport.pay` | 转账 | 所有人 |
| `teleport.exchange` | 矿物兑换 | 所有人 |
| `teleport.land` | 领地系统 | 所有人 |
| `teleport.warp` | 使用地标传送 | 所有人 |
| `teleport.warp.create` | 创建地标 | 所有人 |
| `teleport.warp.delete` | 删除地标 | 所有人 |
| `teleport.warp.admin` | 地标管理 | OP |
| `teleport.warp.perm` | 权限申请/审批 | 所有人 |

## 配置说明

### config.yml 完整配置
```yaml
# RTP配置
rtp:
  min-radius: 50          # 最小随机传送半径
  max-radius: 5000       # 最大随机传送半径
  default-radius: 200     # 默认随机传送半径
  daily-free-count: 3     # 每天免费RTP次数
  price: 100.0           # RTP价格（金币）

# 经济系统配置
economy:
  enabled: true
  transfer-tax: 0.05     # 转账税率 (5%)
  exchange:
    enabled: true
    withdraw-tax: 0.10   # 提取矿物税率 (10%)
    rates:
      gold_ingot: 100.0
      gold_nugget: 11.11
      gold_block: 900.0
      raw_gold: 50.0
      iron_ingot: 20.0
      iron_nugget: 2.22
      iron_block: 180.0
      raw_iron: 10.0
      copper_ingot: 10.0
      copper_nugget: 1.11
      copper_block: 90.0
      raw_copper: 5.0
      diamond: 500.0
      diamond_block: 4500.0
      emerald: 300.0
      emerald_block: 2700.0
      netherite_ingot: 2000.0
      netherite_block: 18000.0
      coal: 10.0
      charcoal: 8.0
      coal_block: 90.0
      lapis_lazuli: 5.0

# 数据库配置
database:
  enabled: true           # 启用数据库存储

# 家配置
homes:
  max-homes: 3           # 玩家最大家数量

# 世界保护配置
world-protection:
  enabled: true
  spawn-protection-radius: 100  # 主城保护半径
  allowed-players: []     # 豁免玩家列表
  protection:
    block-break: true
    block-place: true
    pvp: true
    chest-access: true
    door-interact: true
    bucket-use: true
    explosion-damage: true
    entity-damage: true

# 公告系统配置
announcements:
  motd: "欢迎来到服务器"  # MOTD消息（玩家列表上方）
  signs: []              # 已注册告示牌位置
  list: []               # 公告内容列表
```

## 数据库

插件使用 **SQLite** 数据库，数据自动保存在 `plugins/TeleportPlugin/database.db`。

### 数据表结构

| 表名 | 说明 | 用途 |
|------|------|------|
| `worlds` | 世界数据 | 存储主城位置、世界设置 |
| `users` | 用户数据 | 余额、RTP次数、设置等 |
| `homes` | 家数据 | 玩家家的位置信息 |
| `lands` | 领地数据 | 领地位置、成员、权限 |
| `territories` | 预留领地表 | 未来拓展使用 |
| `user_stats` | 用户统计 | 预留统计功能 |

## 数据迁移

从旧版本升级时，插件会自动执行以下迁移：

1. **家数据迁移**：从 `homes.yml` 文件迁移到数据库
2. **主城数据迁移**：从 `config.yml` 迁移到数据库
3. **用户数据初始化**：自动创建新用户记录

迁移过程完全自动，无需手动操作。

## 安装指南

1. 下载最新版本的 `TeleportPlugin-X.X.X.jar`
2. 将 JAR 文件放入服务器的 `plugins` 目录
3. 重启服务器
4. 插件会自动创建：
   - `plugins/TeleportPlugin/config.yml` - 配置文件
   - `plugins/TeleportPlugin/database.db` - 数据库文件

## 构建说明

### Windows 构建
```powershell
# 编译
javac -cp "server.jar;libraries/*" -d target/classes src/main/java/com/scroam/teleport/*.java

# 打包
jar cf TeleportPlugin.jar -C target/classes . -C src/main/resources .
```

### Linux/Mac 构建
```bash
# 编译
javac -cp "server.jar:libraries/*" -d target/classes src/main/java/com/scroam/teleport/*.java

# 打包
jar cf TeleportPlugin.jar -C target/classes . -C src/main/resources .
```

## 版本历史

### v2.2.0
- ✅ 添加完整领地系统
- ✅ 支持购买、出售领地
- ✅ 多级成员权限管理（owner/member/guest）
- ✅ 8种领地权限控制
- ✅ 领地边界保护（防止重叠）
- ✅ 领地数据自动保存到数据库
- ✅ 优化配置文件自动生成

### v2.1.0
- ✅ 添加矿物兑换系统（金、铁、铜、钻石、绿宝石等）
- ✅ 添加公告和告示牌系统
- ✅ 添加MOTD功能（玩家列表显示）
- ✅ 添加转账税功能
- ✅ 添加世界主城保护系统
- ✅ 支持数据库存储（SQLite）
- ✅ 添加数据迁移功能
- ✅ 添加管理员配置命令

### v2.0.0
- ✅ 重构代码，支持模块化设计
- ✅ 添加数据库支持
- ✅ 添加经济系统
- ✅ 添加 TPA 系统

### v1.0.0
- ✅ 基础传送功能
- ✅ 家系统
- ✅ 随机传送

## 作者

SCROAM

## 网站

https://blog.world123.top

## 支持

如有问题或建议，请通过以下方式联系：
- 博客：https://blog.world123.top
- GitHub：https://github.com/siciyuan/TeleportPlugin
- 直接下载使用版本:https://www.spigotmc.org/resources/teleportplugin.136114/