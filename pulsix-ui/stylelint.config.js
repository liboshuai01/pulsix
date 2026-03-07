module.exports = {
  root: true,
  plugins: ['stylelint-order'],
  customSyntax: 'postcss-html',
  extends: ['stylelint-config-standard'],
  rules: {
    'selector-pseudo-class-no-unknown': [
      true,
      {
        ignorePseudoClasses: ['global', 'deep']
      }
    ],
    'at-rule-no-unknown': [
      true,
      {
        ignoreAtRules: ['function', 'if', 'each', 'include', 'mixin', 'extend']
      }
    ],
    'media-query-no-invalid': null,
    'function-no-unknown': null,
    'no-empty-source': null,
    'named-grid-areas-no-invalid': null,
    'no-descending-specificity': null,
    'font-family-no-missing-generic-family-keyword': null,
    'font-family-name-quotes': null,
    'length-zero-no-unit': null,
    'at-rule-empty-line-before': null,
    'media-feature-range-notation': null,
    'declaration-empty-line-before': null,
    'custom-property-empty-line-before': null,
    'rule-empty-line-before': null,
    'comment-empty-line-before': null,
    'comment-whitespace-inside': null,
    'no-invalid-double-slash-comments': null,
    'color-function-notation': null,
    'alpha-value-notation': null,
    'color-hex-length': null,
    'custom-property-pattern': null,
    'declaration-block-no-redundant-longhand-properties': null,
    'shorthand-property-no-redundant-values': null,
    'unit-no-unknown': [
      true,
      {
        ignoreUnits: ['rpx']
      }
    ],
    'order/order': null,
    'order/properties-order': null
  },
  ignoreFiles: ['**/*.js', '**/*.jsx', '**/*.tsx', '**/*.ts'],
  overrides: [
    {
      files: ['*.vue', '**/*.vue', '*.html', '**/*.html'],
      extends: ['stylelint-config-recommended', 'stylelint-config-html'],
      rules: {
        'keyframes-name-pattern': null,
        'font-family-name-quotes': null,
        'length-zero-no-unit': null,
        'at-rule-empty-line-before': null,
        'media-feature-range-notation': null,
        'declaration-empty-line-before': null,
        'custom-property-empty-line-before': null,
        'selector-class-pattern': null,
        'no-duplicate-selectors': null,
        'selector-pseudo-class-no-unknown': [
          true,
          {
            ignorePseudoClasses: ['deep', 'global']
          }
        ],
        'selector-pseudo-element-no-unknown': [
          true,
          {
            ignorePseudoElements: ['v-deep', 'v-global', 'v-slotted']
          }
        ],
        'rule-empty-line-before': null,
        'comment-empty-line-before': null,
        'comment-whitespace-inside': null,
        'no-invalid-double-slash-comments': null,
        'color-function-notation': null,
        'alpha-value-notation': null,
        'color-hex-length': null,
        'custom-property-pattern': null,
        'declaration-block-no-redundant-longhand-properties': null,
        'shorthand-property-no-redundant-values': null,
        'order/properties-order': null
      }
    }
  ]
}
