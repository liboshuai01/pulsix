export {}

declare global {
  const DICT_TYPE: typeof import('@/utils/dict')['DICT_TYPE']
  const getBoolDictOptions: typeof import('@/utils/dict')['getBoolDictOptions']
  const getDictLabel: typeof import('@/utils/dict')['getDictLabel']
  const getDictObj: typeof import('@/utils/dict')['getDictObj']
  const getIntDictOptions: typeof import('@/utils/dict')['getIntDictOptions']
  const getStrDictOptions: typeof import('@/utils/dict')['getStrDictOptions']
  const required: typeof import('@/utils/formRules')['required']
}
