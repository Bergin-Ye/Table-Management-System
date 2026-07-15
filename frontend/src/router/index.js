import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/delivery-record',
    children: [
      {
        path: 'delivery-record',
        name: 'DeliveryRecord',
        component: () => import('../views/delivery-record/DeliveryRecordView.vue'),
        meta: { title: '送货记录' }
      },
      {
        path: 'original-record',
        name: 'OriginalRecord',
        component: () => import('../views/original-record/OriginalRecordView.vue'),
        meta: { title: '原始记录' }
      },
      {
        path: 'machine-material',
        name: 'MachineMaterial',
        component: () => import('../views/machine-material/MachineMaterialView.vue'),
        meta: { title: '上机物料' }
      },
      {
        path: 'delivery-stats',
        name: 'DeliveryStats',
        component: () => import('../views/delivery-stats/DeliveryStatsView.vue'),
        meta: { title: '送货超比统计' }
      },
      {
        path: 'settlement-machine',
        name: 'SettlementMachine',
        component: () => import('../views/settlement-machine/SettlementMachineView.vue'),
        meta: { title: '结算机台数' }
      },
      {
        path: 'machine-detail',
        name: 'MachineDetail',
        component: () => import('../views/machine-detail/MachineDetailView.vue'),
        meta: { title: '机型明细' }
      },
      {
        path: 'machine-count',
        name: 'MachineCount',
        component: () => import('../views/machine-count/MachineCountView.vue'),
        meta: { title: '开机数量' }
      },
      {
        path: 'material',
        name: 'Material',
        component: () => import('../views/material/MaterialView.vue'),
        meta: { title: '物料表' }
      },
      {
        path: 'operation-log',
        name: 'OperationLog',
        component: () => import('../views/operation-log/OperationLogView.vue'),
        meta: { title: '操作日志' }
      },
      {
        path: 'company',
        name: 'Company',
        component: () => import('../views/company/CompanyView.vue'),
        meta: { title: '公司管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 导航守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
