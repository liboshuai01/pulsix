# 三大风控场景的样例规则与样例事件

## 一、附录目标

本附录的作用不是做“业务百科”，而是给你的开源项目提供一组**可以直接用于演示、开发、联调、仿真和面试讲解**的标准样例。

这组样例应满足几个目标：

1. 能覆盖你项目的三个核心场景：
   - 登录风控
   - 注册反作弊
   - 交易风控

2. 能体现脉流实时风控平台的核心能力：
   - 统一事件模型
   - 流式特征
   - lookup 特征
   - 派生特征
   - 规则
   - 策略

3. 能直接用于：
   - 控制平台初始化数据
   - 仿真测试样例
   - Kafka mock 事件生成
   - README 演示脚本

所以本附录会给你一套“从业务描述 → 事件 → 特征 → 规则 → 策略 → 决策”的完整样例。

---

## 二、统一约定

### 2.1 决策动作

本附录统一使用以下动作：

- `PASS`：放行
- `REVIEW`：人工审核/二次校验
- `REJECT`：拒绝
- `TAG_ONLY`：仅打标签，不阻断

### 2.2 规则引擎

本附录默认规则表达式采用 Aviator/DSL 风格，示例写法尽量接近：

- `amount >= 5000 && user_trade_cnt_5m >= 3`
- `device_in_blacklist == true`

### 2.3 统一事件基础字段

三个场景都建议带上这些基础字段：

- `eventId`
- `traceId`
- `sceneCode`
- `eventCode`
- `eventTime`
- `userId`
- `deviceId`
- `ip`
- `channel`
- `ext`

---

# 三、场景一：登录风控

---

## 3.1 业务目标

登录风控主要解决：

- 撞库
- 爆破密码
- 异地异常登录
- 黑产设备登录
- 高风险 IP 登录

对于个人项目来说，登录风控是一个非常适合作为开场演示的场景，因为：

- 业务直观
- 特征简单清晰
- 演示效果好
- 能很好体现“实时统计 + 名单 + 规则”的组合

---

## 3.2 事件定义

### 标准事件编码

- `LOGIN_EVENT`

### 样例事件 JSON

```json
{
  "eventId": "E_LOGIN_0001",
  "traceId": "T_LOGIN_0001",
  "sceneCode": "LOGIN_RISK",
  "eventCode": "LOGIN_EVENT",
  "eventTime": "2026-03-07T09:00:00",
  "userId": "U1001",
  "deviceId": "D9001",
  "ip": "10.20.30.40",
  "channel": "APP",
  "loginResult": "FAIL",
  "failReason": "PASSWORD_ERROR",
  "city": "Shanghai",
  "province": "Shanghai",
  "ext": {
    "os": "Android",
    "appVersion": "1.0.2"
  }
}
```

---

## 3.3 推荐特征

### 基础字段特征

- `loginResult`
- `channel`
- `city`
- `province`

### 流式特征

1. `user_login_fail_cnt_10m`
   - 用户 10 分钟登录失败次数
   - aggType: `COUNT`
   - filterExpr: `loginResult == 'FAIL'`
   - entity: `userId`

2. `ip_login_fail_cnt_10m`
   - IP 10 分钟登录失败次数
   - aggType: `COUNT`
   - filterExpr: `loginResult == 'FAIL'`
   - entity: `ip`

3. `device_login_user_cnt_1h`
   - 设备 1 小时登录用户数
   - aggType: `DISTINCT_COUNT`
   - valueExpr: `userId`
   - entity: `deviceId`

### lookup 特征

4. `device_in_blacklist`
   - 设备是否命中黑名单
   - lookupType: `REDIS_SET`

5. `ip_risk_level`
   - IP 风险等级
   - lookupType: `REDIS_HASH`
   - 可取值： `LOW / MEDIUM / HIGH`

6. `user_in_white_list`
   - 用户是否命中白名单
   - lookupType: `REDIS_SET`

### 派生特征

7. `high_fail_user_flag`

```latex
user_login_fail_cnt_10m >= 5
```

8. `high_fail_ip_flag`

```latex
ip_login_fail_cnt_10m >= 20
```

---

## 3.4 推荐规则

### 规则 1：黑名单设备直接拒绝

- ruleCode: `LOGIN_R001`
- expr:

```latex
device_in_blacklist == true && user_in_white_list != true
```

- action: `REJECT`
- priority: 100
- reason:

```latex
设备命中黑名单
```

---

### 规则 2：用户短时高频失败登录

- ruleCode: `LOGIN_R002`
- expr:

```latex
user_login_fail_cnt_10m >= 5 && ip_risk_level == 'HIGH'
```

- action: `REVIEW`
- priority: 90
- reason:

```latex
用户10分钟失败次数={user_login_fail_cnt_10m}, IP风险等级={ip_risk_level}
```

---

### 规则 3：高风险 IP 爆破行为

- ruleCode: `LOGIN_R003`
- expr:

```latex
ip_login_fail_cnt_10m >= 20
```

- action: `REJECT`
- priority: 95
- reason:

```latex
IP10分钟失败次数={ip_login_fail_cnt_10m}
```

---

### 规则 4：设备多账号登录异常

- ruleCode: `LOGIN_R004`
- expr:

```latex
device_login_user_cnt_1h >= 5
```

- action: `REVIEW`
- priority: 80
- reason:

```latex
设备1小时关联登录用户数={device_login_user_cnt_1h}
```

---

## 3.5 推荐策略

### 策略名

- `LOGIN_RISK_POLICY`

### 决策模式

- `FIRST_HIT`

### 执行顺序

1. `LOGIN_R001`
2. `LOGIN_R003`
3. `LOGIN_R002`
4. `LOGIN_R004`

### 默认动作

- `PASS`

---

## 3.6 仿真样例

### 样例 A：正常登录

```json
{
  "eventId": "E_LOGIN_0002",
  "traceId": "T_LOGIN_0002",
  "sceneCode": "LOGIN_RISK",
  "eventCode": "LOGIN_EVENT",
  "eventTime": "2026-03-07T09:10:00",
  "userId": "U2001",
  "deviceId": "D2001",
  "ip": "1.1.1.1",
  "channel": "APP",
  "loginResult": "SUCCESS",
  "city": "Shanghai",
  "province": "Shanghai"
}
```

期望结果：

- `PASS`

---

### 样例 B：短时密码爆破

```json
{
  "eventId": "E_LOGIN_0003",
  "traceId": "T_LOGIN_0003",
  "sceneCode": "LOGIN_RISK",
  "eventCode": "LOGIN_EVENT",
  "eventTime": "2026-03-07T09:20:00",
  "userId": "U1001",
  "deviceId": "D9002",
  "ip": "10.20.30.40",
  "channel": "APP",
  "loginResult": "FAIL",
  "failReason": "PASSWORD_ERROR"
}
```

上下文假设：

- `user_login_fail_cnt_10m = 6`
- `ip_risk_level = HIGH`

期望结果：

- 命中 `LOGIN_R002`
- 最终 `REVIEW`

---

# 四、场景二：注册反作弊

---

## 4.1 业务目标

注册反作弊主要解决：

- 黑产批量注册
- 同设备养号
- 同 IP 大量刷号
- 异常手机号/渠道注册

这个场景非常适合体现：

- entity 维度切换
- distinct\_count 特征
- 黑名单命中
- 批量行为识别

---

## 4.2 事件定义

### 标准事件编码

- `REGISTER_EVENT`

### 样例事件 JSON

```json
{
  "eventId": "E_REG_0001",
  "traceId": "T_REG_0001",
  "sceneCode": "REGISTER_ANTI_FRAUD",
  "eventCode": "REGISTER_EVENT",
  "eventTime": "2026-03-07T10:00:00",
  "userId": "U3001",
  "deviceId": "D_REG_001",
  "ip": "22.33.44.55",
  "channel": "APP",
  "mobile": "13800001111",
  "inviteCode": "INV1001",
  "registerResult": "SUCCESS",
  "ext": {
    "appVersion": "2.0.1",
    "os": "iOS"
  }
}
```

---

## 4.3 推荐特征

### 流式特征

1. `device_register_user_cnt_1h`
   - 设备 1 小时注册用户数
   - aggType: `DISTINCT_COUNT`
   - valueExpr: `userId`
   - entity: `deviceId`

2. `ip_register_cnt_10m`
   - IP 10 分钟注册次数
   - aggType: `COUNT`
   - entity: `ip`

3. `mobile_prefix_register_cnt_1h`
   - 号段 1 小时注册次数
   - aggType: `COUNT`
   - valueExpr: `substr(mobile,0,7)`
   - entity: `mobilePrefix`
   - 一期可以先不做太复杂，实现时可改为直接上传字段

### lookup 特征

4. `device_in_blacklist`
5. `ip_in_proxy_list`
6. `mobile_in_risk_list`

### 派生特征

7. `suspicious_device_flag`

```latex
device_register_user_cnt_1h >= 3
```

8. `suspicious_ip_flag`

```latex
ip_register_cnt_10m >= 10
```

---

## 4.4 推荐规则

### 规则 1：黑名单设备注册拒绝

- ruleCode: `REG_R001`
- expr:

```latex
device_in_blacklist == true
```

- action: `REJECT`
- priority: 100

---

### 规则 2：代理 IP 高频注册

- ruleCode: `REG_R002`
- expr:

```latex
ip_in_proxy_list == true && ip_register_cnt_10m >= 10
```

- action: `REJECT`
- priority: 95

---

### 规则 3：同设备批量注册

- ruleCode: `REG_R003`
- expr:

```latex
device_register_user_cnt_1h >= 3
```

- action: `REVIEW`
- priority: 90

---

### 规则 4：风险手机号注册

- ruleCode: `REG_R004`
- expr:

```latex
mobile_in_risk_list == true
```

- action: `REVIEW`
- priority: 80

---

## 4.5 推荐策略

### 策略名

- `REGISTER_ANTI_FRAUD_POLICY`

### 决策模式

- `FIRST_HIT`

### 默认动作

- `PASS`

### 执行顺序

1. `REG_R001`
2. `REG_R002`
3. `REG_R003`
4. `REG_R004`

---

## 4.6 仿真样例

### 样例 A：正常注册

```json
{
  "eventId": "E_REG_0002",
  "traceId": "T_REG_0002",
  "sceneCode": "REGISTER_ANTI_FRAUD",
  "eventCode": "REGISTER_EVENT",
  "eventTime": "2026-03-07T10:30:00",
  "userId": "U3002",
  "deviceId": "D_REG_002",
  "ip": "2.2.2.2",
  "channel": "APP",
  "mobile": "13900002222",
  "registerResult": "SUCCESS"
}
```

期望结果：

- `PASS`

---

### 样例 B：同设备批量注册

```json
{
  "eventId": "E_REG_0003",
  "traceId": "T_REG_0003",
  "sceneCode": "REGISTER_ANTI_FRAUD",
  "eventCode": "REGISTER_EVENT",
  "eventTime": "2026-03-07T10:35:00",
  "userId": "U3999",
  "deviceId": "D_REG_001",
  "ip": "22.33.44.55",
  "channel": "APP",
  "mobile": "13800009999",
  "registerResult": "SUCCESS"
}
```

上下文假设：

- `device_register_user_cnt_1h = 4`

期望结果：

- 命中 `REG_R003`
- 最终 `REVIEW`

---

# 五、场景三：交易风控

---

## 5.1 业务目标

交易风控是整套平台中最有代表性的场景，因为它能体现：

- 实时聚合
- 金额类特征
- 设备/用户多维关联
- 名单 + 画像 + 行为联动
- FIRST\_HIT / SCORE\_CARD 两种策略模式

这也是你最适合拿来做核心展示的场景。

---

## 5.2 事件定义

### 标准事件编码

- `TRADE_EVENT`

### 样例事件 JSON

```json
{
  "eventId": "E_TRADE_0001",
  "traceId": "T_TRADE_0001",
  "sceneCode": "TRADE_RISK",
  "eventCode": "TRADE_EVENT",
  "eventTime": "2026-03-07T11:00:00",
  "userId": "U5001",
  "deviceId": "D5001",
  "ip": "66.77.88.99",
  "channel": "APP",
  "amount": 6800,
  "currency": "CNY",
  "tradeResult": "SUCCESS",
  "merchantId": "M1001",
  "payMethod": "CARD",
  "ext": {
    "city": "Shanghai"
  }
}
```

---

## 5.3 推荐特征

### 流式特征

1. `user_trade_cnt_5m`
   - 用户 5 分钟交易次数
   - aggType: `COUNT`
   - entity: `userId`
   - filterExpr: `tradeResult == 'SUCCESS'`

2. `user_trade_amt_sum_30m`
   - 用户 30 分钟交易金额和
   - aggType: `SUM`
   - valueExpr: `amount`
   - entity: `userId`

3. `device_bind_user_cnt_1h`
   - 设备 1 小时关联用户数
   - aggType: `DISTINCT_COUNT`
   - valueExpr: `userId`
   - entity: `deviceId`

4. `ip_trade_cnt_10m`
   - IP 10 分钟交易次数
   - aggType: `COUNT`
   - entity: `ip`

### lookup 特征

5. `device_in_blacklist`
6. `user_risk_level`
7. `merchant_risk_level`
8. `user_in_white_list`

### 派生特征

9. `high_amt_flag`

```latex
amount >= 5000
```

10. `trade_burst_flag`

```latex
user_trade_cnt_5m >= 3 && amount >= 5000
```

11. `high_device_risk_flag`

```latex
device_bind_user_cnt_1h >= 4
```

---

## 5.4 推荐规则（FIRST\_HIT 版本）

### 规则 1：白名单用户直接放行

- ruleCode: `TRADE_R001`
- expr:

```latex
user_in_white_list == true
```

- action: `PASS`
- priority: 110

---

### 规则 2：黑名单设备直接拒绝

- ruleCode: `TRADE_R002`
- expr:

```latex
device_in_blacklist == true
```

- action: `REJECT`
- priority: 100

---

### 规则 3：短时高频大额交易

- ruleCode: `TRADE_R003`
- expr:

```latex
user_trade_cnt_5m >= 3 && amount >= 5000
```

- action: `REVIEW`
- priority: 90

---

### 规则 4：高风险用户 + 高风险设备

- ruleCode: `TRADE_R004`
- expr:

```latex
device_bind_user_cnt_1h >= 4 && (user_risk_level == 'M' || user_risk_level == 'H')
```

- action: `REJECT`
- priority: 95

---

### 规则 5：商户高风险且交易频繁

- ruleCode: `TRADE_R005`
- expr:

```latex
merchant_risk_level == 'HIGH' && ip_trade_cnt_10m >= 10
```

- action: `REVIEW`
- priority: 85

---

## 5.5 推荐策略（FIRST\_HIT）

### 策略名

- `TRADE_RISK_POLICY_FIRST_HIT`

### 决策模式

- `FIRST_HIT`

### 执行顺序

1. `TRADE_R001`
2. `TRADE_R002`
3. `TRADE_R004`
4. `TRADE_R003`
5. `TRADE_R005`

### 默认动作

- `PASS`

---

## 5.6 推荐策略（SCORE\_CARD 版本）

如果你后面要演示评分卡模式，可以配这样一套。

### 规则分值

- `TRADE_SCORE_R001`：黑名单设备 → 100
- `TRADE_SCORE_R002`：高频大额交易 → 60
- `TRADE_SCORE_R003`：设备多账号 → 50
- `TRADE_SCORE_R004`：高风险用户 → 40
- `TRADE_SCORE_R005`：高风险商户 → 30

### 收敛规则

- 总分 < 40： `PASS`
- 40 <= 总分 < 80： `REVIEW`
- 总分 >= 80： `REJECT`

---

## 5.7 仿真样例

### 样例 A：正常交易

```json
{
  "eventId": "E_TRADE_0002",
  "traceId": "T_TRADE_0002",
  "sceneCode": "TRADE_RISK",
  "eventCode": "TRADE_EVENT",
  "eventTime": "2026-03-07T11:10:00",
  "userId": "U5002",
  "deviceId": "D5002",
  "ip": "3.3.3.3",
  "channel": "APP",
  "amount": 88,
  "currency": "CNY",
  "tradeResult": "SUCCESS",
  "merchantId": "M1002",
  "payMethod": "CARD"
}
```

期望结果：

- `PASS`

---

### 样例 B：高频大额交易

```json
{
  "eventId": "E_TRADE_0003",
  "traceId": "T_TRADE_0003",
  "sceneCode": "TRADE_RISK",
  "eventCode": "TRADE_EVENT",
  "eventTime": "2026-03-07T11:15:00",
  "userId": "U5001",
  "deviceId": "D5001",
  "ip": "66.77.88.99",
  "channel": "APP",
  "amount": 6800,
  "currency": "CNY",
  "tradeResult": "SUCCESS",
  "merchantId": "M1001",
  "payMethod": "CARD"
}
```

上下文假设：

- `user_trade_cnt_5m = 4`
- `device_in_blacklist = false`
- `device_bind_user_cnt_1h = 2`
- `user_risk_level = L`

期望结果：

- 命中 `TRADE_R003`
- 最终 `REVIEW`

---

### 样例 C：黑名单设备交易

```json
{
  "eventId": "E_TRADE_0004",
  "traceId": "T_TRADE_0004",
  "sceneCode": "TRADE_RISK",
  "eventCode": "TRADE_EVENT",
  "eventTime": "2026-03-07T11:20:00",
  "userId": "U5003",
  "deviceId": "D_BLACK_001",
  "ip": "77.88.99.11",
  "channel": "APP",
  "amount": 1200,
  "currency": "CNY",
  "tradeResult": "SUCCESS",
  "merchantId": "M1003",
  "payMethod": "CARD"
}
```

上下文假设：

- `device_in_blacklist = true`

期望结果：

- 命中 `TRADE_R002`
- 最终 `REJECT`

---

### 样例 D：高风险用户 + 多账号设备

```json
{
  "eventId": "E_TRADE_0005",
  "traceId": "T_TRADE_0005",
  "sceneCode": "TRADE_RISK",
  "eventCode": "TRADE_EVENT",
  "eventTime": "2026-03-07T11:25:00",
  "userId": "U5004",
  "deviceId": "D_MULTI_001",
  "ip": "99.88.77.66",
  "channel": "APP",
  "amount": 2600,
  "currency": "CNY",
  "tradeResult": "SUCCESS",
  "merchantId": "M1004",
  "payMethod": "CARD"
}
```

上下文假设：

- `device_bind_user_cnt_1h = 5`
- `user_risk_level = H`

期望结果：

- 命中 `TRADE_R004`
- 最终 `REJECT`

---

# 六、如何把这些样例用于你的项目

---

## 6.1 用于初始化数据

你可以把本附录里的：

- 场景
- 特征
- 规则
- 策略
- 仿真事件

做成初始化 SQL 或 JSON seed data。

这样你的项目启动后就不是一个空平台，而是：

- 已经有 3 个场景
- 已经有 10+ 规则
- 已经有可直接跑通的仿真样例

这会非常利于演示和开源体验。

---

## 6.2 用于 README 演示

README 可以直接写：

1. 初始化环境
2. 导入样例规则
3. 发送一条 `TRADE_RISK` 事件
4. 查看命中结果
5. 修改规则阈值
6. 重新发布
7. 再次仿真

这种展示会很有说服力。

---

## 6.3 用于仿真测试页

在控制平台中，你可以把这些样例直接做成：

- 可选模板
- 一键填充事件 JSON
- 一键执行仿真

这会让你的仿真页非常好用。

---

## 6.4 用于 Kafka mock 事件生成器

你可以做一个简单数据生成器：

- 正常登录流量
- 高频失败登录流量
- 正常注册流量
- 批量注册流量
- 正常交易流量
- 高频大额交易流量

这样就能很方便做联调、压测、演示。

---

# 七、本附录小结

本附录给出了三大核心场景的样例：

- 登录风控
- 注册反作弊
- 交易风控

并且每个场景都包含：

- 业务目标
- 事件定义
- 推荐特征
- 推荐规则
- 推荐策略
- 仿真样例

这些内容的价值在于：

1. 帮你快速初始化项目
2. 帮你快速完成仿真页和 demo
3. 帮你在 README 和面试中更容易展示系统价值
4. 帮你把“平台能力”落到具体业务上

如果后续你愿意，我还可以继续帮你把这些样例进一步整理成：

- 初始化 SQL
- 初始化 JSON Seed 文件
- mock Kafka 事件脚本
- 仿真测试用例清单
- README 演示脚本
