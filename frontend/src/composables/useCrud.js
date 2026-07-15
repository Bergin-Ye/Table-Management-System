import { ElMessage, ElMessageBox } from 'element-plus'

export function useCrud(api, fetchList) {
  async function handleCreate(data) {
    await api.create(data)
    ElMessage.success('新增成功')
    fetchList()
  }

  async function handleUpdate(id, data) {
    await api.update(id, data)
    ElMessage.success('修改成功')
    fetchList()
  }

  async function handleDelete(id) {
    try {
      await ElMessageBox.confirm('确定要删除该条记录吗？', '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await api.remove(id)
      ElMessage.success('删除成功')
      fetchList()
    } catch {
      // 取消删除
    }
  }

  async function handleBatchDelete(ids) {
    if (ids.length === 0) {
      ElMessage.warning('请选择要删除的记录')
      return
    }
    try {
      await ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 条记录吗？`, '批量删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await api.batchDelete(ids)
      ElMessage.success('批量删除成功')
      fetchList()
    } catch {
      // 取消删除
    }
  }

  return {
    handleCreate,
    handleUpdate,
    handleDelete,
    handleBatchDelete
  }
}
