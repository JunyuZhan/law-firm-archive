<script setup lang="ts">
import type { Recordable } from '@vben/types';

import type { SettingProps } from './types';

import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  Switch,
} from '@vben-core/shadcn-ui';

withDefaults(defineProps<SettingProps>(), {
  formSchema: () => [],
});

const emit = defineEmits<{
  change: [Recordable<any>];
}>();

function handleChange(fieldName: string, value: boolean) {
  emit('change', { fieldName, value });
}
</script>
<template>
  <Form class="space-y-6 lg:space-y-8">
    <div class="space-y-3 lg:space-y-4">
      <template v-for="item in formSchema" :key="item.fieldName">
        <FormField type="checkbox" :name="item.fieldName">
          <FormItem
            class="flex flex-row items-center justify-between gap-3 rounded-lg border p-3 lg:p-4"
          >
            <div class="min-w-0 flex-1 space-y-0.5">
              <FormLabel class="text-sm lg:text-base">
                {{ item.label }}
              </FormLabel>
              <FormDescription class="text-xs lg:text-sm">
                {{ item.description }}
              </FormDescription>
            </div>
            <FormControl class="flex-shrink-0">
              <Switch
                :model-value="item.value"
                @update:model-value="handleChange(item.fieldName, $event)"
              />
            </FormControl>
          </FormItem>
        </FormField>
      </template>
    </div>
  </Form>
</template>
