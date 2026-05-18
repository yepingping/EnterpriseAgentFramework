import { agentRequest } from './request'
import type { RuntimeRegistryEntry } from '@/types/agent'

export function listRuntimeRegistry() {
  return agentRequest.get<RuntimeRegistryEntry[]>('/api/runtimes')
}
