declare const customTranslate: (
  translations: Record<string, string>
) => (template: string, replacements?: Record<string, string>) => string

export default customTranslate
