<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
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
          <el-form-item label="事件编码" prop="eventCode">
            <el-input
              v-model="formData.eventCode"
              :disabled="formType === 'update'"
              placeholder="请输入事件编码，例如 TRADE_EVENT"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="事件名称" prop="eventName">
            <el-input v-model="formData.eventName" placeholder="请输入事件名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="事件类别" prop="eventType">
            <el-select v-model="formData.eventType" class="w-full" placeholder="请选择事件类别">
              <el-option
                v-for="item in riskEventSchemaTypeOptions"
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
          <el-form-item label="接入方式" prop="sourceType">
            <el-select v-model="formData.sourceType" class="w-full" placeholder="请选择接入方式">
              <el-option
                v-for="item in riskEventSchemaSourceTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="标准 Topic" prop="standardTopicName">
            <el-input v-model="formData.standardTopicName" placeholder="例如 pulsix.event.standard" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="原始 Topic" prop="rawTopicName">
        <el-input v-model="formData.rawTopicName" placeholder="例如 pulsix.event.raw.trade；HTTP 场景可为空" />
      </el-form-item>

      <el-form-item label="模型说明" prop="description">
        <el-input v-model="formData.description" :rows="4" type="textarea" placeholder="请输入事件模型说明" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :loading="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as EventSchemaApi from '@/api/risk/eventSchema'
import { riskEventSchemaSourceTypeOptions, riskEventSchemaTypeOptions } from './constants'

defineOptions({ name: 'RiskEventSchemaForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')

const createDefaultFormData = (): EventSchemaApi.EventSchemaVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  eventCode: '',
  eventName: '',
  eventType: 'BUSINESS',
  sourceType: 'MIXED',
  rawTopicName: '',
  standardTopicName: '',
  description: ''
})

const formData = ref<EventSchemaApi.EventSchemaVO>(createDefaultFormData())
const formRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  eventCode: [
    { required: true, message: '事件编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: '事件编码只允许字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  eventName: [{ required: true, message: '事件名称不能为空', trigger: 'blur' }],
  eventType: [{ required: true, message: '事件类别不能为空', trigger: 'change' }],
  sourceType: [{ required: true, message: '接入方式不能为空', trigger: 'change' }]
})
const formRef = ref()

const resetForm = () => {
  formData.value = createDefaultFormData()
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
      formData.value = await EventSchemaApi.getEventSchema(id)
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
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await EventSchemaApi.createEventSchema(data)
      message.success(t('common.createSuccess'))
    } else {
      await EventSchemaApi.updateEventSchema(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
