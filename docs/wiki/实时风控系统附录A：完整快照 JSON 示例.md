本附录给出一份**可直接作为运行态配置产物参考模板**的 Snapshot 示例，目标是让控制平台发布后的数据结构尽可能自包含、去关系化、易于 Flink 消费。

## 1. 快照设计目标

一份合格的运行时快照应满足：

- **自包含**：Flink 拿到快照后不需要再查设计态多表
- **去关系化**：场景、特征、规则、策略关系已在发布时展开
- **可恢复**：可直接进入 Broadcast State
- **可追溯**：能够明确标识 scene、version、checksum、发布时间
- **可编译**：表达式和 Groovy 可在 Flink 本地预编译

## 2. 完整快照 JSON 示例

```json
{
  "snapshotId": "TRADE_RISK_v12",
  "sceneCode": "TRADE_RISK",
  "sceneName": "交易风控",
  "version": 12,
  "status": "ACTIVE",
  "checksum": "8d2041a7cf8f47b4b6b0f91d2ab8d9d0",
  "publishedAt": "2026-03-07T20:00:00Z",
  "effectiveFrom": "2026-03-07T20:00:10Z",
  "runtimeMode": "ASYNC_DECISION",

  "scene": {
    "defaultPolicyCode": "TRADE_RISK_POLICY",
    "allowedEventTypes": ["trade"],
    "decisionTimeoutMs": 500,
    "logLevel": "FULL"
  },

  "eventSchema": {
    "eventCode": "TRADE_EVENT",
    "eventCode": "TRADE_EVENT",
    "requiredFields": [
      "eventId",
      "sceneCode",
      "eventCode",
      "eventTime",
      "userId",
      "deviceId",
      "ip",
      "amount",
      "result"
    ],
    "optionalFields": [
      "merchantId",
      "channel",
      "province",
      "city",
      "ext"
    ]
  },

  "variables": {
    "baseFields": [
      "eventId",
      "sceneCode",
      "eventCode",
      "eventTime",
      "traceId",
      "userId",
      "deviceId",
      "ip",
      "amount",
      "result",
      "merchantId",
      "channel",
      "province",
      "city"
    ]
  },

  "streamFeatures": [
    {
      "code": "user_trade_cnt_5m",
      "name": "用户5分钟交易次数",
      "sourceEventCodes": ["TRADE_EVENT"],
      "entityType": "USER",
      "entityKeyExpr": "userId",
      "aggType": "COUNT",
      "valueExpr": "1",
      "filterExpr": "result == 'SUCCESS'",
      "windowType": "SLIDING",
      "windowSize": "5m",
      "windowSlide": "1m",
      "includeCurrentEvent": true,
      "ttl": "10m",
      "valueType": "LONG"
    },
    {
      "code": "user_trade_amt_sum_30m",
      "name": "用户30分钟交易金额和",
      "sourceEventCodes": ["TRADE_EVENT"],
      "entityType": "USER",
      "entityKeyExpr": "userId",
      "aggType": "SUM",
      "valueExpr": "amount",
      "filterExpr": "result == 'SUCCESS'",
      "windowType": "SLIDING",
      "windowSize": "30m",
      "windowSlide": "1m",
      "includeCurrentEvent": true,
      "ttl": "40m",
      "valueType": "DECIMAL"
    },
    {
      "code": "device_bind_user_cnt_1h",
      "name": "设备1小时关联用户数",
      "sourceEventCodes": ["TRADE_EVENT", "LOGIN_EVENT"],
      "entityType": "DEVICE",
      "entityKeyExpr": "deviceId",
      "aggType": "DISTINCT_COUNT",
      "valueExpr": "userId",
      "filterExpr": "deviceId != null && userId != null",
      "windowType": "SLIDING",
      "windowSize": "1h",
      "windowSlide": "5m",
      "includeCurrentEvent": true,
      "ttl": "2h",
      "valueType": "LONG"
    }
  ],

  "lookupFeatures": [
    {
      "code": "device_in_blacklist",
      "name": "设备是否命中黑名单",
      "lookupType": "REDIS_SET",
      "keyExpr": "deviceId",
      "sourceRef": "pulsix:list:black:device",
      "defaultValue": false,
      "valueType": "BOOLEAN",
      "timeoutMs": 20,
      "cacheTtlSeconds": 30
    },
    {
      "code": "user_risk_level",
      "name": "用户风险等级",
      "lookupType": "REDIS_HASH",
      "keyExpr": "userId",
      "sourceRef": "pulsix:profile:user:risk",
      "defaultValue": "L",
      "valueType": "STRING",
      "timeoutMs": 20,
      "cacheTtlSeconds": 30
    }
  ],

  "derivedFeatures": [
    {
      "code": "high_amt_flag",
      "name": "高金额标记",
      "engineType": "AVIATOR",
      "expr": "amount >= 5000",
      "dependsOn": ["amount"],
      "valueType": "BOOLEAN"
    },
    {
      "code": "trade_burst_flag",
      "name": "短时高频交易标记",
      "engineType": "AVIATOR",
      "expr": "user_trade_cnt_5m >= 3 && amount >= 5000",
      "dependsOn": ["user_trade_cnt_5m", "amount"],
      "valueType": "BOOLEAN"
    }
  ],

  "rules": [
    {
      "code": "R001",
      "name": "黑名单设备直接拒绝",
      "engineType": "AVIATOR",
      "priority": 100,
      "whenExpr": "device_in_blacklist == true",
      "dependsOn": ["device_in_blacklist"],
      "hitAction": "REJECT",
      "riskScore": 100,
      "hitReasonTemplate": "设备命中黑名单"
    },
    {
      "code": "R002",
      "name": "大额且短时高频交易",
      "engineType": "AVIATOR",
      "priority": 90,
      "whenExpr": "trade_burst_flag == true",
      "dependsOn": ["trade_burst_flag"],
      "hitAction": "REVIEW",
      "riskScore": 60,
      "hitReasonTemplate": "用户5分钟交易次数={user_trade_cnt_5m}, 当前金额={amount}"
    },
    {
      "code": "R003",
      "name": "高风险用户多账号设备",
      "engineType": "GROOVY",
      "priority": 80,
      "whenExpr": "return device_bind_user_cnt_1h >= 4 && ['M','H'].contains(user_risk_level)",
      "dependsOn": ["device_bind_user_cnt_1h", "user_risk_level"],
      "hitAction": "REJECT",
      "riskScore": 80,
      "hitReasonTemplate": "设备1小时关联用户数={device_bind_user_cnt_1h}, 用户风险等级={user_risk_level}"
    }
  ],

  "policy": {
    "policyCode": "TRADE_RISK_POLICY",
    "policyName": "交易风控主策略",
    "decisionMode": "FIRST_HIT",
    "defaultAction": "PASS",
    "ruleOrder": ["R001", "R002", "R003"]
  },

  "runtimeHints": {
    "requiredStreamFeatures": [
      "user_trade_cnt_5m",
      "user_trade_amt_sum_30m",
      "device_bind_user_cnt_1h"
    ],
    "requiredLookupFeatures": [
      "device_in_blacklist",
      "user_risk_level"
    ],
    "requiredDerivedFeatures": [
      "high_amt_flag",
      "trade_burst_flag"
    ],
    "maxRuleExecutionCount": 100,
    "allowGroovy": true,
    "needFullDecisionLog": true
  }
}
```

## 3. 字段解释

### 3.1 顶层元信息

- `snapshotId`：推荐用 `sceneCode_version` 形式，便于日志与调试
- `sceneCode`：场景主键，Flink 常按它定位活跃快照
- `version`：该场景的稳定运行版本号
- `checksum`：用于幂等判断与一致性校验
- `publishedAt / effectiveFrom`：区分发布时间与真正生效时间

### 3.2 scene / eventSchema

- `scene` 描述当前场景整体约束和默认策略
- `eventSchema` 用于运行时最基本的事件合法性判断

### 3.3 streamFeatures

这类特征需要 Flink State 支撑，通常包括：

- `COUNT`
- `SUM`
- `MAX`
- `LATEST`
- `DISTINCT_COUNT`

其中：

- `entityKeyExpr` 决定按哪个实体维度聚合
- `filterExpr` 决定哪些事件参与计算
- `windowSize / windowSlide` 决定窗口语义
- `includeCurrentEvent` 决定当前事件是否参与本次决策值计算

### 3.4 lookupFeatures

这类特征通常依赖 Redis 或在线画像：

- 黑名单命中
- 白名单命中
- 用户风险等级
- 设备标签

### 3.5 derivedFeatures

这类特征属于“计算后再派生的上下文变量”，适合表达式引擎或少量 Groovy：

- 高金额标识
- 高频交易标识
- 自定义复合布尔变量

### 3.6 rules

规则是在完整上下文之上做布尔判断，并附带：

- 命中动作
- 风险分
- 命中原因模板
- 优先级

### 3.7 policy

策略负责多条规则的收敛：

- `FIRST_HIT`：按顺序命中即返回
- `SCORE_CARD`：累计分值再按区间决策

### 3.8 runtimeHints

这是给运行时引擎的提示信息，不属于纯业务语义，但对性能和执行规划很有帮助。

## 4. 推荐的发布后检查项

生成快照后，控制平台建议再做一次发布后检查：

- 同一场景是否存在重复 feature code
- 规则依赖变量是否全部可达
- Groovy 规则是否仅出现在允许的场景/环境
- required\* 列表是否与 rule 依赖分析结果一致
- `ruleOrder` 是否覆盖全部启用规则

## 5. 推荐的演进方式

一期建议：

- 只支持固定结构快照
- stream feature 仅支持 COUNT / SUM / MAX / LATEST / DISTINCT\_COUNT(基础版)
- rule engine 以表达式为主，Groovy 为补充

二期可扩展：

- 灰度发布字段
- 回滚来源链路字段
- 多策略分支字段
- 更丰富的 runtime hints
- CEP/序列规则描述
