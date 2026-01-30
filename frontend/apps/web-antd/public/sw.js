/**
 * Service Worker - 律师事务所 PWA
 * 手动实现的 Service Worker，避免 vite-plugin-pwa 与 Vite 7 的兼容性问题
 * 
 * 安全策略：
 * - 只持久化缓存安全的只读 API（字典、配置、案由等公共数据）
 * - 敏感业务数据使用短期内存缓存（不持久化，提升性能）
 * 
 * 性能优化：
 * - 静态资源：CacheFirst（优先缓存）
 * - 安全 API：NetworkFirst + 持久化缓存
 * - 业务 API：StaleWhileRevalidate + 短期内存缓存（不持久化）
 */

const CACHE_NAME = 'law-firm-cache-v5';
const STATIC_CACHE_NAME = 'law-firm-static-v5';
const API_CACHE_NAME = 'law-firm-api-v5';

// 短期内存缓存（不持久化到磁盘，安全）
// 格式: { url: { data: Response, timestamp: number } }
const memoryCache = new Map();

// 内存缓存 TTL（毫秒）- 与后端 Cache-Control 保持一致
const MEMORY_CACHE_TTL = {
  list: 1 * 60 * 1000,       // 列表数据：1分钟（与后端一致）
  detail: 2 * 60 * 1000,     // 详情数据：2分钟
  default: 30 * 1000,        // 默认：30秒
};

// 预缓存的核心资源
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/offline.html'
];

// 安全的只读 API 路径（持久化缓存）
// 这些 API 返回的是公共数据，不包含敏感信息
const SAFE_API_PATTERNS = [
  '/api/dict/',           // 字典数据
  '/api/config/',         // 系统配置
  '/api/causes/',         // 案由/罪名数据
  '/api/public/',         // 公共接口
  '/api/menu/',           // 菜单数据
  '/api/department/',     // 部门数据（公共）
];

// 可使用内存缓存的业务 API（不持久化，但可短期缓存）
const CACHEABLE_BUSINESS_API = [
  { pattern: '/api/matter', ttlKey: 'list' },
  { pattern: '/api/crm', ttlKey: 'list' },
  { pattern: '/api/document', ttlKey: 'list' },
  { pattern: '/api/finance', ttlKey: 'list' },
  { pattern: '/api/task', ttlKey: 'list' },
  { pattern: '/api/timesheet', ttlKey: 'list' },
  { pattern: '/api/archive', ttlKey: 'list' },
  { pattern: '/api/knowledge', ttlKey: 'list' },
];

// 不缓存的敏感操作 API（包含用户隐私或高度敏感的数据）
const NO_CACHE_PATTERNS = [
  '/api/auth/',           // 认证相关
  '/api/admin/',          // 整个管理模块（用户、角色、权限等）
  '/api/hr/',             // 整个人事模块（薪资、考勤、合同、绩效等）
  '/api/user/info',       // 当前用户信息
  '/api/user/profile',    // 用户档案
  '/api/ai/',             // AI 相关（可能包含敏感内容）
];

// 请求去重：正在进行的请求
const pendingRequests = new Map();

// 安装事件 - 预缓存核心资源
self.addEventListener('install', (event) => {
  console.log('[SW] Installing Service Worker v5');
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('[SW] Precaching core resources');
        return cache.addAll(PRECACHE_URLS);
      })
      .then(() => {
        return self.skipWaiting();
      })
  );
});

// 激活事件 - 清理旧缓存
self.addEventListener('activate', (event) => {
  console.log('[SW] Activating Service Worker v5');
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames
          .filter((name) => {
            return name.startsWith('law-firm-') && 
                   name !== CACHE_NAME && 
                   name !== STATIC_CACHE_NAME && 
                   name !== API_CACHE_NAME;
          })
          .map((name) => {
            console.log('[SW] Deleting old cache:', name);
            return caches.delete(name);
          })
      );
    }).then(() => {
      return self.clients.claim();
    })
  );
});

// 请求拦截
self.addEventListener('fetch', (event) => {
  const { request } = event;
  
  // 只处理 GET 请求，其他请求完全不干预
  if (request.method !== 'GET') {
    // 异步清理缓存，不阻塞请求
    try {
      const url = new URL(request.url);
      if (url.origin === location.origin && url.pathname.startsWith('/api/')) {
        // 使用 setTimeout 确保不阻塞
        setTimeout(() => clearRelatedMemoryCache(url.pathname), 0);
      }
    } catch {
      // 忽略解析错误
    }
    return; // 完全不干预非 GET 请求
  }

  const url = new URL(request.url);

  // 只处理同源请求
  if (url.origin !== location.origin) {
    return;
  }

  // API 请求处理
  if (url.pathname.startsWith('/api/')) {
    // 检查是否为不缓存的敏感操作
    if (isNoCacheApi(url.pathname)) {
      return; // 直接走网络
    }

    // 检查是否为安全的只读 API（持久化缓存）
    if (isSafeApi(url.pathname)) {
      event.respondWith(networkFirst(request, API_CACHE_NAME));
      return;
    }

    // 业务 API：使用 Stale-While-Revalidate + 内存缓存
    const ttlConfig = getCacheableTTL(url.pathname);
    if (ttlConfig) {
      event.respondWith(staleWhileRevalidate(request, ttlConfig.ttl));
      return;
    }

    // 其他 API：不缓存
    return;
  }

  // 静态资源 - 缓存优先策略
  if (isStaticAsset(url.pathname)) {
    event.respondWith(cacheFirst(request, STATIC_CACHE_NAME));
    return;
  }

  // 页面导航 - 网络优先，失败时返回离线页面
  if (request.mode === 'navigate') {
    event.respondWith(
      networkFirst(request, CACHE_NAME).catch(() => {
        return caches.match('/offline.html');
      })
    );
    return;
  }

  // 其他请求 - 网络优先
  event.respondWith(networkFirst(request, CACHE_NAME));
});

// ========== 辅助函数 ==========

function isSafeApi(pathname) {
  return SAFE_API_PATTERNS.some(pattern => pathname.startsWith(pattern));
}

function isNoCacheApi(pathname) {
  return NO_CACHE_PATTERNS.some(pattern => pathname.startsWith(pattern));
}

function getCacheableTTL(pathname) {
  for (const config of CACHEABLE_BUSINESS_API) {
    if (pathname.startsWith(config.pattern)) {
      return { ttl: MEMORY_CACHE_TTL[config.ttlKey] || MEMORY_CACHE_TTL.default };
    }
  }
  return null;
}

function isStaticAsset(pathname) {
  return /\.(js|css|png|jpg|jpeg|svg|gif|webp|woff2?|ico)$/i.test(pathname);
}

// ========== 缓存策略 ==========

// 缓存优先策略（静态资源）
async function cacheFirst(request, cacheName) {
  // 安全检查：只缓存 GET 请求
  if (request.method !== 'GET') {
    // 非 GET 请求直接走网络，不缓存
    return fetch(request);
  }
  
  const cachedResponse = await caches.match(request);
  if (cachedResponse) {
    return cachedResponse;
  }

  try {
    const networkResponse = await fetch(request);
    if (networkResponse.ok) {
      const cache = await caches.open(cacheName);
      cache.put(request, networkResponse.clone());
    }
    return networkResponse;
  } catch (error) {
    console.log('[SW] Cache first failed:', error);
    throw error;
  }
}

// 网络优先策略（安全 API + 持久化缓存）
async function networkFirst(request, cacheName) {
  // 安全检查：只缓存 GET 请求
  if (request.method !== 'GET') {
    // 非 GET 请求直接走网络，不缓存
    return fetch(request);
  }
  
  try {
    const networkResponse = await fetch(request);
    if (networkResponse.ok) {
      const cache = await caches.open(cacheName);
      cache.put(request, networkResponse.clone());
    }
    return networkResponse;
  } catch (error) {
    console.log('[SW] Network first failed, trying cache:', request.url);
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }
    throw error;
  }
}

// Stale-While-Revalidate 策略（业务 API + 内存缓存）
// 先返回缓存（如果有且未过期），同时在后台更新
async function staleWhileRevalidate(request, ttl) {
  // 安全检查：只缓存 GET 请求
  if (request.method !== 'GET') {
    // 非 GET 请求直接走网络，不缓存
    return fetch(request);
  }
  
  const cacheKey = request.url;
  const now = Date.now();

  // 检查内存缓存
  const cached = memoryCache.get(cacheKey);
  if (cached && (now - cached.timestamp) < ttl) {
    // 缓存有效，直接返回
    // 同时在后台更新缓存（不阻塞响应）
    updateMemoryCacheInBackground(request, cacheKey);
    return cached.response.clone();
  }

  // 缓存过期或不存在，请求网络
  // 使用请求去重，避免短时间内重复请求
  return deduplicatedFetch(request, cacheKey, ttl);
}

// 去重的网络请求
async function deduplicatedFetch(request, cacheKey, ttl) {
  // 检查是否有正在进行的相同请求
  if (pendingRequests.has(cacheKey)) {
    console.log('[SW] Deduplicating request:', cacheKey);
    return pendingRequests.get(cacheKey);
  }

  // 创建请求 Promise
  const fetchPromise = (async () => {
    try {
      const response = await fetch(request);
      if (response.ok) {
        // 存入内存缓存
        memoryCache.set(cacheKey, {
          response: response.clone(),
          timestamp: Date.now(),
        });
      }
      return response;
    } finally {
      // 请求完成，移除 pending 状态
      pendingRequests.delete(cacheKey);
    }
  })();

  // 记录正在进行的请求
  pendingRequests.set(cacheKey, fetchPromise);

  return fetchPromise;
}

// 后台更新内存缓存
function updateMemoryCacheInBackground(request, cacheKey) {
  // 延迟执行，避免阻塞响应
  setTimeout(async () => {
    try {
      const response = await fetch(request);
      if (response.ok) {
        memoryCache.set(cacheKey, {
          response: response.clone(),
          timestamp: Date.now(),
        });
        console.log('[SW] Background cache updated:', cacheKey);
      }
    } catch (error) {
      console.log('[SW] Background update failed:', error);
    }
  }, 100);
}

// 清除相关内存缓存（当有修改操作时）
function clearRelatedMemoryCache(pathname) {
  try {
    const parts = pathname.split('/');
    if (parts.length < 3) return;
    
    const prefix = parts.slice(0, 3).join('/'); // 如 /api/matter
    let cleared = 0;
    const keysToDelete = [];
    
    // 先收集要删除的键，避免遍历时修改
    for (const key of memoryCache.keys()) {
      if (key.includes(prefix)) {
        keysToDelete.push(key);
      }
    }
    
    // 然后删除
    for (const key of keysToDelete) {
      memoryCache.delete(key);
      cleared++;
    }
    
    if (cleared > 0) {
      console.log('[SW] Cleared', cleared, 'memory cache entries for:', prefix);
    }
  } catch (error) {
    console.warn('[SW] Error clearing memory cache:', error);
  }
}

// 定期清理过期的内存缓存（每5分钟）
setInterval(() => {
  const now = Date.now();
  let expired = 0;
  for (const [key, value] of memoryCache.entries()) {
    // 超过最长 TTL（5分钟）的缓存清除
    if (now - value.timestamp > MEMORY_CACHE_TTL.detail) {
      memoryCache.delete(key);
      expired++;
    }
  }
  if (expired > 0) {
    console.log('[SW] Cleaned up', expired, 'expired memory cache entries');
  }
}, 5 * 60 * 1000);

// 监听来自客户端的消息
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
  
  // 支持手动清除缓存
  if (event.data && event.data.type === 'CLEAR_CACHE') {
    memoryCache.clear();
    console.log('[SW] Memory cache cleared');
    
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames
          .filter((name) => name.startsWith('law-firm-'))
          .map((name) => caches.delete(name))
      );
    }).then(() => {
      event.source.postMessage({ type: 'CACHE_CLEARED' });
    });
  }
});
