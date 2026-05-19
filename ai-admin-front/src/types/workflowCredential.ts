export type WorkflowCredentialType =
  | 'BEARER'
  | 'BASIC'
  | 'API_KEY_HEADER'
  | 'API_KEY_QUERY'
  | 'CUSTOM_HEADERS'

export interface WorkflowCredential {
  id: number
  credentialRef: string
  name: string
  type: WorkflowCredentialType
  projectId?: number | null
  projectCode?: string | null
  scope: 'PROJECT' | 'GLOBAL'
  status: 'ACTIVE' | 'DISABLED'
  secretPreview?: Record<string, unknown>
  createdAt?: string
  updatedAt?: string
}

export interface WorkflowCredentialPayload {
  credentialRef?: string
  name: string
  type: WorkflowCredentialType
  projectId?: number | null
  projectCode?: string | null
  scope?: 'PROJECT' | 'GLOBAL'
  status?: 'ACTIVE' | 'DISABLED'
  secret: Record<string, unknown>
}
