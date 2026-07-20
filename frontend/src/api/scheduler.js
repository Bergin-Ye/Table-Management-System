import request from './request'

export function getSchedulerCron() {
  return request.get('/admin/scheduler')
}

export function updateSchedulerCron(cron) {
  return request.put('/admin/scheduler', { cron })
}
