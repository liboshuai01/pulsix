<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="860px">
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
          <el-form-item label="字段编码" prop="fieldCode">
            <el-input
              v-model="formData.fieldCode"
              :disabled="formType === 'update'"
              placeholder="请输入字段编码，例如 userId"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="字段名称" prop="fieldName">
            <el-input v-model="formData.fieldName" placeholder="请输入字段名称" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="字段类型" prop="fieldType">
            <el-select v-model="formData.fieldType" class="w-full" placeholder="请选择字段类型">
              <el-option
                v-for="item in riskEventFieldTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序号" prop="sortNo">
            <el-input-number v-model="formData.sortNo" :min="0" class="!w-full" controls-position="right" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="字段路径" prop="fieldPath">
        <el-input v-model="formData.fieldPath" placeholder="默认自动补成 $.字段编码，例如 $.userId" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="标准字段" prop="standardFieldFlag">
            <el-switch v-model="formData.standardFieldFlag" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="是否必填" prop="requiredFlag">
            <el-switch v-model="formData.requiredFlag" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="允许为空" prop="nullableFlag">
            <el-switch v-model="formData.nullableFlag" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="默认值" prop="defaultValue">
            <el-input v-model="formData.defaultValue" placeholder="请输入默认值，可为空" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="示例值" prop="sampleValue">
            <el-input v-model="formData.sampleValue" placeholder="请输入示例值，例如 U1001" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="字段说明" prop="description">
        <el-input v-model="formData.description" :rows="4" type="textarea" placeholder="请输入字段说明" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :loading="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as EventFieldApi from '@/api/risk/eventField'
import { riskEventFieldTypeOptions } from './constants'

defineOptions({ name: 'RiskEventFieldForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')

const createDefaultFormData = (): EventFieldApi.EventFieldVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  eventCode: 'TRADE_EVENT',
  fieldCode: '',
  fieldName: '',
  fieldType: 'STRING',
  fieldPath: '',
  standardFieldFlag: 0,
  requiredFlag: 0,
  nullableFlag: 1,
  defaultValue: '',
  sampleValue: '',
  description: '',
  sortNo: 0
})

const formData = ref<EventFieldApi.EventFieldVO>(createDefaultFormData())
const formRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  eventCode: [{ required: true, message: '所属事件不能为空', trigger: 'blur' }],
  fieldCode: [
    { required: true, message: '字段编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: '字段编码只允许字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  fieldName: [{ required: true, message: '字段名称不能为空', trigger: 'blur' }],
  fieldType: [{ required: true, message: '字段类型不能为空', trigger: 'change' }],
  sortNo: [{ required: true, message: '排序号不能为空', trigger: 'blur' }]
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
      formData.value = await EventFieldApi.getEventField(id)
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
      await EventFieldApi.createEventField(data)
      message.success(t('common.createSuccess'))
    } else {
      await EventFieldApi.updateEventField(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
