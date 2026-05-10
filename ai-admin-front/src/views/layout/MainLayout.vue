<template>
  <el-container class="main-layout" :class="{ 'registry-shell': isRegistryShell, 'is-dark': theme === 'dark' }">
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <div class="logo-icon-wrap">
          <el-icon :size="22"><Cpu /></el-icon>
        </div>
        <span class="logo-text gradient-text">AI 能力中台</span>
      </div>
      <el-scrollbar class="sidebar-scroll">
        <el-menu
          :default-active="activeMenu"
          router
          class="sidebar-menu"
          :default-openeds="defaultOpenMenuGroups"
        >
          <!-- 1 概览 -->
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>概览</span>
          </el-menu-item>

          <!-- 2 AI 注册中心 -->
          <el-sub-menu index="/registry-group">
            <template #title>
              <el-icon><Connection /></el-icon>
              <span>AI 注册中心</span>
            </template>
            <el-menu-item index="/registry/projects">项目管理</el-menu-item>
            <el-menu-item index="/registry/capability-sync">能力变更评审</el-menu-item>
          </el-sub-menu>

          <!-- 3 Agent -->
          <el-menu-item index="/agent">
            <el-icon><Cpu /></el-icon>
            <span>Agent</span>
          </el-menu-item>

          <!-- 4 Skill -->
          <el-sub-menu index="/skill-group">
            <template #title>
              <el-icon><Aim /></el-icon>
              <span>Skill</span>
            </template>
            <el-menu-item index="/skill">Skill 管理</el-menu-item>
            <el-menu-item index="/skill/mining">Skill Mining</el-menu-item>
            <el-menu-item index="/skill/slot/extractors">槽位提取器</el-menu-item>
            <el-menu-item index="/skill/slot/dict-dept">部门字典</el-menu-item>
            <el-menu-item index="/skill/slot/dict-user">人员字典</el-menu-item>
            <el-menu-item index="/skill/slot/logs">槽位调用日志</el-menu-item>
          </el-sub-menu>

          <!-- 5 Tool -->
          <el-sub-menu index="/tool-group">
            <template #title>
              <el-icon><SetUp /></el-icon>
              <span>Tool</span>
            </template>
            <el-menu-item index="/tool">Tool 列表</el-menu-item>
            <el-menu-item index="/tool/retrieval">Tool 检索测试</el-menu-item>
            <el-menu-item index="/settings/tool-acl">Tool ACL</el-menu-item>
          </el-sub-menu>

          <div class="menu-divider" />

          <!-- 6 知识与检索 -->
          <el-sub-menu index="/knowledge-group">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>知识与检索</span>
            </template>
            <el-menu-item index="/knowledge">知识库管理</el-menu-item>
            <el-menu-item index="/knowledge/import">文件入库</el-menu-item>
            <el-menu-item index="/retrieval">检索测试</el-menu-item>
            <el-menu-item index="/biz-index">业务索引</el-menu-item>
          </el-sub-menu>

          <!-- 7 模型管理 -->
          <el-sub-menu index="/model-group">
            <template #title>
              <el-icon><Coin /></el-icon>
              <span>模型管理</span>
            </template>
            <el-menu-item index="/model">Provider 管理</el-menu-item>
            <el-menu-item index="/model/playground">模型调试台</el-menu-item>
          </el-sub-menu>

          <div class="menu-divider" />

          <!-- 8 对外开放 -->
          <el-sub-menu index="/open-group">
            <template #title>
              <el-icon><Share /></el-icon>
              <span>对外开放</span>
            </template>
            <el-menu-item index="/mcp/visibility">MCP · 暴露白名单</el-menu-item>
            <el-menu-item index="/mcp/clients">MCP · Client 凭证</el-menu-item>
            <el-menu-item index="/mcp/monitor">MCP · 调用流水</el-menu-item>
            <el-menu-item index="/mcp/onboarding">MCP · 接入向导</el-menu-item>
            <el-menu-item index="/a2a/endpoints">A2A · 暴露 Agent</el-menu-item>
            <el-menu-item index="/a2a/monitor">A2A · 会话监控</el-menu-item>
          </el-sub-menu>

          <!-- 9 治理与运维 -->
          <el-sub-menu index="/domain-group">
            <template #title>
              <el-icon><Compass /></el-icon>
              <span>治理与运维</span>
            </template>
            <el-menu-item index="/domain">领域定义</el-menu-item>
            <el-menu-item index="/domain/board">归属画布</el-menu-item>
            <el-menu-item index="/domain/classifier-test">分类器测试</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="breadcrumb-area">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-actions">
          <el-tooltip :content="theme === 'dark' ? '切换日间模式' : '切换夜间模式'" placement="bottom">
            <el-button
              :icon="theme === 'dark' ? Sunny : Moon"
              circle
              size="small"
              class="topbar-btn"
              @click="toggleTheme"
            />
          </el-tooltip>
          <div class="topbar-search">
            <el-input
              placeholder="搜索功能..."
              size="small"
              :prefix-icon="Search"
              clearable
              class="search-input"
            />
          </div>
          <el-tooltip content="通知" placement="bottom">
            <el-badge :value="3" :max="99" class="notify-badge">
              <el-button :icon="Bell" circle size="small" class="topbar-btn" />
            </el-badge>
          </el-tooltip>
          <ProjectSelector :compact="isRegistryShell" />
          <el-avatar :size="32" class="user-avatar">
            <el-icon :size="16"><User /></el-icon>
          </el-avatar>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import ProjectSelector from '@/components/ProjectSelector.vue'
import {
  Aim,
  Bell,
  Collection,
  Compass,
  Connection,
  DataAnalysis,
  Cpu,
  Coin,
  Search,
  SetUp,
  Share,
  User,
  Sunny,
  Moon,
} from '@element-plus/icons-vue'

const { theme, toggleTheme } = useTheme()

const route = useRoute()

/** 默认展开的侧边栏分组，可按需填入 index，如 '/registry-group' */
const defaultOpenMenuGroups: string[] = []

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/knowledge')) return '/knowledge'
  if (path.startsWith('/biz-index')) return '/biz-index'
  if (path.startsWith('/agent')) return '/agent'
  if (path.startsWith('/model/playground')) return '/model/playground'
  if (path.startsWith('/model')) return '/model'
  if (path.startsWith('/tool/retrieval')) return '/tool/retrieval'
  if (path.startsWith('/settings/tool-acl')) return '/settings/tool-acl'
  if (path.startsWith('/skill/mining')) return '/skill/mining'
  if (path.startsWith('/skill/slot/extractors')) return '/skill/slot/extractors'
  if (path.startsWith('/skill/slot/dict-dept')) return '/skill/slot/dict-dept'
  if (path.startsWith('/skill/slot/dict-user')) return '/skill/slot/dict-user'
  if (path.startsWith('/skill/slot/logs')) return '/skill/slot/logs'
  if (path.startsWith('/skill')) return '/skill'
  if (path.startsWith('/tool')) return '/tool'
  if (path.startsWith('/registry/capability-sync')) return '/registry/capability-sync'
  if (path.startsWith('/registry/projects')) return '/registry/projects'
  if (path.startsWith('/scan-project')) return '/registry/projects'
  if (path.startsWith('/domain/board')) return '/domain/board'
  if (path.startsWith('/domain/classifier-test')) return '/domain/classifier-test'
  if (path.startsWith('/domain')) return '/domain'
  if (path.startsWith('/mcp/visibility')) return '/mcp/visibility'
  if (path.startsWith('/mcp/clients')) return '/mcp/clients'
  if (path.startsWith('/mcp/monitor')) return '/mcp/monitor'
  if (path.startsWith('/mcp/onboarding')) return '/mcp/onboarding'
  if (path.startsWith('/a2a/endpoints')) return '/a2a/endpoints'
  if (path.startsWith('/a2a/monitor')) return '/a2a/monitor'
  return path
})

const currentTitle = computed(() => (route.meta.title as string) || '')

const isRegistryShell = computed(() => route.path.startsWith('/registry/projects') || route.path.startsWith('/scan-project'))

</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
}

.sidebar {
  background:
    radial-gradient(circle at 24px 24px, rgba(99, 102, 241, 0.28), transparent 26%),
    radial-gradient(circle at 210px 160px, rgba(14, 165, 233, 0.12), transparent 30%),
    linear-gradient(180deg, #07111f 0%, #08101c 42%, #050b14 100%);
  border-right: 1px solid rgba(148, 163, 184, 0.12);
  box-shadow: 18px 0 46px rgba(2, 6, 23, 0.22);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 10;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      linear-gradient(90deg, rgba(255, 255, 255, 0.055) 1px, transparent 1px),
      linear-gradient(180deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
    background-size: 32px 32px;
    mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.75), transparent 62%);
    opacity: 0.45;
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    top: 64px;
    right: 0;
    bottom: 0;
    width: 1px;
    background: linear-gradient(180deg, rgba(99, 102, 241, 0.65), transparent 40%, rgba(14, 165, 233, 0.22));
    pointer-events: none;
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 72px;
  padding: 0 22px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  border-bottom: 1px solid rgba(148, 163, 184, 0.10);

  &::after {
    content: 'Enterprise AI';
    position: absolute;
    left: 72px;
    bottom: 13px;
    font-size: 10px;
    line-height: 1;
    font-weight: 700;
    color: rgba(148, 163, 184, 0.64);
    letter-spacing: 0.12em;
    text-transform: uppercase;
  }
}

.logo-icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.22), transparent),
    linear-gradient(135deg, #4f46e5 0%, #2563eb 52%, #0ea5e9 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow:
    0 12px 28px rgba(37, 99, 235, 0.32),
    inset 0 0 0 1px rgba(255, 255, 255, 0.24);

  .el-icon {
    filter: drop-shadow(0 2px 5px rgba(15, 23, 42, 0.35));
  }
}

.logo-text {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: 0;
  padding-bottom: 10px;
  background: linear-gradient(135deg, #ffffff 0%, #dbeafe 46%, #93c5fd 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-scroll {
  flex: 1;
  overflow: hidden;
}

.menu-divider {
  height: 1px;
  margin: 12px 20px;
  background: linear-gradient(90deg, transparent, rgba(148, 163, 184, 0.18), transparent);
}

.sidebar-menu {
  border-right: none;
  padding: 14px 10px 18px;
  background: transparent;

  :deep(.el-menu) {
    background: transparent;
    border: none;
  }

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    isolation: isolate;

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  :deep(.el-menu-item) {
    height: 44px;
    line-height: 44px;
    margin: 4px 0;
    padding-right: 12px;
    border-radius: 8px;
    font-size: 13.5px;
    font-weight: 600;
    color: #a8b3c7;
    transition: color 0.18s ease, background 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
    position: relative;

    .el-icon {
      width: 28px;
      height: 28px;
      margin-right: 10px;
      border-radius: 8px;
      font-size: 16px;
      color: #8da2c0;
      background: rgba(148, 163, 184, 0.08);
      box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.07);
      transition: color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
    }

    &:hover {
      background: rgba(99, 102, 241, 0.12);
      color: #f8fafc;
      transform: translateX(2px);

      .el-icon {
        color: #dbeafe;
        background: rgba(99, 102, 241, 0.18);
        box-shadow: inset 0 0 0 1px rgba(129, 140, 248, 0.18);
      }
    }

    &.is-active {
      background:
        linear-gradient(135deg, rgba(79, 70, 229, 0.92), rgba(37, 99, 235, 0.76)),
        linear-gradient(90deg, rgba(255, 255, 255, 0.16), transparent);
      color: #ffffff;
      box-shadow:
        0 12px 26px rgba(37, 99, 235, 0.24),
        inset 0 0 0 1px rgba(255, 255, 255, 0.13);

      &::before {
        content: '';
        position: absolute;
        left: -10px;
        top: 8px;
        bottom: 8px;
        width: 4px;
        border-radius: 0 999px 999px 0;
        background: linear-gradient(180deg, #67e8f9, #818cf8);
        box-shadow: 0 0 14px rgba(103, 232, 249, 0.55);
      }

      &::after {
        content: '';
        position: absolute;
        inset: 1px;
        border-radius: 7px;
        background: linear-gradient(90deg, rgba(255, 255, 255, 0.16), transparent 42%);
        pointer-events: none;
        z-index: -1;
      }

      .el-icon {
        color: #ffffff;
        background: rgba(255, 255, 255, 0.16);
        box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
      }
    }
  }

  :deep(.el-sub-menu__title) {
    height: 44px;
    line-height: 44px;
    margin: 4px 0;
    padding-right: 12px;
    border-radius: 8px;
    font-size: 13.5px;
    font-weight: 700;
    color: #a8b3c7;
    transition: color 0.18s ease, background 0.18s ease, transform 0.18s ease;

    .el-icon {
      width: 28px;
      height: 28px;
      margin-right: 10px;
      border-radius: 8px;
      font-size: 16px;
      color: #8da2c0;
      background: rgba(148, 163, 184, 0.08);
      box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.07);
      transition: color 0.18s ease, background 0.18s ease;
    }

    .el-sub-menu__icon-arrow {
      width: auto;
      height: auto;
      margin-right: 0;
      background: transparent;
      box-shadow: none;
      color: #64748b;
    }

    &:hover {
      background: rgba(99, 102, 241, 0.10);
      color: #f8fafc;
      transform: translateX(2px);

      .el-icon {
        color: #dbeafe;
        background: rgba(99, 102, 241, 0.16);
      }

      .el-sub-menu__icon-arrow {
        color: #94a3b8;
        background: transparent;
        box-shadow: none;
      }
    }
  }

  :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
    color: #f8fafc;
    background: rgba(148, 163, 184, 0.08);

    .el-icon {
      color: #c7d2fe;
      background: rgba(99, 102, 241, 0.16);
    }
  }

  :deep(.el-sub-menu .el-menu) {
    background: transparent;
    padding: 2px 0 6px 18px;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      left: 22px;
      top: 2px;
      bottom: 8px;
      width: 1px;
      background: linear-gradient(180deg, rgba(99, 102, 241, 0.35), rgba(148, 163, 184, 0.08));
    }
  }

  :deep(.el-sub-menu .el-menu-item) {
    padding-left: 34px !important;
    height: 36px;
    line-height: 36px;
    margin: 2px 0 2px 8px;
    font-size: 12.5px;
    font-weight: 600;
    color: #8fa0ba;
    background: transparent;
    box-shadow: none;

    &::before {
      left: -18px;
      top: 12px;
      bottom: auto;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: rgba(148, 163, 184, 0.24);
      box-shadow: none;
    }

    .el-icon {
      display: none;
    }

    &:hover {
      color: #e2e8f0;
      background: rgba(99, 102, 241, 0.10);
    }

    &.is-active {
      color: #ffffff;
      background: linear-gradient(135deg, rgba(79, 70, 229, 0.72), rgba(37, 99, 235, 0.50));
      box-shadow: 0 10px 22px rgba(37, 99, 235, 0.18);

      &::before {
        background: #67e8f9;
        box-shadow: 0 0 10px rgba(103, 232, 249, 0.55);
      }
    }
  }
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(15, 15, 25, 0.6);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  height: 56px;
  padding: 0 24px;
}

.breadcrumb-area {
  display: flex;
  align-items: center;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.topbar-search {
  .search-input {
    width: 200px;
    transition: width 0.3s ease;

    &:focus-within {
      width: 280px;
    }

    :deep(.el-input__wrapper) {
      background: rgba(255, 255, 255, 0.04);
      border-radius: 8px;
    }
  }
}

.topbar-btn {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.06);
  color: #94a3b8;

  &:hover {
    background: rgba(99, 102, 241, 0.1);
    border-color: rgba(99, 102, 241, 0.2);
    color: #e2e8f0;
  }
}

.notify-badge {
  :deep(.el-badge__content) {
    background: linear-gradient(135deg, #f43f5e, #e11d48);
    border: none;
  }
}

.user-avatar {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  cursor: pointer;
  color: #fff;
  transition: box-shadow 0.2s ease;

  &:hover {
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.3);
  }
}

.main-content {
  background: var(--bg-primary);
  overflow-y: auto;
  position: relative;

  &::before {
    content: '';
    position: fixed;
    top: 0;
    left: 240px;
    right: 0;
    height: 300px;
    background: radial-gradient(ellipse at 50% 0%, rgba(99, 102, 241, 0.04) 0%, transparent 70%);
    pointer-events: none;
    z-index: 0;
  }
}

.registry-shell {
  .topbar {
    height: 56px;
    background: rgba(255, 255, 255, 0.92);
    border-bottom: 1px solid #edf0f7;
    box-shadow: 0 8px 24px rgba(17, 24, 39, 0.03);
    backdrop-filter: blur(16px);
  }

  .breadcrumb-area {
    :deep(.el-breadcrumb__inner),
    :deep(.el-breadcrumb__separator) {
      color: #667085;
      font-weight: 600;
    }

    :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
      color: #101828;
    }
  }

  .topbar-search {
    .search-input {
      :deep(.el-input__wrapper) {
        background: #f8f9fc;
        border-radius: 8px;
        box-shadow: 0 0 0 1px #e4e7ee inset;
      }
    }
  }

  .topbar-btn {
    background: #fff;
    border-color: #e4e7ee;
    color: #667085;

    &:hover {
      background: #f5f3ff;
      border-color: #ddd6fe;
      color: #5b3df5;
    }
  }

  .main-content {
    padding: 0;
    background: var(--bg-primary);

    &::before {
      display: none;
    }
  }
}

.main-layout.registry-shell.is-dark {
  .topbar {
    background: rgba(15, 15, 25, 0.72);
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    box-shadow: none;
    backdrop-filter: blur(16px);
  }

  .breadcrumb-area {
    :deep(.el-breadcrumb__inner),
    :deep(.el-breadcrumb__separator) {
      color: #94a3b8;
    }

    :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
      color: #e2e8f0;
    }
  }

  .topbar-search {
    .search-input {
      :deep(.el-input__wrapper) {
        background: rgba(255, 255, 255, 0.04);
        border-radius: 8px;
        box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08) inset;
      }
    }
  }

  .topbar-btn {
    background: rgba(255, 255, 255, 0.04);
    border-color: rgba(255, 255, 255, 0.08);
    color: #94a3b8;

    &:hover {
      background: rgba(99, 102, 241, 0.12);
      border-color: rgba(99, 102, 241, 0.35);
      color: #e2e8f0;
    }
  }
}

</style>
