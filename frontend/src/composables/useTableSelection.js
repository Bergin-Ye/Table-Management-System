import { ref, computed } from 'vue'

export function useTableSelection() {
  const selectedRows = ref([])

  const selectedIds = computed(() => selectedRows.value.map(row => row.id))

  function handleSelectionChange(rows) {
    selectedRows.value = rows
  }

  function clearSelection() {
    selectedRows.value = []
  }

  return {
    selectedRows,
    selectedIds,
    handleSelectionChange,
    clearSelection
  }
}
