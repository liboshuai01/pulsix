<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="920px">
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
            <el-select
              v-model="formData.sceneCode"
              class="w-full"
              :disabled="formType === 'update'"
              filterable
              placeholder="请选择所属场景"
            >
              <el-option
                v-for="item in sceneOptions"
                :key="item.sceneCode"
                :label="`${item.sceneName} (${item.sceneCode})`"
                :value="item.sceneCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="名单编码" prop="listCode">
            <el-input
              v-model="formData.listCode"
              :disabled="formType === 'update'"
              placeholder="例如 DEVICE_BLACKLIST"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="名单名称" prop="listName">
            <el-input v-model="formData.listName" placeholder="请输入名单名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="匹配维度" prop="matchType">
            <el-select v-model="formData.matchType" class="w-full" placeholder="请选择匹配维度">
              <el-option
                v-for="item in riskListMatchTypeOptions"
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
          <el-form-item label="名单类型" prop="listType">
            <el-select v-model="formData.listType" class="w-full" placeholder="请选择名单类型">
              <el-option
                v-for="item in riskListTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="运行时存储" prop="storageType">
            <el-select v-model="formData.storageType" class="w-full" placeholder="请选择运行时存储形式">
              <el-option
                v-for="item in riskListStorageTypeOptions"
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
          <el-form-item label="同步模式" prop="syncMode">
            <el-select v-model="formData.syncMode" class="w-full" placeholder="请选择同步模式">
              <el-option
                v-for="item in riskListSyncModeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
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

      <el-form-item label="Redis 前缀">
        <el-input :model-value="redisKeyPrefix" disabled />
      </el-form-item>

      <el-form-item label="名单说明" prop="description">
        <el-input v-model="formData.description" :rows="4" type="textarea" placeholder="请输入名单说明" />
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
import * as RiskListApi from '@/api/risk/list'
import {
  buildRiskListRedisKeyPrefix,
  riskListMatchTypeOptions,
  riskListStorageTypeOptions,
  riskListSyncModeOptions,
  riskListTypeOptions
} from './constants'

defineOptions({ name: 'RiskListSetForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const sceneOptions = ref<SceneApi.SceneVO[]>([])

const createDefaultFormData = (): RiskListApi.ListSetVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  listCode: 'DEVICE_BLACKLIST',
  listName: '设备黑名单',
  matchType: 'DEVICE',
  listType: 'BLACK',
  storageType: 'REDIS_SET',
  syncMode: 'FULL',
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<RiskListApi.ListSetVO>(createDefaultFormData())
const formRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'change' }],
  listCode: [
    { required: true, message: '名单编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: '名单编码只允许字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  listName: [{ required: true, message: '名单名称不能为空', trigger: 'blur' }],
  matchType: [{ required: true, message: '匹配维度不能为空', trigger: 'change' }],
  listType: [{ required: true, message: '名单类型不能为空', trigger: 'change' }],
  storageType: [{ required: true, message: '运行时存储不能为空', trigger: 'change' }],
  syncMode: [{ required: true, message: '同步模式不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})
const formRef = ref()

const redisKeyPrefix = computed(() => {
  return buildRiskListRedisKeyPrefix(formData.value.listCode, formData.value.listType, formData.value.matchType)
})

const loadSceneOptions = async () => {
  const data = await SceneApi.getScenePage({ pageNo: 1, pageSize: 200 })
  sceneOptions.value = data.list
}

const resetForm = () => {
  formData.value = createDefaultFormData()
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
      formData.value = await RiskListApi.getListSet(id)
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
    if (formType.value === 'create') {
      await RiskListApi.createListSet(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await RiskListApi.updateListSet(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
