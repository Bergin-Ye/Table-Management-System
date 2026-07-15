import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCompanies } from '../api/company'

export const useCompanyStore = defineStore('company', () => {
  const currentCompanyId = ref(null)
  const currentCompanyName = ref('全部公司')
  const companyList = ref([])

  async function fetchCompanies() {
    const res = await getCompanies()
    companyList.value = res.data || []
    if (companyList.value.length > 0 && !currentCompanyId.value) {
      currentCompanyId.value = companyList.value[0].id
      currentCompanyName.value = companyList.value[0].name
    }
  }

  function setCurrentCompany(id) {
    currentCompanyId.value = id
    const company = companyList.value.find(c => c.id === id)
    currentCompanyName.value = company ? company.name : '全部公司'
  }

  return {
    currentCompanyId,
    currentCompanyName,
    companyList,
    fetchCompanies,
    setCurrentCompany
  }
})
