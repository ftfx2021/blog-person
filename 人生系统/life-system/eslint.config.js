import eslint from '@eslint/js'
import globals from 'globals'
import tseslint from 'typescript-eslint'
import vue from 'eslint-plugin-vue'
import vueParser from 'vue-eslint-parser'

export default tseslint.config(
  { ignores: ['out', 'release', 'coverage', 'node_modules'] },
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
  ...vue.configs['flat/recommended'],
  {
    files: ['src/renderer/**/*.{ts,vue}'],
    languageOptions: {
      parser: vueParser,
      parserOptions: { parser: tseslint.parser, sourceType: 'module' },
      globals: globals.browser
    },
    rules: {
      'no-restricted-imports': ['error', {
        paths: ['electron', 'mysql2', 'mysql2/promise', 'fs', 'node:fs', 'node:fs/promises'],
        patterns: ['@zilliz/*', '@milvus-io/*']
      }]
    }
  },
  {
    files: ['src/shared/domain/**/*.ts'],
    rules: {
      'no-restricted-imports': ['error', { patterns: ['vue', 'electron', '@renderer/*'] }]
    }
  },
  {
    files: ['src/main/**/*.ts', 'src/preload/**/*.ts', 'scripts/**/*.ts', 'tests/**/*.ts'],
    languageOptions: { globals: globals.node }
  },
  {
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      'vue/multi-word-component-names': 'off'
    }
  }
)
