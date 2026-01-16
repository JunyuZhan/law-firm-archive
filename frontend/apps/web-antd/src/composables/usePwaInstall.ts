import { ref, onMounted, onUnmounted } from 'vue';

export function usePwaInstall() {
  const deferredPrompt = ref<Event | null>(null);
  const showInstallPrompt = ref(false);
  const isInstallable = ref(false);

  const handler = (e: Event) => {
    e.preventDefault();
    deferredPrompt.value = e;
    isInstallable.value = true;
  };

  onMounted(() => {
    window.addEventListener('beforeinstallprompt', handler);
  });

  onUnmounted(() => {
    window.removeEventListener('beforeinstallprompt', handler);
  });

  const install = async () => {
    if (!deferredPrompt.value) return;

    (deferredPrompt.value as any).prompt();
    const { outcome } = await (deferredPrompt.value as any).userChoice;
    if (outcome === 'accepted') {
      isInstallable.value = false;
    }
    deferredPrompt.value = null;
  };

  return {
    isInstallable,
    showInstallPrompt,
    install,
  };
}
