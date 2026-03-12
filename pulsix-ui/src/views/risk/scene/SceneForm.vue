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
          <el-form-item label="场景编码" prop="sceneCode">
            <el-input
              v-model="formData.sceneCode"
              :disabled="formType === 'update'"
              placeholder="请输入场景编码，例如 TRADE_RISK"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="场景名称" prop="sceneName">
            <el-input v-model="formData.sceneName" placeholder="请输入场景名称" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="场景类型" prop="sceneType">
            <el-select v-model="formData.sceneType" class="w-full" placeholder="请选择场景类型">
              <el-option
                v-for="item in riskSceneTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="接入模式" prop="accessMode">
            <el-select v-model="formData.accessMode" class="w-full" placeholder="请选择接入模式">
              <el-option
                v-for="item in riskSceneAccessModeOptions"
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
          <el-form-item label="默认事件编码" prop="defaultEventCode">
            <el-input v-model="formData.defaultEventCode" placeholder="例如 TRADE_EVENT" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="默认策略编码" prop="defaultPolicyCode">
            <el-input v-model="formData.defaultPolicyCode" placeholder="例如 TRADE_RISK_POLICY" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="标准事件 Topic" prop="standardTopicName">
            <el-input v-model="formData.standardTopicName" placeholder="例如 pulsix.event.standard" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="决策结果 Topic" prop="decisionTopicName">
            <el-input v-model="formData.decisionTopicName" placeholder="例如 pulsix.decision.result" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="场景说明" prop="description">
        <el-input v-model="formData.description" :rows="4" type="textarea" placeholder="请输入场景说明" />
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
import * as SceneApi from '@/api/risk/scene'
import { riskSceneAccessModeOptions, riskSceneTypeOptions } from './constants'

defineOptions({ name: 'RiskSceneForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')

const createDefaultFormData = (): SceneApi.SceneVO => ({
  id: undefined,
  sceneCode: '',
  sceneName: '',
  sceneType: 'GENERAL',
  accessMode: 'MIXED',
  defaultEventCode: '',
  defaultPolicyCode: '',
  standardTopicName: '',
  decisionTopicName: '',
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<SceneApi.SceneVO>(createDefaultFormData())
const formRules = reactive({
  sceneCode: [{ required: true, message: '场景编码不能为空', trigger: 'blur' }],
  sceneName: [{ required: true, message: '场景名称不能为空', trigger: 'blur' }],
  sceneType: [{ required: true, message: '场景类型不能为空', trigger: 'change' }],
  accessMode: [{ required: true, message: '接入模式不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
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
      formData.value = await SceneApi.getScene(id)
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
      await SceneApi.createScene(data)
      message.success(t('common.createSuccess'))
    } else {
      await SceneApi.updateScene(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
