<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="980px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="来源编码" prop="sourceCode">
            <el-input
              v-model="formData.sourceCode"
              :disabled="formType === 'update'"
              placeholder="请输入来源编码，例如 trade_http_demo"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="来源名称" prop="sourceName">
            <el-input v-model="formData.sourceName" placeholder="请输入来源名称" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="接入方式" prop="sourceType">
            <el-select v-model="formData.sourceType" class="w-full" placeholder="请选择接入方式">
              <el-option
                v-for="item in riskIngestSourceTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="鉴权方式" prop="authType">
            <el-select v-model="formData.authType" class="w-full" placeholder="请选择鉴权方式">
              <el-option
                v-for="item in riskIngestSourceAuthTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="标准 Topic" prop="standardTopicName">
            <el-input v-model="formData.standardTopicName" placeholder="例如 pulsix.event.standard" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="异常 Topic" prop="errorTopicName">
            <el-input v-model="formData.errorTopicName" placeholder="例如 pulsix.event.dlq" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="限流 QPS" prop="rateLimitQps">
            <el-input-number v-model="formData.rateLimitQps" :min="0" class="!w-full" controls-position="right" />
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
      </el-row>

      <el-form-item label="场景范围" prop="sceneScopeJson">
        <el-select
          v-model="formData.sceneScopeJson"
          class="w-full"
          multiple
          filterable
          allow-create
          default-first-option
          clearable
          collapse-tags
          collapse-tags-tooltip
          placeholder="为空表示不限制场景；可选择已存在场景或直接输入"
        >
          <el-option v-for="item in sceneOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="来源说明" prop="description">
        <el-input v-model="formData.description" :rows="3" type="textarea" placeholder="请输入来源说明" />
      </el-form-item>

      <el-form-item label="鉴权配置" prop="authConfigJson">
        <div class="w-full">
          <JsonEditor v-model="formData.authConfigJson" mode="code" height="300px" @error="onJsonError" />
          <div v-if="hasJsonError" class="mt-8px text-12px text-[var(--el-color-danger)]">
            JSON 格式不正确，请修正后再提交
          </div>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :loading="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import { isEmpty } from '@/utils/is'
import * as IngestSourceApi from '@/api/risk/ingestSource'
import * as SceneApi from '@/api/risk/scene'
import { riskIngestSourceAuthTypeOptions, riskIngestSourceTypeOptions } from './constants'

defineOptions({ name: 'RiskIngestSourceForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const hasJsonError = ref(false)
const sceneOptions = ref<{ label: string; value: string }[]>([])

const createDefaultFormData = (): IngestSourceApi.IngestSourceVO => ({
  id: undefined,
  sourceCode: '',
  sourceName: '',
  sourceType: 'HTTP',
  authType: 'NONE',
  authConfigJson: {},
  sceneScopeJson: [],
  standardTopicName: 'pulsix.event.standard',
  errorTopicName: 'pulsix.event.dlq',
  rateLimitQps: 0,
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<IngestSourceApi.IngestSourceVO>(createDefaultFormData())
const formRules = reactive({
  sourceCode: [
    { required: true, message: '来源编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: '来源编码只允许字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  sourceName: [{ required: true, message: '来源名称不能为空', trigger: 'blur' }],
  sourceType: [{ required: true, message: '接入方式不能为空', trigger: 'change' }],
  authType: [{ required: true, message: '鉴权方式不能为空', trigger: 'change' }],
  standardTopicName: [{ required: true, message: '标准 Topic 不能为空', trigger: 'blur' }],
  errorTopicName: [{ required: true, message: '异常 Topic 不能为空', trigger: 'blur' }],
  rateLimitQps: [{ required: true, message: '限流阈值不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})
const formRef = ref()

const loadSceneOptions = async () => {
  const data = await SceneApi.getScenePage({ pageNo: 1, pageSize: 200 })
  sceneOptions.value = data.list.map((item) => ({ label: `${item.sceneName} (${item.sceneCode})`, value: item.sceneCode }))
}

const resetForm = () => {
  formData.value = createDefaultFormData()
  hasJsonError.value = false
  formRef.value?.resetFields()
}

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await loadSceneOptions()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await IngestSourceApi.getIngestSource(id)
      formData.value.authConfigJson = formData.value.authConfigJson || {}
      formData.value.sceneScopeJson = formData.value.sceneScopeJson || []
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return
  if (hasJsonError.value) {
    message.warning('JSON 格式不正确，请修正后再提交')
    return
  }
  formLoading.value = true
  try {
    const data = {
      ...formData.value,
      sceneScopeJson: formData.value.sceneScopeJson?.filter((item) => !!item) || []
    }
    if (formType.value === 'create') {
      await IngestSourceApi.createIngestSource(data)
      message.success(t('common.createSuccess'))
    } else {
      await IngestSourceApi.updateIngestSource(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const onJsonError = (errors: any) => {
  hasJsonError.value = !isEmpty(errors)
}
</script>
