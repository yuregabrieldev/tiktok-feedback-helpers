;(() => {
  'use strict';
  if (window.__harvesterPageLock) return;
  const lock = { enabled: false, pinned: '' };
  const stripHash = (url) => String(url || '').split('#')[0];
  const isLeaving = (raw) => {
    if (!lock.enabled) return false;
    try {
      const resolved = new URL(raw, location.href);
      return stripHash(resolved.href) !== stripHash(lock.pinned);
    } catch (_) {
      return true;
    }
  };
  const notify = () => {
    try { console.log('__HBR_PIN__blocked'); } catch (_) {}
  };
  document.addEventListener('click', (event) => {
    if (!lock.enabled || event.defaultPrevented) return;
    const anchor = event.target && event.target.closest && event.target.closest('a[href]');
    if (!anchor) return;
    const target = String(anchor.target || '').toLowerCase();
    if (target && target !== '_self') return; // native popup handling
    if (isLeaving(anchor.href)) {
      event.preventDefault();
      event.stopPropagation();
      notify();
    }
  }, true);
  document.addEventListener('submit', (event) => {
    if (!lock.enabled) return;
    const form = event.target;
    if (!form || !form.action) return;
    const target = String(form.target || '').toLowerCase();
    if (target && target !== '_self') return;
    if (isLeaving(form.action)) {
      event.preventDefault();
      event.stopPropagation();
      notify();
    }
  }, true);
  for (const name of ['pushState', 'replaceState']) {
    const original = history[name];
    history[name] = function(state, unused, url) {
      if (url != null && isLeaving(url)) {
        notify();
        return;
      }
      return original.apply(this, arguments);
    };
  }
  window.__harvesterPageLock = {
    configure(enabled, pinnedUrl) {
      lock.enabled = !!enabled;
      lock.pinned = String(pinnedUrl || '');
    }
  };
})();