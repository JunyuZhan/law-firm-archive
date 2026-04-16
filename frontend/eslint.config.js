import js from '@eslint/js'
import vue from 'eslint-plugin-vue'

export default [
  {
    ignores: ['dist/**', 'node_modules/**', '**/*.d.ts']
  },
  js.configs.recommended,
  ...vue.configs['flat/recommended'],
  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        __dirname: 'readonly',
        __APP_PRODUCT_VERSION__: 'readonly'
      }
    },
    rules: {
      'no-unused-vars': 'off',
      'vue/no-unused-vars': 'off',
      'vue/multi-word-component-names': 'off'
    }
  }
]
