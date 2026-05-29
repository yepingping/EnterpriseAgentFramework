<template>
  <el-tag size="small" :type="tagType">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { commonStatusTagType, formatCommonStatusLabel } from '@/utils/uiLabels'

const props = defineProps<{
  status?: string | null
}>()

const label = computed(() => formatCommonStatusLabel(props.status))
const tagType = computed(() => commonStatusTagType(props.status))
</script>
