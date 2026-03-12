<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="920px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-alert
        title="S09 先只支持 Redis Set / Redis Hash；sourceRef 分别对应前缀 Key/Hash Key。"
        type="info"
        :closable="false"
        class="mb-16px"
      />

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="所属场景" prop="sceneCode">
            <el-input
              v-model="formData.sceneCode"
              placeholder="请输入所属场景编码"
              :disabled="formType === 'update'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="特征编码" prop="featureCode">
            <el-input
              v-model="formData.featureCode"
              placeholder="请输入特征编码"
              :disabled="formType === 'update'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="特征名称" prop="featureName">
            <el-input v-model="formData.featureName" placeholder="请输入特征名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="查询类型" prop="lookupType">
            <el-select v-model="formData.lookupType" class="w-full" placeholder="请选择查询类型">
              <el-option
                v-for="item in riskFeatureLookupTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Key 表达式" prop="keyExpr">
            <el-input v-model="formData.keyExpr" :placeholder="keyExprPlaceholder" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="来源引用" prop="sourceRef">
            <el-input v-model="formData.sourceRef" :placeholder="sourceRefPlaceholder" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="值类型" prop="valueType">
            <el-select
              v-model="formData.valueType"
              class="w-full"
              placeholder="请选择值类型"
              :disabled="formData.lookupType === 'REDIS_SET'"
            >
              <el-option
                v-for="item in riskFeatureLookupValueTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="默认值" prop="defaultValue">
            <el-input v-model="formData.defaultValue" :placeholder="defaultValuePlaceholder" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="超时(ms)" prop="timeoutMs">
            <el-input-number v-model="formData.timeoutMs" class="!w-full" :min="1" :precision="0" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="缓存 TTL(s)" prop="cacheTtlSeconds">
            <el-input-number v-model="formData.cacheTtlSeconds" class="!w-full" :min="1" :precision="0" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-select v-model="formData.status" class="w-full" placeholder="请选择状态">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="扩展配置 JSON">
            <el-input
              v-model="extraJsonText"
              type="textarea"
              :rows="5"
              placeholder="可选，例如 {&#10;  &quot;keyMode&quot;: &quot;PREFIX_KEY&quot;&#10;}"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="口径说明" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="4"
              placeholder="建议写清楚 lookup 来源、默认值策略与超时边界"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button :disabled="formLoading" @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">确 定</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as FeatureLookupApi from '@/api/risk/featureLookup'
import { riskFeatureLookupTypeOptions, riskFeatureLookupValueTypeOptions } from './constants'

defineOptions({ name: 'RiskFeatureLookupForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const extraJsonText = ref('')

const createDefaultFormData = (): FeatureLookupApi.FeatureLookupVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  featureCode: '',
  featureName: '',
  lookupType: 'REDIS_SET',
  keyExpr: 'deviceId',
  sourceRef: 'pulsix:list:black:device',
  defaultValue: 'false',
  valueType: 'BOOLEAN',
  cacheTtlSeconds: 30,
  timeoutMs: 20,
  extraJson: undefined,
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<FeatureLookupApi.FeatureLookupVO>(createDefaultFormData())
const formRef = ref()

const keyExprPlaceholder = computed(() => {
  return formData.value.lookupType === 'REDIS_SET' ? '例如 deviceId' : '例如 userId'
})

const sourceRefPlaceholder = computed(() => {
  return formData.value.lookupType === 'REDIS_SET' ? '例如 pulsix:list:black:device' : '例如 pulsix:profile:user:risk'
})

const defaultValuePlaceholder = computed(() => {
  return formData.value.lookupType === 'REDIS_SET' ? '例如 false' : '例如 L / LOW / 0'
})

const formRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  featureCode: [
    { required: true, message: '特征编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: '特征编码只允许字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  featureName: [{ required: true, message: '特征名称不能为空', trigger: 'blur' }],
  lookupType: [{ required: true, message: '查询类型不能为空', trigger: 'change' }],
  keyExpr: [{ required: true, message: 'Key 表达式不能为空', trigger: 'blur' }],
  sourceRef: [{ required: true, message: '来源引用不能为空', trigger: 'blur' }],
  valueType: [{ required: true, message: '值类型不能为空', trigger: 'change' }],
  timeoutMs: [{ required: true, message: '超时时间不能为空', trigger: 'change' }],
  cacheTtlSeconds: [{ required: true, message: '缓存 TTL 不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const resetForm = () => {
  formData.value = createDefaultFormData()
  extraJsonText.value = ''
  formRef.value?.resetFields()
}

const applyLookupTypeDefaults = () => {
  if (formData.value.lookupType === 'REDIS_SET') {
    formData.value.valueType = 'BOOLEAN'
    if (formType.value === 'create') {
      formData.value.keyExpr = 'deviceId'
      formData.value.sourceRef = 'pulsix:list:black:device'
      formData.value.defaultValue = 'false'
    }
  } else if (formType.value === 'create') {
    formData.value.valueType = 'STRING'
    formData.value.keyExpr = 'userId'
    formData.value.sourceRef = 'pulsix:profile:user:risk'
    formData.value.defaultValue = 'L'
  }
}

watch(
  () => formData.value.lookupType,
  () => {
    if (!dialogVisible.value) {
      return
    }
    applyLookupTypeDefaults()
  }
)

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await FeatureLookupApi.getFeatureLookup(id)
      extraJsonText.value = formData.value.extraJson ? JSON.stringify(formData.value.extraJson, null, 2) : ''
    } finally {
      formLoading.value = false
    }
  } else {
    applyLookupTypeDefaults()
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  let extraJson: Record<string, any> | undefined
  const extraJsonRaw = extraJsonText.value.trim()
  if (extraJsonRaw) {
    try {
      extraJson = JSON.parse(extraJsonRaw)
    } catch {
      message.error('扩展配置 JSON 格式不正确')
      return
    }
  }

  formLoading.value = true
  try {
    const data: FeatureLookupApi.FeatureLookupVO = {
      ...formData.value,
      defaultValue: formData.value.defaultValue?.trim() || undefined,
      extraJson,
      description: formData.value.description?.trim() || undefined
    }
    if (formType.value === 'create') {
      await FeatureLookupApi.createFeatureLookup(data)
      message.success(t('common.createSuccess'))
    } else {
      await FeatureLookupApi.updateFeatureLookup(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
