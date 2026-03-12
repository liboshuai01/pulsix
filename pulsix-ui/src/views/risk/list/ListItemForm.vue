<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="980px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-alert
        :title="`当前名单：${listContext?.listName || '-'} (${listContext?.listCode || '-'})`"
        type="info"
        show-icon
        class="mb-12px"
      />

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="匹配键名" prop="matchKey">
            <el-input v-model="formData.matchKey" placeholder="例如 deviceId；纯一维名单可留空" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="匹配值" prop="matchValue">
            <el-input v-model="formData.matchValue" placeholder="例如 D0009" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="过期时间" prop="expireAt">
            <el-date-picker
              v-model="formData.expireAt"
              class="!w-full"
              clearable
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="为空表示长期有效"
            />
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

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="来源类型" prop="sourceType">
            <el-select v-model="formData.sourceType" class="w-full" placeholder="请选择来源类型">
              <el-option
                v-for="item in riskListItemSourceTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="导入批次号" prop="batchNo">
            <el-input v-model="formData.batchNo" placeholder="可为空" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="条目备注" prop="remark">
        <el-input v-model="formData.remark" :rows="3" type="textarea" placeholder="请输入条目备注" />
      </el-form-item>

      <el-form-item label="扩展信息" prop="extJson">
        <div class="w-full">
          <JsonEditor v-model="formData.extJson" mode="code" height="240px" @error="onJsonError" />
          <div class="mt-8px text-12px text-[var(--el-text-color-secondary)]">
            如需写入 Redis Hash 值，可在 JSON 中提供 `value` 字段。
          </div>
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
import * as RiskListApi from '@/api/risk/list'
import { riskListItemSourceTypeOptions } from './constants'

defineOptions({ name: 'RiskListItemForm' })

interface ListContext {
  sceneCode: string
  listCode: string
  listName?: string
}

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const hasJsonError = ref(false)
const listContext = ref<ListContext>()

const createDefaultFormData = (): RiskListApi.ListItemVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  listCode: '',
  matchKey: 'deviceId',
  matchValue: '',
  expireAt: undefined,
  status: CommonStatusEnum.ENABLE,
  sourceType: 'MANUAL',
  batchNo: '',
  remark: '',
  extJson: {}
})

const formData = ref<RiskListApi.ListItemVO>(createDefaultFormData())
const formRules = reactive({
  matchValue: [{ required: true, message: '匹配值不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
  sourceType: [{ required: true, message: '来源类型不能为空', trigger: 'change' }]
})
const formRef = ref()

const resetForm = () => {
  formData.value = createDefaultFormData()
  hasJsonError.value = false
  formRef.value?.resetFields()
}

const open = async (type: 'create' | 'update', context: ListContext, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  listContext.value = context
  resetForm()
  formData.value.sceneCode = context.sceneCode
  formData.value.listCode = context.listCode
  if (id) {
    formLoading.value = true
    try {
      formData.value = await RiskListApi.getListItem(id)
      formData.value.extJson = formData.value.extJson || {}
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
      sceneCode: listContext.value?.sceneCode || formData.value.sceneCode,
      listCode: listContext.value?.listCode || formData.value.listCode,
      extJson: formData.value.extJson || {}
    }
    if (formType.value === 'create') {
      await RiskListApi.createListItem(data)
      message.success(t('common.createSuccess'))
    } else {
      await RiskListApi.updateListItem(data)
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
