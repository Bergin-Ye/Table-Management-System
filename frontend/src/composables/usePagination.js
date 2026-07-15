import { ref, reactive, computed } from 'vue'

export function usePagination(fetchFn, defaultPageSize = 20) {
  const list = ref([])
  const total = ref(0)
  const loading = ref(false)
  const queryParams = reactive({
    page: 1,
    pageSize: defaultPageSize
  })

  const totalPages = computed(() => Math.ceil(total.value / queryParams.pageSize))

  async function fetchData(extraParams = {}) {
    loading.value = true
    try {
      const params = { ...queryParams, ...extraParams }
      const res = await fetchFn(params)
      list.value = res.data.list || []
      total.value = res.data.total || 0
    } finally {
      loading.value = false
    }
  }

  function handlePageChange(page) {
    queryParams.page = page
    fetchData()
  }

  function handleSizeChange(size) {
    queryParams.pageSize = size
    queryParams.page = 1
    fetchData()
  }

  function resetPage() {
    queryParams.page = 1
  }

  return {
    list,
    total,
    loading,
    queryParams,
    totalPages,
    fetchData,
    handlePageChange,
    handleSizeChange,
    resetPage
  }
}
