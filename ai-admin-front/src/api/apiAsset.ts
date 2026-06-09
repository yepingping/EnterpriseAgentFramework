import { agentRequest } from './request'
import type { ApiAssetPageResponse, ApiAssetQuery } from '@/types/apiAsset'

export function listApiAssets(params: ApiAssetQuery) {
  return agentRequest.get<ApiAssetPageResponse>('/api/api-assets', { params })
}
