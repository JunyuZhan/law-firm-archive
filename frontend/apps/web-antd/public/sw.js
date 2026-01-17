/**
 * Service Worker - 律师事务所 PWA
 * 手动实现的 Service Worker，避免 vite-plugin-pwa 与 Vite 7 的兼容性问题
 * 
 * 安全策略：
 * - 只缓存安全的只读 API（字典、配置、案由等公共数据）
 * - 敏感业务数据（案件、客户、财务等）不缓存
 */

const CACHE_NAME = 'law-firm-cache-v3';
const STATIC_CACHE_NAME = 'law-firm-static-v3';
const API_CACHE_NAME = 'law-firm-api-v3';

// 预缓存的核心资源
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/offline.html'
];

// 安全的只读 API 路径（可缓存）
// 这些 API 返回的是公共数据，不包含敏感信息
const SAFE_API_PATTERNS = [
  '/api/dict/',           // 字典数据
  '/api/config/',         // 系统配置
  '/api/causes/',         // 案由/罪名数据
  '/api/public/',         // 公共接口
  '/api/menu/',           // 菜单数据
  '/api/department/',     // 部门数据（公共）
];

// 安装事件 - 预缓存核心资源
self.addEventListener('install', (event) => {
  console.log('[SW] Installing Service Worker');
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('[SW] Precaching core resources');
        return cache.addAll(PRECACHE_URLS);
      })
      .then(() => {
        // 立即激活，不等待旧的 Service Worker 停止
        return self.skipWaiting();
      })
  );
});

// 激活事件 - 清理旧缓存
self.addEventListener('activate', (event) => {
  console.log('[SW] Activating Service Worker');
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames
          .filter((name) => {
            // 删除旧版本的缓存
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
      // 立即接管所有客户端
      return self.clients.claim();
    })
  );
});

// 请求拦截
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // 只处理同源请求
  if (url.origin !== location.origin) {
    return;
  }

  // API 请求处理
  if (url.pathname.startsWith('/api/')) {
    // 检查是否为安全的只读 API
    const isSafeApi = SAFE_API_PATTERNS.some(pattern => 
      url.pathname.startsWith(pattern)
    );
    
    if (isSafeApi) {
      // 安全 API - 网络优先，支持离线缓存
      event.respondWith(networkFirst(request, API_CACHE_NAME));
    }
    // 敏感 API（案件、客户、财务等）- 不缓存，直接走网络
    // 不调用 event.respondWith()，让浏览器正常处理
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

// 判断是否为静态资源
function isStaticAsset(pathname) {
  return /\.(js|css|png|jpg|jpeg|svg|gif|webp|woff2?|ico)$/i.test(pathname);
}

// 缓存优先策略
async function cacheFirst(request, cacheName) {
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

// 网络优先策略
async function networkFirst(request, cacheName) {
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

// 监听来自客户端的消息
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});
