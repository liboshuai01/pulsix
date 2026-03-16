<template>
  <RiskCenterDialog
    v-model="dialogVisible"
    :title="dialogTitle"
    :fullscreen="false"
    width="760px"
    max-height="calc(100vh - 320px)"
    scroll
  >
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-row :gutter="18">
        <el-col :span="12">
          <el-form-item label="场景名称" prop="sceneName">
            <el-input v-model="formData.sceneName" placeholder="请输入场景名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="场景编码" prop="sceneCode">
            <el-input
              v-model="formData.sceneCode"
              placeholder="请输入场景编码"
              :disabled="formType === 'update'"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="18">
        <el-col :span="12">
          <el-form-item label="运行模式" prop="runtimeMode">
            <el-select v-model="formData.runtimeMode" placeholder="请选择运行模式">
              <el-option
                v-for="item in SCENE_RUNTIME_MODE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="18">
        <el-col :span="12">
          <el-form-item label="默认策略编码" prop="defaultPolicyCode">
            <el-input v-model="formData.defaultPolicyCode" placeholder="请输入默认策略编码" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="18">
        <el-col :span="24">
          <el-form-item v-if="formType === 'create'" label="状态" prop="status">
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
          <el-form-item v-else label="状态">
            <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="formData.status" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="场景描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="4"
          placeholder="请输入场景描述"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :disabled="formLoading" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </RiskCenterDialog>
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import * as SceneApi from '@/api/risk/scene'
import RiskCenterDialog from '../../components/RiskCenterDialog.vue'
import { SCENE_RUNTIME_MODE_OPTIONS } from './constants'
import type { FormRules } from 'element-plus'

defineOptions({ name: 'RiskSceneForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()

const createDefaultFormData = (): SceneApi.SceneVO => ({
  id: undefined,
  sceneCode: '',
  sceneName: '',
  runtimeMode: SCENE_RUNTIME_MODE_OPTIONS[0].value,
  defaultPolicyCode: '',
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<SceneApi.SceneVO>(createDefaultFormData())

const formRules = reactive<FormRules>({
  sceneName: [{ required: true, message: '场景名称不能为空', trigger: 'blur' }],
  sceneCode: [
    { required: true, message: '场景编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Z0-9_]+$/,
      message: '场景编码只能包含大写字母、数字和下划线',
      trigger: 'blur'
    }
  ],
  runtimeMode: [{ required: true, message: '运行模式不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? t('action.create') : t('action.update')
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
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate()
  if (!valid) {
    return
  }
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

const resetForm = () => {
  formData.value = createDefaultFormData()
  formRef.value?.resetFields()
}
</script>
