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
          <el-form-item label="所属场景" prop="sceneCode">
            <el-input
              v-model="formData.sceneCode"
              :disabled="formType === 'update'"
              placeholder="请输入所属场景编码，例如 TRADE_RISK"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属事件" prop="eventCode">
            <el-input
              v-model="formData.eventCode"
              :disabled="formType === 'update'"
              placeholder="请输入所属事件编码，例如 TRADE_EVENT"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="样例编码" prop="sampleCode">
            <el-input
              v-model="formData.sampleCode"
              :disabled="formType === 'update'"
              placeholder="请输入样例编码，例如 TRADE_STD_SUCCESS"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="样例名称" prop="sampleName">
            <el-input v-model="formData.sampleName" placeholder="请输入样例名称" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="样例类型" prop="sampleType">
            <el-select v-model="formData.sampleType" class="w-full" placeholder="请选择样例类型">
              <el-option
                v-for="item in riskEventSampleTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="来源编码" prop="sourceCode">
            <el-input v-model="formData.sourceCode" placeholder="RAW 样例可填写接入源编码；当前阶段可为空" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="排序号" prop="sortNo">
            <el-input-number v-model="formData.sortNo" :min="0" class="!w-full" controls-position="right" />
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

      <el-form-item label="样例说明" prop="description">
        <el-input v-model="formData.description" :rows="3" type="textarea" placeholder="请输入样例说明" />
      </el-form-item>

      <el-form-item label="样例 JSON" prop="sampleJson" required>
        <div class="w-full">
          <JsonEditor v-model="formData.sampleJson" mode="code" height="380px" @error="onJsonError" />
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
import * as EventSampleApi from '@/api/risk/eventSample'
import { riskEventSampleTypeOptions } from './constants'

defineOptions({ name: 'RiskEventSampleForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const hasJsonError = ref(false)

const createDefaultFormData = (): EventSampleApi.EventSampleVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  eventCode: 'TRADE_EVENT',
  sampleCode: '',
  sampleName: '',
  sampleType: 'STANDARD',
  sourceCode: '',
  sampleJson: {},
  description: '',
  sortNo: 0,
  status: CommonStatusEnum.ENABLE
})

const formData = ref<EventSampleApi.EventSampleVO>(createDefaultFormData())
const formRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  eventCode: [{ required: true, message: '所属事件不能为空', trigger: 'blur' }],
  sampleCode: [
    { required: true, message: '样例编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: '样例编码只允许字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  sampleName: [{ required: true, message: '样例名称不能为空', trigger: 'blur' }],
  sampleType: [{ required: true, message: '样例类型不能为空', trigger: 'change' }],
  sortNo: [{ required: true, message: '排序号不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
  sampleJson: [
    {
      validator: (_rule, value, callback) => {
        if (!value || typeof value !== 'object') {
          callback(new Error('样例 JSON 不能为空'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
})
const formRef = ref()

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
  if (id) {
    formLoading.value = true
    try {
      formData.value = await EventSampleApi.getEventSample(id)
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
    const data = formData.value
    if (formType.value === 'create') {
      await EventSampleApi.createEventSample(data)
      message.success(t('common.createSuccess'))
    } else {
      await EventSampleApi.updateEventSample(data)
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
